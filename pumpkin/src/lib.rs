pub mod alerts;
pub mod backend;
pub mod commands;
pub mod config;
pub mod history;
pub mod manager;
pub mod schedule;

use alerts::AlertFormatter;
use commands::{CommandProcessor, MessageSender};
use config::PluginConfig;
use history::RestartHistory;
use manager::{RestartManager, TickAction};
use pumpkin_plugin_api::command::{
    Arg, ArgumentType, Command, CommandError, CommandNode, ConsumedArgs,
};
use pumpkin_plugin_api::commands::CommandHandler;
use pumpkin_plugin_api::scheduler::SchedulerExt;
use pumpkin_plugin_api::text::TextComponent;
use pumpkin_plugin_api::wit::pumpkin::plugin::sounds::SoundCategory;
use pumpkin_plugin_api::{Context, Plugin, PluginMetadata, Server, register_plugin};
use std::fs;
use std::path::PathBuf;
use std::sync::{Arc, RwLock};

static GLOBAL_PROCESSOR: RwLock<Option<Arc<CommandProcessor>>> = RwLock::new(None);
static GLOBAL_MANAGER: RwLock<Option<Arc<RestartManager>>> = RwLock::new(None);

struct PumpkinSender<'a> {
    sender: &'a pumpkin_plugin_api::command::CommandSender,
}

impl MessageSender for PumpkinSender<'_> {
    fn send(&mut self, message: &str) {
        let component = TextComponent::from_legacy_string(message);
        self.sender.send_message(component);
    }
}

struct RootCommandHandler;

impl CommandHandler for RootCommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_status(&mut wrapped);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct StatusSubcommandHandler;

impl CommandHandler for StatusSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_status(&mut wrapped);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct NowSubcommandHandler;

impl CommandHandler for NowSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let delay_val = match args.get_value("delay") {
                Arg::Num(Ok(num)) => match num {
                    pumpkin_plugin_api::wit::pumpkin::plugin::command::Number::Int32(v) => {
                        Some(v.max(0) as u32)
                    }
                    pumpkin_plugin_api::wit::pumpkin::plugin::command::Number::Int64(v) => {
                        Some(v.max(0) as u32)
                    }
                    _ => None,
                },
                _ => None,
            };

            let name = sender.get_name();
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_now(&mut wrapped, &name, delay_val);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct ScheduleSubcommandHandler;

impl CommandHandler for ScheduleSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let delay_val = match args.get_value("seconds") {
                Arg::Num(Ok(num)) => match num {
                    pumpkin_plugin_api::wit::pumpkin::plugin::command::Number::Int32(v) => {
                        Some(v.max(0) as u32)
                    }
                    pumpkin_plugin_api::wit::pumpkin::plugin::command::Number::Int64(v) => {
                        Some(v.max(0) as u32)
                    }
                    _ => None,
                },
                _ => None,
            };

            let name = sender.get_name();
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_schedule(&mut wrapped, &name, delay_val);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct CancelSubcommandHandler;

impl CommandHandler for CancelSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let name = sender.get_name();
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_cancel(&mut wrapped, &name);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct ReloadSubcommandHandler;

impl CommandHandler for ReloadSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_reload(&mut wrapped);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct HistorySubcommandHandler;

impl CommandHandler for HistorySubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_history(&mut wrapped);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

struct HelpSubcommandHandler;

impl CommandHandler for HelpSubcommandHandler {
    fn handle(
        &self,
        sender: pumpkin_plugin_api::command::CommandSender,
        _server: Server,
        _args: ConsumedArgs,
    ) -> Result<i32, CommandError> {
        let lock = GLOBAL_PROCESSOR.read().unwrap();
        if let Some(processor) = lock.as_ref() {
            let mut wrapped = PumpkinSender { sender: &sender };
            processor.handle_help(&mut wrapped);
            Ok(0)
        } else {
            Err(CommandError::CommandFailed(TextComponent::text(
                "RedstoneReboot engine not initialized",
            )))
        }
    }
}

pub struct RedstoneRebootPlugin;

impl Default for RedstoneRebootPlugin {
    fn default() -> Self {
        Self::new()
    }
}

impl Plugin for RedstoneRebootPlugin {
    fn new() -> Self {
        Self
    }

    fn metadata(&self) -> PluginMetadata {
        PluginMetadata {
            name: "RedstoneReboot".to_string(),
            version: commands::VERSION.to_string(),
            authors: vec!["Cyrusbye720".to_string()],
            description: "Automated restart and health monitoring engine for Pumpkin MC"
                .to_string(),
            dependencies: Vec::new(),
            permissions: vec![
                pumpkin_plugin_api::permissions::FS_READ_DATA.to_string(),
                pumpkin_plugin_api::permissions::FS_WRITE_DATA.to_string(),
                pumpkin_plugin_api::permissions::SYS_ENV.to_string(),
            ],
        }
    }

    fn on_load(&self, context: Context) -> pumpkin_plugin_api::Result<()> {
        let data_dir_str = context.get_data_folder();
        let data_folder = PathBuf::from(&data_dir_str);
        let _ = fs::create_dir_all(&data_folder);

        let config_path = data_folder.join("config.yml");
        let config = if config_path.exists() {
            PluginConfig::load_from_path(&config_path).unwrap_or_default()
        } else {
            let default_cfg = PluginConfig::default();
            let _ = default_cfg.save_to_path(&config_path);
            default_cfg
        };

        let history_path = config.resolve_history_path(&data_folder);
        let history = RestartHistory::new_with_data_folder(history_path, Some(&data_folder));
        let backend = backend::BackendManager::new(&config.backend, data_folder.clone());
        let backend_summary = backend.summary();
        pumpkin_plugin_api::logging::log(
            pumpkin_plugin_api::logging::LogLevel::Info,
            &format!("RedstoneReboot active backend: {}", backend_summary),
        );
        if backend.effective_type() == backend::BackendType::LocalScript
            && backend.state() != backend::BackendState::Full
        {
            pumpkin_plugin_api::logging::log(
                pumpkin_plugin_api::logging::LogLevel::Info,
                &format!("Restart wrapper script ready at: {}/run.bat", data_dir_str),
            );
        }
        let manager = Arc::new(RestartManager::new(config, history, backend));
        let processor = Arc::new(CommandProcessor::new(
            Arc::clone(&manager),
            Some(config_path),
        ));

        {
            let mut mgr_lock = GLOBAL_MANAGER.write().unwrap();
            *mgr_lock = Some(Arc::clone(&manager));
            let mut proc_lock = GLOBAL_PROCESSOR.write().unwrap();
            *proc_lock = Some(Arc::clone(&processor));
        }

        let command_names = ["reboot".to_string()];
        let status_node = CommandNode::literal("status").execute(StatusSubcommandHandler);

        let delay_arg = CommandNode::argument("delay", &ArgumentType::Integer((Some(0), None)))
            .execute(NowSubcommandHandler);
        let now_node = CommandNode::literal("now")
            .execute(NowSubcommandHandler)
            .then(delay_arg);

        let schedule_arg =
            CommandNode::argument("seconds", &ArgumentType::Integer((Some(0), None)))
                .execute(ScheduleSubcommandHandler);
        let schedule_node = CommandNode::literal("schedule").then(schedule_arg);

        let cancel_node = CommandNode::literal("cancel").execute(CancelSubcommandHandler);
        let reload_node = CommandNode::literal("reload").execute(ReloadSubcommandHandler);
        let history_node = CommandNode::literal("history").execute(HistorySubcommandHandler);
        let help_node = CommandNode::literal("help").execute(HelpSubcommandHandler);

        let command = Command::new(
            &command_names,
            "RedstoneReboot restart and server monitoring engine",
        )
        .execute(RootCommandHandler)
        .then(status_node)
        .then(now_node)
        .then(schedule_node)
        .then(cancel_node)
        .then(reload_node)
        .then(history_node)
        .then(help_node);

        context.register_command(command, "redstonereboot.use");

        let manager_for_task = Arc::clone(&manager);
        context.schedule_repeating_task(20, 20, move |server| {
            let action = manager_for_task.tick();
            match action {
                TickAction::None => {}
                TickAction::WarningAlert {
                    seconds_remaining,
                    reason,
                    ..
                } => {
                    let cfg = manager_for_task.config().read().unwrap().clone();
                    let time_str = AlertFormatter::format_duration(seconds_remaining as u64);

                    if cfg.alerts.chat.enabled {
                        let chat_msg = AlertFormatter::format_template(
                            &cfg.alerts.chat.format,
                            &time_str,
                            reason.display_name(),
                            &cfg.general.plugin_prefix,
                        );
                        server.broadcast(&chat_msg);
                    }

                    if cfg.alerts.title.enabled || cfg.alerts.actionbar.enabled || cfg.alerts.sound.enabled {
                        let players = server.get_all_players();
                        for player in players {
                            if cfg.alerts.title.enabled {
                                let main_title = AlertFormatter::format_template(
                                    &cfg.alerts.title.main_title,
                                    &time_str,
                                    reason.display_name(),
                                    &cfg.general.plugin_prefix,
                                );
                                let sub_title = AlertFormatter::format_template(
                                    &cfg.alerts.title.sub_title,
                                    &time_str,
                                    reason.display_name(),
                                    &cfg.general.plugin_prefix,
                                );
                                player.show_title(TextComponent::from_legacy_string(&main_title));
                                player.show_subtitle(TextComponent::from_legacy_string(&sub_title));
                            }

                            if cfg.alerts.actionbar.enabled {
                                let action_bar = AlertFormatter::format_template(
                                    &cfg.alerts.actionbar.format,
                                    &time_str,
                                    reason.display_name(),
                                    &cfg.general.plugin_prefix,
                                );
                                player.show_actionbar(TextComponent::from_legacy_string(&action_bar));
                            }

                            if cfg.alerts.sound.enabled {
                                let raw_sound = cfg.alerts.sound.sound_name.to_ascii_lowercase().replace('_', ".");
                                let sound_id = if raw_sound.contains(':') {
                                    raw_sound
                                } else {
                                    format!("minecraft:{}", raw_sound)
                                };
                                player.play_custom_sound(&sound_id, SoundCategory::Master, 1.0, 1.0);
                            }
                        }
                    }
                }
                TickAction::ExecuteShutdown { reason, .. } => {
                    let cfg = manager_for_task.config().read().unwrap().clone();
                    let final_msg = format!(
                        "{} §cServer is restarting NOW! Reason: §e{}",
                        cfg.general.plugin_prefix,
                        reason.display_name()
                    );
                    server.broadcast(&final_msg);
                    let _ = server.save_all();
                    manager_for_task.backend().read().unwrap().arm_restart_marker();

                    let shutdown_delay = cfg.advanced.shutdown_delay_ticks as u64;
                    if shutdown_delay > 0 {
                        server.schedule_delayed_task(shutdown_delay, |s| {
                            s.execute_command(
                                "stop",
                                pumpkin_plugin_api::wit::pumpkin::plugin::server::CommandSender::Console,
                            );
                        });
                    } else {
                        server.execute_command(
                            "stop",
                            pumpkin_plugin_api::wit::pumpkin::plugin::server::CommandSender::Console,
                        );
                    }
                }
            }
        });

        Ok(())
    }

    fn on_unload(&self, _context: Context) -> pumpkin_plugin_api::Result<()> {
        if let Some(manager) = GLOBAL_MANAGER.write().unwrap().take() {
            manager.cancel_restart("Plugin Unload");
        }
        let _ = GLOBAL_PROCESSOR.write().unwrap().take();
        Ok(())
    }
}

register_plugin!(RedstoneRebootPlugin);
