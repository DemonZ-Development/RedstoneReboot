use crate::config::BackendConfig;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum BackendType {
    Auto,
    LocalScript,
    DependOnHost,
    Docker,
    Pterodactyl,
    Systemd,
}

impl BackendType {
    pub fn parse(raw: &str) -> Self {
        match raw.trim().to_ascii_uppercase().as_str() {
            "AUTO" => Self::Auto,
            "LOCALSCRIPT" | "LOCAL_SCRIPT" | "SCRIPT" => Self::LocalScript,
            "DEPEND_ON_HOST" | "DEPENDONHOST" | "HOST" | "SHUTDOWN_ONLY" => Self::DependOnHost,
            "DOCKER" => Self::Docker,
            "PTERODACTYL" | "PTERO" => Self::Pterodactyl,
            "SYSTEMD" => Self::Systemd,
            _ => Self::Auto,
        }
    }

    pub fn display_name(&self) -> &'static str {
        match self {
            Self::Auto => "AUTO",
            Self::LocalScript => "LOCAL_SCRIPT",
            Self::DependOnHost => "DEPEND_ON_HOST",
            Self::Docker => "DOCKER",
            Self::Pterodactyl => "PTERODACTYL",
            Self::Systemd => "SYSTEMD",
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BackendState {
    Full,
    Generated,
    DependOnHost,
}

impl BackendState {
    pub fn display_name(&self) -> &'static str {
        match self {
            Self::Full => "Wired (Active Wrapper)",
            Self::Generated => "Script Generated (Launch via run.bat / run.sh)",
            Self::DependOnHost => "Host / Container Managed",
        }
    }
}

pub struct BackendManager {
    configured_type: BackendType,
    effective_type: BackendType,
    state: BackendState,
    data_folder: PathBuf,
}

impl BackendManager {
    pub fn new(config: &BackendConfig, data_folder: PathBuf) -> Self {
        let configured = BackendType::parse(&config.active_backend);
        let is_container = Self::is_container_or_hosting_environment();
        let is_wired = Self::is_wrapper_active(&data_folder);

        let (effective, state) = match configured {
            BackendType::Auto => {
                if is_container {
                    (BackendType::DependOnHost, BackendState::DependOnHost)
                } else if is_wired {
                    (BackendType::LocalScript, BackendState::Full)
                } else if config.auto_generate_script {
                    let _ = Self::generate_scripts(&data_folder);
                    (BackendType::LocalScript, BackendState::Generated)
                } else {
                    (BackendType::LocalScript, BackendState::DependOnHost)
                }
            }
            BackendType::LocalScript => {
                if is_wired {
                    (BackendType::LocalScript, BackendState::Full)
                } else {
                    if config.auto_generate_script {
                        let _ = Self::generate_scripts(&data_folder);
                    }
                    (BackendType::LocalScript, BackendState::Generated)
                }
            }
            BackendType::DependOnHost
            | BackendType::Docker
            | BackendType::Pterodactyl
            | BackendType::Systemd => (configured, BackendState::DependOnHost),
        };

        Self {
            configured_type: configured,
            effective_type: effective,
            state,
            data_folder,
        }
    }

    pub fn is_container_or_hosting_environment() -> bool {
        let indicators = [
            "PTERODACTYL",
            "PTERODACTYL_ENVIRONMENT",
            "CONTAINER",
            "DOCKER",
            "KUBERNETES_SERVICE_HOST",
            "SERVER_MEMORY",
        ];
        for var in &indicators {
            if std::env::var(var).is_ok() {
                return true;
            }
        }

        if Path::new("/.dockerenv").exists() || Path::new("/run/.containerenv").exists() {
            return true;
        }

        false
    }

    pub fn is_wrapper_active(data_folder: &Path) -> bool {
        if std::env::var("REDSTONEREBOOT_ACTIVE").as_deref() == Ok("1") {
            return true;
        }

        if data_folder.join(".redstonereboot_wired").exists() {
            return true;
        }

        if Path::new(".redstonereboot_wired").exists() {
            return true;
        }

        false
    }

    pub fn generate_scripts(data_folder: &Path) -> Result<(), std::io::Error> {
        let bat_content = Self::windows_template();
        let sh_content = Self::linux_template();

        let _ = fs::write(data_folder.join("run.bat"), bat_content);
        let _ = fs::write(data_folder.join("run.sh"), sh_content);

        if let Some(parent) = data_folder.parent() {
            let _ = fs::write(parent.join("run.bat"), bat_content);
            let _ = fs::write(parent.join("run.sh"), sh_content);
            if let Some(grandparent) = parent.parent() {
                let _ = fs::write(grandparent.join("run.bat"), bat_content);
                let _ = fs::write(grandparent.join("run.sh"), sh_content);
            }
        }

        Ok(())
    }

    pub fn arm_restart_marker(&self) {
        let marker = "restart";
        let _ = fs::write(self.data_folder.join(".redstonereboot_restart"), marker);

        if let Some(parent) = self.data_folder.parent() {
            let _ = fs::write(parent.join(".redstonereboot_restart"), marker);
            if let Some(grandparent) = parent.parent() {
                let _ = fs::write(grandparent.join(".redstonereboot_restart"), marker);
            }
        }
    }

    pub fn configured_type(&self) -> BackendType {
        self.configured_type
    }

    pub fn effective_type(&self) -> BackendType {
        self.effective_type
    }

    pub fn state(&self) -> BackendState {
        self.state
    }

    pub fn data_folder(&self) -> &Path {
        &self.data_folder
    }

    pub fn summary(&self) -> String {
        match self.effective_type {
            BackendType::DependOnHost => {
                if self.configured_type == BackendType::Auto {
                    "DEPEND_ON_HOST §7(Auto-detected Host/Container)".to_string()
                } else {
                    "DEPEND_ON_HOST §7(Host Panel Managed)".to_string()
                }
            }
            BackendType::LocalScript => match self.state {
                BackendState::Full => "LOCAL_SCRIPT §a(Active Wrapper Loop)".to_string(),
                BackendState::Generated => {
                    "LOCAL_SCRIPT §e(Script Generated - Start via run.bat / run.sh)".to_string()
                }
                BackendState::DependOnHost => "LOCAL_SCRIPT §7(Unwired)".to_string(),
            },
            BackendType::Docker => "DOCKER §7(Container Managed)".to_string(),
            BackendType::Pterodactyl => "PTERODACTYL §7(Panel Managed)".to_string(),
            BackendType::Systemd => "SYSTEMD §7(Service Managed)".to_string(),
            BackendType::Auto => "AUTO".to_string(),
        }
    }

    pub fn windows_template() -> &'static str {
        "@echo off\r\ntitle Pumpkin Server Wrapper\r\nset REDSTONEREBOOT_ACTIVE=1\r\n\r\nif exist \"%~dp0pumpkin-X64-Windows.exe\" cd /d \"%~dp0\"\r\nif exist \"%~dp0pumpkin.exe\" cd /d \"%~dp0\"\r\nif exist \"%~dp0..\\pumpkin-X64-Windows.exe\" cd /d \"%~dp0..\"\r\nif exist \"%~dp0..\\pumpkin.exe\" cd /d \"%~dp0..\"\r\nif exist \"%~dp0..\\..\\pumpkin-X64-Windows.exe\" cd /d \"%~dp0..\\..\"\r\nif exist \"%~dp0..\\..\\pumpkin.exe\" cd /d \"%~dp0..\\..\"\r\nif exist \"%~dp0..\\..\\..\\pumpkin-X64-Windows.exe\" cd /d \"%~dp0..\\..\\..\"\r\nif exist \"%~dp0..\\..\\..\\pumpkin.exe\" cd /d \"%~dp0..\\..\\..\"\r\n\r\nset PUMPKIN_EXE=\r\nif exist \"pumpkin-X64-Windows.exe\" set PUMPKIN_EXE=pumpkin-X64-Windows.exe\r\nif exist \"pumpkin.exe\" set PUMPKIN_EXE=pumpkin.exe\r\n\r\nif \"%PUMPKIN_EXE%\"==\"\" (\r\n    echo [RedstoneReboot] ERROR: pumpkin-X64-Windows.exe was not found.\r\n    echo Current directory: %CD%\r\n    pause\r\n    exit /b 1\r\n)\r\n\r\necho 1 > \"plugins\\data\\RedstoneReboot\\.redstonereboot_wired\" 2>nul\r\necho 1 > \"plugins\\RedstoneReboot\\.redstonereboot_wired\" 2>nul\r\necho 1 > \".redstonereboot_wired\" 2>nul\r\n\r\n:start\r\necho Starting Pumpkin Server...\r\n%PUMPKIN_EXE%\r\n\r\nif not exist \"plugins\\RedstoneReboot\\.redstonereboot_restart\" if not exist \"plugins\\data\\RedstoneReboot\\.redstonereboot_restart\" if not exist \".redstonereboot_restart\" goto end\r\nif exist \"plugins\\RedstoneReboot\\.redstonereboot_restart\" del /f /q \"plugins\\RedstoneReboot\\.redstonereboot_restart\" >nul 2>&1\r\nif exist \"plugins\\data\\RedstoneReboot\\.redstonereboot_restart\" del /f /q \"plugins\\data\\RedstoneReboot\\.redstonereboot_restart\" >nul 2>&1\r\nif exist \".redstonereboot_restart\" del /f /q \".redstonereboot_restart\" >nul 2>&1\r\necho Server stopped by RedstoneReboot. Restarting in 5 seconds... (Press Ctrl+C to cancel)\r\ntimeout /t 5\r\ngoto start\r\n\r\n:end\r\necho Server shut down normally. Exiting.\r\ndel /f /q \"plugins\\data\\RedstoneReboot\\.redstonereboot_wired\" >nul 2>&1\r\ndel /f /q \"plugins\\RedstoneReboot\\.redstonereboot_wired\" >nul 2>&1\r\ndel /f /q \".redstonereboot_wired\" >nul 2>&1\r\npause\r\nexit /b 0\r\n"
    }

    pub fn linux_template() -> &'static str {
        "#!/bin/bash\nexport REDSTONEREBOOT_ACTIVE=1\nSCRIPT_DIR=\"$(cd \"$(dirname \"$0\")\" && pwd)\"\nif [ -f \"$SCRIPT_DIR/pumpkin-X64-Linux\" ] || [ -f \"$SCRIPT_DIR/pumpkin\" ]; then\n    cd \"$SCRIPT_DIR\"\nelif [ -f \"$SCRIPT_DIR/../pumpkin-X64-Linux\" ] || [ -f \"$SCRIPT_DIR/../pumpkin\" ]; then\n    cd \"$SCRIPT_DIR/..\"\nelif [ -f \"$SCRIPT_DIR/../../pumpkin-X64-Linux\" ] || [ -f \"$SCRIPT_DIR/../../pumpkin\" ]; then\n    cd \"$SCRIPT_DIR/../..\"\nelif [ -f \"$SCRIPT_DIR/../../../pumpkin-X64-Linux\" ] || [ -f \"$SCRIPT_DIR/../../../pumpkin\" ]; then\n    cd \"$SCRIPT_DIR/../../..\"\nfi\nPUMPKIN_BIN=\"\"\nif [ -f \"./pumpkin-X64-Linux\" ]; then\n    PUMPKIN_BIN=\"./pumpkin-X64-Linux\"\nelif [ -f \"./pumpkin\" ]; then\n    PUMPKIN_BIN=\"./pumpkin\"\nfi\nif [ -z \"$PUMPKIN_BIN\" ]; then\n    echo \"[RedstoneReboot] ERROR: pumpkin-X64-Linux executable not found.\"\n    exit 1\nfi\nmkdir -p \"plugins/data/RedstoneReboot\" \"plugins/RedstoneReboot\" 2>/dev/null\ntouch \"plugins/data/RedstoneReboot/.redstonereboot_wired\" 2>/dev/null\ntouch \"plugins/RedstoneReboot/.redstonereboot_wired\" 2>/dev/null\ntouch \".redstonereboot_wired\" 2>/dev/null\nwhile true; do\n    echo \"Starting Pumpkin Server...\"\n    $PUMPKIN_BIN\n    if [ ! -f \"plugins/RedstoneReboot/.redstonereboot_restart\" ] && [ ! -f \"plugins/data/RedstoneReboot/.redstonereboot_restart\" ] && [ ! -f \".redstonereboot_restart\" ]; then\n        echo \"Server shut down normally. Exiting.\"\n        rm -f \"plugins/data/RedstoneReboot/.redstonereboot_wired\" \"plugins/RedstoneReboot/.redstonereboot_wired\" \".redstonereboot_wired\" 2>/dev/null\n        exit 0\n    fi\n    rm -f \"plugins/RedstoneReboot/.redstonereboot_restart\" \"plugins/data/RedstoneReboot/.redstonereboot_restart\" \".redstonereboot_restart\" 2>/dev/null\n    echo \"Server stopped by RedstoneReboot. Restarting in 5 seconds... (Press Ctrl+C to cancel)\"\n    sleep 5\ndone\n"
    }
}
