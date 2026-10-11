use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct GeneralConfig {
    pub plugin_prefix: String,
    pub debug_mode: bool,
    pub strict_validation: bool,
}

impl Default for GeneralConfig {
    fn default() -> Self {
        Self {
            plugin_prefix: "§8[§cRedstone§8] §aReboot".to_string(),
            debug_mode: false,
            strict_validation: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct ScheduledRestartsConfig {
    pub enabled: bool,
    pub times: Vec<String>,
    pub timezone: String,
    pub days: Vec<String>,
    pub warning_time: u32,
}

impl Default for ScheduledRestartsConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            times: vec![
                "06:00".to_string(),
                "12:00".to_string(),
                "18:00".to_string(),
                "00:00".to_string(),
            ],
            timezone: "Europe/London".to_string(),
            days: vec!["ALL".to_string()],
            warning_time: 300,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct ChatAlertConfig {
    pub enabled: bool,
    pub format: String,
}

impl Default for ChatAlertConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            format: "§8[§cRedstone§8] §eServer will restart in §c{time}§e!".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct TitleAlertConfig {
    pub enabled: bool,
    pub main_title: String,
    pub sub_title: String,
}

impl Default for TitleAlertConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            main_title: "§c⚡ Server Restart".to_string(),
            sub_title: "§ein §c{time}".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct ActionBarAlertConfig {
    pub enabled: bool,
    pub format: String,
}

impl Default for ActionBarAlertConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            format: "§8[§cRedstone§8] §eRestart in: §c{time}".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct SoundAlertConfig {
    pub enabled: bool,
    pub sound_name: String,
}

impl Default for SoundAlertConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            sound_name: "BLOCK_NOTE_BLOCK_PLING".to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct AlertsConfig {
    pub enabled: bool,
    pub warning_times: Vec<u32>,
    pub chat: ChatAlertConfig,
    pub title: TitleAlertConfig,
    pub actionbar: ActionBarAlertConfig,
    pub sound: SoundAlertConfig,
}

impl Default for AlertsConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            warning_times: vec![
                3600, 1800, 900, 600, 300, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1,
            ],
            chat: ChatAlertConfig::default(),
            title: TitleAlertConfig::default(),
            actionbar: ActionBarAlertConfig::default(),
            sound: SoundAlertConfig::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct AdvancedConfig {
    pub history_file: String,
    pub shutdown_delay_ticks: u32,
}

impl Default for AdvancedConfig {
    fn default() -> Self {
        Self {
            history_file: "logs/redstonereboot/restarts.log".to_string(),
            shutdown_delay_ticks: 60,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct BackendConfig {
    pub active_backend: String,
    pub auto_generate_script: bool,
    pub script_name: String,
    pub lockout_duration_seconds: u32,
}

impl Default for BackendConfig {
    fn default() -> Self {
        Self {
            active_backend: "AUTO".to_string(),
            auto_generate_script: true,
            script_name: "run.bat".to_string(),
            lockout_duration_seconds: 300,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "kebab-case", default)]
pub struct PluginConfig {
    pub general: GeneralConfig,
    pub scheduled_restarts: ScheduledRestartsConfig,
    pub alerts: AlertsConfig,
    pub advanced: AdvancedConfig,
    pub backend: BackendConfig,
    pub config_version: u32,
}

impl Default for PluginConfig {
    fn default() -> Self {
        Self {
            general: GeneralConfig::default(),
            scheduled_restarts: ScheduledRestartsConfig::default(),
            alerts: AlertsConfig::default(),
            advanced: AdvancedConfig::default(),
            backend: BackendConfig::default(),
            config_version: 4,
        }
    }
}

impl PluginConfig {
    pub fn load_from_str(content: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let config: PluginConfig = serde_yaml::from_str(content)?;
        Ok(config)
    }

    pub fn load_from_path(path: &Path) -> Result<Self, Box<dyn std::error::Error>> {
        let content = fs::read_to_string(path)?;
        Self::load_from_str(&content)
    }

    pub fn save_to_path(&self, path: &Path) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)?;
        }
        let content = serde_yaml::to_string(self)?;
        fs::write(path, content)?;
        Ok(())
    }

    pub fn resolve_history_path(&self, data_folder: &Path) -> PathBuf {
        let raw = self.advanced.history_file.trim();
        if raw.is_empty() {
            return data_folder.join("restarts.log");
        }

        if let Some(stripped) = raw.strip_prefix("{dataFolder}/") {
            return data_folder.join(stripped);
        }
        if let Some(stripped) = raw.strip_prefix("{dataFolder}\\") {
            return data_folder.join(stripped);
        }
        if let Some(stripped) = raw.strip_prefix("dataFolder/") {
            return data_folder.join(stripped);
        }
        if let Some(stripped) = raw.strip_prefix("dataFolder\\") {
            return data_folder.join(stripped);
        }

        PathBuf::from(raw)
    }
}
