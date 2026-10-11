use crate::config::PluginConfig;
use crate::manager::{RestartManager, RestartReason};
use std::path::PathBuf;
use std::sync::Arc;

pub const VERSION: &str = "1.6.7";
pub const PLATFORM: &str = "Pumpkin";

pub trait MessageSender {
    fn send(&mut self, message: &str);
}

pub struct BufferSender {
    pub messages: Vec<String>,
}

impl BufferSender {
    pub fn new() -> Self {
        Self {
            messages: Vec::new(),
        }
    }
}

impl Default for BufferSender {
    fn default() -> Self {
        Self::new()
    }
}

impl MessageSender for BufferSender {
    fn send(&mut self, message: &str) {
        self.messages.push(message.to_string());
    }
}

pub struct CommandProcessor {
    manager: Arc<RestartManager>,
    config_path: Option<PathBuf>,
}

impl CommandProcessor {
    pub fn new(manager: Arc<RestartManager>, config_path: Option<PathBuf>) -> Self {
        Self {
            manager,
            config_path,
        }
    }

    pub fn handle_status(&self, sender: &mut dyn MessageSender) {
        let status = self.manager.status();

        sender.send("§6=== RedstoneReboot Status ===");
        sender.send(&format!("§7Version: §f{}", VERSION));
        sender.send(&format!("§7Platform: §f{}", PLATFORM));
        sender.send(&format!("§7Backend: §f{}", self.manager.backend_summary()));
        {
            let backend_lock = self.manager.backend();
            let b = backend_lock.read().unwrap();
            if b.effective_type() == crate::backend::BackendType::LocalScript
                && b.state() != crate::backend::BackendState::Full
            {
                sender
                    .send("§eNotice: Start the server using run.bat to enable auto restart loops.");
            }
        }

        if status.is_restart_in_progress {
            if status.seconds_remaining < 0 {
                sender.send("§cStatus: §lRestart executing...");
            } else {
                sender.send(&format!(
                    "§cStatus: §lRestart in progress §r§7(§e{}s remaining§7)",
                    status.seconds_remaining
                ));
            }
            if let Some(reason) = status.reason {
                sender.send(&format!("§7Reason: §f{}", reason.display_name()));
            }
        } else {
            sender.send("§aStatus: §fNormal operation");
        }

        if let Some(next) = status.next_scheduled {
            sender.send(&format!(
                "§bNext: §f{} {}",
                next.format("%Y-%m-%d %H:%M:%S"),
                status.timezone
            ));
        }

        if let Some(update) = crate::updater::get_cached_update() {
            if update.has_update || crate::updater::is_newer_version(VERSION, &update.latest_version) {
                sender.send(&format!(
                    "§eUpdate: §aNew v{} available! §7(Run /reboot checkupdate or visit demonz.org)",
                    update.latest_version
                ));
            } else {
                sender.send(&format!("§aUpdate: §fUp to date (v{})", VERSION));
            }
        }
    }

    pub fn handle_now(
        &self,
        sender: &mut dyn MessageSender,
        sender_name: &str,
        delay: Option<u32>,
    ) {
        let delay_seconds = delay.unwrap_or(0);
        let scheduled =
            self.manager
                .schedule_restart(delay_seconds, RestartReason::Manual, sender_name);

        if scheduled {
            if delay_seconds == 0 {
                sender.send(&format!(
                    "§aImmediate restart triggered by {}.",
                    sender_name
                ));
            } else {
                sender.send(&format!(
                    "§aRestart triggered by {} in {}s.",
                    sender_name, delay_seconds
                ));
            }
        } else {
            sender.send("§eA sooner restart is already in progress.");
        }
    }

    pub fn handle_schedule(
        &self,
        sender: &mut dyn MessageSender,
        sender_name: &str,
        delay: Option<u32>,
    ) {
        let delay_seconds = delay.unwrap_or(0);
        let scheduled =
            self.manager
                .schedule_restart(delay_seconds, RestartReason::ScheduledApi, sender_name);

        if scheduled {
            sender.send(&format!(
                "§aManual restart scheduled in {}s.",
                delay_seconds
            ));
        } else {
            sender.send("§eA sooner restart is already in progress.");
        }
    }

    pub fn handle_cancel(&self, sender: &mut dyn MessageSender, sender_name: &str) {
        let cancelled = self.manager.cancel_restart(sender_name);
        if cancelled {
            sender.send("§aRestart cancelled.");
        } else {
            sender.send("§eNo restart pending.");
        }
    }

    pub fn handle_reload(&self, sender: &mut dyn MessageSender) {
        if let Some(path) = &self.config_path {
            match PluginConfig::load_from_path(path) {
                Ok(loaded) => {
                    self.manager.reload(loaded);
                    sender.send("§aConfiguration reloaded successfully.");
                }
                Err(err) => {
                    sender.send(&format!("§cFailed to reload config: {}", err));
                }
            }
        } else {
            self.manager.recalculate_next_restart();
            sender.send("§aRuntime schedule refreshed with existing configuration.");
        }
    }

    pub fn handle_history(&self, sender: &mut dyn MessageSender) {
        sender.send("§6=== Recent Restarts ===");
        let recent = self.manager.history().lock().unwrap().recent(10);
        if recent.is_empty() {
            sender.send("§7No restart events recorded this session.");
            return;
        }

        for entry in recent {
            sender.send(&format!("§7{}", entry.to_display_string()));
        }
    }

    pub fn handle_check_update(&self, sender: &mut dyn MessageSender) {
        sender.send("§7Checking for updates from demonz.org...");
        match crate::updater::fetch_updates(VERSION) {
            Ok(update) => {
                if update.has_update || crate::updater::is_newer_version(VERSION, &update.latest_version) {
                    sender.send(&format!(
                        "§aA new version is available: §ev{} §7(Current: §f{}§7)",
                        update.latest_version, VERSION
                    ));
                    if let Some(dl) = update
                        .download
                        .as_ref()
                        .and_then(|d| d.direct_url.as_ref().or(d.url.as_ref()))
                    {
                        sender.send(&format!("§7Download: §b{}", dl));
                    }
                } else {
                    sender.send(&format!("§aRedstoneReboot is up to date (v{}).", VERSION));
                }
            }
            Err(err) => {
                sender.send(&format!("§cFailed to check updates: {}", err));
            }
        }
    }

    pub fn handle_help(&self, sender: &mut dyn MessageSender) {
        sender.send("§6=== RedstoneReboot Commands ===");
        sender.send("§7/reboot status §8- §fView restart status");
        sender.send("§7/reboot now [delay] §8- §fRestart now");
        sender.send("§7/reboot schedule <seconds> §8- §fSchedule restart");
        sender.send("§7/reboot cancel §8- §fCancel restart");
        sender.send("§7/reboot reload §8- §fReload config");
        sender.send("§7/reboot history §8- §fRecent restart events");
        sender.send("§7/reboot checkupdate §8- §fCheck for updates");
        sender.send("§7/reboot help §8- §fShow this menu");
    }

    pub fn dispatch(&self, sender: &mut dyn MessageSender, sender_name: &str, args: &[&str]) {
        if args.is_empty() {
            self.handle_status(sender);
            return;
        }

        match args[0].to_ascii_lowercase().as_str() {
            "status" => self.handle_status(sender),
            "now" => {
                if args.len() > 1 {
                    match args[1].parse::<u32>() {
                        Ok(sec) => self.handle_now(sender, sender_name, Some(sec)),
                        Err(_) => sender
                            .send("§cInvalid delay value. Expected number of seconds (e.g. 60)."),
                    }
                } else {
                    self.handle_now(sender, sender_name, Some(0));
                }
            }
            "schedule" => {
                if args.len() > 1 {
                    match args[1].parse::<u32>() {
                        Ok(sec) => self.handle_schedule(sender, sender_name, Some(sec)),
                        Err(_) => sender
                            .send("§cInvalid delay value. Expected number of seconds (e.g. 300)."),
                    }
                } else {
                    sender.send("§cUsage: /reboot schedule <seconds>");
                }
            }
            "cancel" => self.handle_cancel(sender, sender_name),
            "reload" => self.handle_reload(sender),
            "history" => self.handle_history(sender),
            "checkupdate" | "update" => self.handle_check_update(sender),
            "help" => self.handle_help(sender),
            _ => {
                sender.send("§cUnknown subcommand. Use /reboot help for command list.");
            }
        }
    }
}
