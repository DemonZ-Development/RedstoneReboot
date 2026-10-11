use chrono::{DateTime, NaiveDate, NaiveTime, TimeZone, Weekday};
use chrono_tz::Tz;
use redstonereboot_pumpkin::alerts::AlertFormatter;
use redstonereboot_pumpkin::backend::{BackendManager, BackendState, BackendType};
use redstonereboot_pumpkin::commands::{BufferSender, CommandProcessor};
use redstonereboot_pumpkin::config::PluginConfig;
use redstonereboot_pumpkin::history::{HistoryEntry, RestartHistory};
use redstonereboot_pumpkin::manager::{RestartManager, RestartReason, TickAction};
use redstonereboot_pumpkin::schedule::RestartScheduleCalculator;
use std::fs;
use std::path::PathBuf;
use std::sync::Arc;
use tempfile::tempdir;

#[test]
fn test_config_defaults() {
    let cfg = PluginConfig::default();
    assert_eq!(cfg.general.plugin_prefix, "§8[§cRedstone§8] §aReboot");
    assert!(!cfg.general.debug_mode);
    assert!(cfg.scheduled_restarts.enabled);
    assert_eq!(cfg.scheduled_restarts.timezone, "Europe/London");
    assert_eq!(cfg.scheduled_restarts.warning_time, 300);
    assert_eq!(
        cfg.scheduled_restarts.times,
        vec!["06:00", "12:00", "18:00", "00:00"]
    );
    assert_eq!(cfg.scheduled_restarts.days, vec!["ALL"]);
    assert!(cfg.alerts.enabled);
    assert!(cfg.alerts.warning_times.contains(&300));
    assert!(cfg.alerts.warning_times.contains(&60));
    assert_eq!(
        cfg.advanced.history_file,
        "logs/redstonereboot/restarts.log"
    );
    assert_eq!(cfg.backend.active_backend, "AUTO");
    assert!(cfg.backend.auto_generate_script);
    assert_eq!(cfg.backend.script_name, "run.bat");
}

#[test]
fn test_config_serialization_roundtrip() {
    let dir = tempdir().unwrap();
    let config_path = dir.path().join("config.yml");

    let cfg = PluginConfig::default();
    cfg.save_to_path(&config_path).unwrap();
    assert!(config_path.exists());

    let loaded = PluginConfig::load_from_path(&config_path).unwrap();
    assert_eq!(cfg, loaded);
}

#[test]
fn test_config_resolve_history_path() {
    let dir = tempdir().unwrap();
    let data_folder = dir.path();

    let mut cfg = PluginConfig::default();
    cfg.advanced.history_file = "{dataFolder}/custom_restarts.log".to_string();
    assert_eq!(
        cfg.resolve_history_path(data_folder),
        data_folder.join("custom_restarts.log")
    );

    cfg.advanced.history_file = "dataFolder/nested/restarts.log".to_string();
    assert_eq!(
        cfg.resolve_history_path(data_folder),
        data_folder.join("nested/restarts.log")
    );

    cfg.advanced.history_file = "logs/redstonereboot/restarts.log".to_string();
    assert_eq!(
        cfg.resolve_history_path(data_folder),
        PathBuf::from("logs/redstonereboot/restarts.log")
    );
}

#[test]
fn test_schedule_parse_time() {
    assert_eq!(
        RestartScheduleCalculator::parse_time("06:00"),
        Some(NaiveTime::from_hms_opt(6, 0, 0).unwrap())
    );
    assert_eq!(
        RestartScheduleCalculator::parse_time("06:00:00"),
        Some(NaiveTime::from_hms_opt(6, 0, 0).unwrap())
    );
    assert_eq!(
        RestartScheduleCalculator::parse_time("18:30"),
        Some(NaiveTime::from_hms_opt(18, 30, 0).unwrap())
    );
    assert_eq!(
        RestartScheduleCalculator::parse_time("18:30:45"),
        Some(NaiveTime::from_hms_opt(18, 30, 45).unwrap())
    );
    assert_eq!(
        RestartScheduleCalculator::parse_time("0:00"),
        Some(NaiveTime::from_hms_opt(0, 0, 0).unwrap())
    );
    assert_eq!(RestartScheduleCalculator::parse_time("invalid"), None);
    assert_eq!(RestartScheduleCalculator::parse_time(""), None);
}

#[test]
fn test_schedule_parse_days() {
    let all = RestartScheduleCalculator::parse_days(&["ALL".to_string()]);
    assert_eq!(all.len(), 7);

    let specific = RestartScheduleCalculator::parse_days(&[
        "MONDAY".to_string(),
        "TUES".to_string(),
        "THURS".to_string(),
        "FRI".to_string(),
    ]);
    assert_eq!(specific.len(), 4);
    assert!(specific.contains(&Weekday::Mon));
    assert!(specific.contains(&Weekday::Tue));
    assert!(specific.contains(&Weekday::Thu));
    assert!(specific.contains(&Weekday::Fri));
}

#[test]
fn test_schedule_calculate_next_restart_same_day() {
    let tz: Tz = "UTC".parse().unwrap();
    let date = NaiveDate::from_ymd_opt(2026, 10, 10).unwrap();
    let time = NaiveTime::from_hms_opt(10, 0, 0).unwrap();
    let now: DateTime<Tz> = tz.from_utc_datetime(&date.and_time(time));

    let times = vec![
        "06:00".to_string(),
        "12:00".to_string(),
        "18:00".to_string(),
    ];
    let days = vec!["ALL".to_string()];

    let next = RestartScheduleCalculator::calculate_next_restart(now, &times, &days);
    assert!(next.is_some());
    let next_dt = next.unwrap();
    assert_eq!(next_dt.date_naive(), date);
    assert_eq!(next_dt.time(), NaiveTime::from_hms_opt(12, 0, 0).unwrap());
}

#[test]
fn test_schedule_calculate_next_restart_next_day() {
    let tz: Tz = "UTC".parse().unwrap();
    let date = NaiveDate::from_ymd_opt(2026, 10, 10).unwrap();
    let time = NaiveTime::from_hms_opt(20, 0, 0).unwrap();
    let now: DateTime<Tz> = tz.from_utc_datetime(&date.and_time(time));

    let times = vec!["06:00".to_string(), "12:00".to_string()];
    let days = vec!["ALL".to_string()];

    let next = RestartScheduleCalculator::calculate_next_restart(now, &times, &days);
    assert!(next.is_some());
    let next_dt = next.unwrap();
    assert_eq!(
        next_dt.date_naive(),
        NaiveDate::from_ymd_opt(2026, 10, 11).unwrap()
    );
    assert_eq!(next_dt.time(), NaiveTime::from_hms_opt(6, 0, 0).unwrap());
}

#[test]
fn test_alerts_formatting() {
    assert_eq!(AlertFormatter::format_duration(45), "45s");
    assert_eq!(AlertFormatter::format_duration(60), "1m");
    assert_eq!(AlertFormatter::format_duration(125), "2m 5s");
    assert_eq!(AlertFormatter::format_duration(3600), "1h");
    assert_eq!(AlertFormatter::format_duration(3665), "1h 1m");

    let template = "Server restarting in {time}! Reason: {reason}";
    let formatted = AlertFormatter::format_template(template, "5m", "Scheduled", "");
    assert_eq!(formatted, "Server restarting in 5m! Reason: Scheduled");
}

#[test]
fn test_alerts_color_translation_and_stripping() {
    let raw = "&cServer &aRestart &8[&eNow&8]";
    let translated = AlertFormatter::translate_color_codes(raw);
    assert_eq!(translated, "§cServer §aRestart §8[§eNow§8]");

    let stripped = AlertFormatter::strip_color_codes(&translated);
    assert_eq!(stripped, "Server Restart [Now]");
}

#[test]
fn test_history_record_and_recent() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("logs").join("restarts.log");

    let mut history = RestartHistory::new(log_file.clone());
    history
        .record("SCHEDULED", "Regular Reboot", "System")
        .unwrap();
    history
        .record("MANUAL", "Manual Restart", "Cyrusbye720")
        .unwrap();

    let recent = history.recent(10);
    assert_eq!(recent.len(), 2);
    assert_eq!(recent[0].event_type, "SCHEDULED");
    assert_eq!(recent[0].reason, "Regular Reboot");
    assert_eq!(recent[0].initiator, "System");

    assert_eq!(recent[1].event_type, "MANUAL");
    assert_eq!(recent[1].reason, "Manual Restart");
    assert_eq!(recent[1].initiator, "Cyrusbye720");

    let file_content = fs::read_to_string(&log_file).unwrap();
    assert!(file_content.contains("SCHEDULED"));
    assert!(file_content.contains("MANUAL"));
    assert!(file_content.contains("Cyrusbye720"));
}

#[test]
fn test_history_parse_log_line() {
    let line = "2026-10-10 18:00:00 | SCHEDULED | reason=Scheduled | initiator=System";
    let parsed = HistoryEntry::parse_log_line(line);
    assert!(parsed.is_some());
    let entry = parsed.unwrap();
    assert_eq!(entry.event_type, "SCHEDULED");
    assert_eq!(entry.reason, "Scheduled");
    assert_eq!(entry.initiator, "System");
}

#[test]
fn test_restart_manager_scheduling_and_cancellation() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");

    let cfg = PluginConfig::default();
    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = RestartManager::new(cfg, history, backend);

    assert!(!manager.status().is_restart_in_progress);

    let scheduled = manager.schedule_restart(60, RestartReason::Manual, "Admin");
    assert!(scheduled);

    let status = manager.status();
    assert!(status.is_restart_in_progress);
    assert_eq!(status.seconds_remaining, 60);
    assert_eq!(status.reason, Some(RestartReason::Manual));
    assert_eq!(status.initiator, Some("Admin".to_string()));

    let cancelled = manager.cancel_restart("Admin");
    assert!(cancelled);
    assert!(!manager.status().is_restart_in_progress);

    let cancelled_again = manager.cancel_restart("Admin");
    assert!(!cancelled_again);
}

#[test]
fn test_restart_manager_tick_progression_and_alert() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");

    let mut cfg = PluginConfig::default();
    cfg.alerts.warning_times = vec![5, 3, 1];

    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = RestartManager::new(cfg, history, backend);

    manager.schedule_restart(5, RestartReason::Manual, "Tester");

    let action = manager.tick();
    assert_eq!(
        action,
        TickAction::WarningAlert {
            seconds_remaining: 5,
            reason: RestartReason::Manual,
            initiator: "Tester".to_string()
        }
    );

    let action = manager.tick();
    assert_eq!(action, TickAction::None);

    let action = manager.tick();
    assert_eq!(
        action,
        TickAction::WarningAlert {
            seconds_remaining: 3,
            reason: RestartReason::Manual,
            initiator: "Tester".to_string()
        }
    );

    let action = manager.tick();
    assert_eq!(action, TickAction::None);

    let action = manager.tick();
    assert_eq!(
        action,
        TickAction::WarningAlert {
            seconds_remaining: 1,
            reason: RestartReason::Manual,
            initiator: "Tester".to_string()
        }
    );

    let action = manager.tick();
    assert_eq!(
        action,
        TickAction::ExecuteShutdown {
            reason: RestartReason::Manual,
            initiator: "Tester".to_string()
        }
    );

    assert!(!manager.status().is_restart_in_progress);
}

#[test]
fn test_command_processor_dispatch() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");
    let cfg_path = dir.path().join("config.yml");

    let cfg = PluginConfig::default();
    cfg.save_to_path(&cfg_path).unwrap();

    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = Arc::new(RestartManager::new(cfg, history, backend));
    let processor = CommandProcessor::new(Arc::clone(&manager), Some(cfg_path));

    let mut sender = BufferSender::new();
    processor.dispatch(&mut sender, "Cyrusbye720", &["status"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("RedstoneReboot Status"))
    );
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Normal operation"))
    );

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["now", "30"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Restart triggered by Cyrusbye720 in 30s"))
    );
    assert!(manager.status().is_restart_in_progress);

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["cancel"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Restart cancelled."))
    );
    assert!(!manager.status().is_restart_in_progress);

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["reload"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("reloaded successfully"))
    );

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["history"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Recent Restarts"))
    );

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["schedule", "120"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Manual restart scheduled in 120s"))
    );
    assert!(manager.status().is_restart_in_progress);

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["help"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("/reboot schedule"))
    );
}

#[test]
fn test_command_processor_invalid_delay_safety() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");
    let cfg = PluginConfig::default();
    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = Arc::new(RestartManager::new(cfg, history, backend));
    let processor = CommandProcessor::new(Arc::clone(&manager), None);

    let mut sender = BufferSender::new();
    processor.dispatch(&mut sender, "Cyrusbye720", &["now", "60s"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Invalid delay value"))
    );
    assert!(!manager.status().is_restart_in_progress);

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["schedule", "invalid"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Invalid delay value"))
    );
    assert!(!manager.status().is_restart_in_progress);

    sender.messages.clear();
    processor.dispatch(&mut sender, "Cyrusbye720", &["schedule"]);
    assert!(
        sender
            .messages
            .iter()
            .any(|m| m.contains("Usage: /reboot schedule"))
    );
    assert!(!manager.status().is_restart_in_progress);
}

#[test]
fn test_scheduled_restart_tick_no_double_alert() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");

    let mut cfg = PluginConfig::default();
    cfg.scheduled_restarts.enabled = true;
    cfg.scheduled_restarts.warning_time = 300;
    cfg.alerts.warning_times = vec![300, 60, 10];

    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = RestartManager::new(cfg, history, backend);

    manager.schedule_restart(300, RestartReason::Scheduled, "Scheduled System");

    let first_tick = manager.tick();
    assert_eq!(
        first_tick,
        TickAction::WarningAlert {
            seconds_remaining: 300,
            reason: RestartReason::Scheduled,
            initiator: "Scheduled System".to_string()
        }
    );

    let second_tick = manager.tick();
    assert_ne!(
        second_tick,
        TickAction::WarningAlert {
            seconds_remaining: 300,
            reason: RestartReason::Scheduled,
            initiator: "Scheduled System".to_string()
        }
    );
    assert_eq!(second_tick, TickAction::None);
    assert_eq!(manager.status().seconds_remaining, 298);
}

#[test]
fn test_load_bukkit_config_yml() {
    let bukkit_config_path = std::path::Path::new("../bukkit/src/main/resources/config.yml");
    if bukkit_config_path.exists() {
        let loaded = PluginConfig::load_from_path(bukkit_config_path).unwrap();
        assert_eq!(loaded.general.plugin_prefix, "§8[§cRedstone§8] §aReboot");
        assert_eq!(loaded.scheduled_restarts.timezone, "Europe/London");
        assert_eq!(loaded.scheduled_restarts.warning_time, 300);
        assert_eq!(loaded.advanced.shutdown_delay_ticks, 60);
        assert_eq!(loaded.config_version, 4);
    }
}

#[test]
fn test_history_migrate_data_folder() {
    let dir = tempdir().unwrap();
    let data_folder = dir.path().join("plugin_data");
    fs::create_dir_all(&data_folder).unwrap();

    let legacy_file = data_folder.join("restarts.log");
    fs::write(
        &legacy_file,
        "2026-10-10 12:00:00 | MANUAL | reason=Manual | initiator=Admin\n",
    )
    .unwrap();

    let new_target = data_folder.join("logs").join("restarts.log");
    let history = RestartHistory::new_with_data_folder(new_target.clone(), Some(&data_folder));

    assert!(new_target.exists());
    assert!(!legacy_file.exists());
    let recent = history.recent(1);
    assert_eq!(recent.len(), 1);
    assert_eq!(recent[0].event_type, "MANUAL");
}

#[test]
fn test_backend_mode_parsing() {
    assert_eq!(BackendType::parse("AUTO"), BackendType::Auto);
    assert_eq!(BackendType::parse("auto"), BackendType::Auto);
    assert_eq!(BackendType::parse("LOCAL_SCRIPT"), BackendType::LocalScript);
    assert_eq!(BackendType::parse("localscript"), BackendType::LocalScript);
    assert_eq!(
        BackendType::parse("DEPEND_ON_HOST"),
        BackendType::DependOnHost
    );
    assert_eq!(BackendType::parse("host"), BackendType::DependOnHost);
    assert_eq!(BackendType::parse("DOCKER"), BackendType::Docker);
    assert_eq!(BackendType::parse("PTERODACTYL"), BackendType::Pterodactyl);
    assert_eq!(BackendType::parse("SYSTEMD"), BackendType::Systemd);
}

#[test]
fn test_backend_script_generation() {
    let dir = tempdir().unwrap();
    let data_folder = dir.path().join("RedstoneReboot");
    fs::create_dir_all(&data_folder).unwrap();

    BackendManager::generate_scripts(&data_folder).unwrap();

    let bat_file = data_folder.join("run.bat");
    let sh_file = data_folder.join("run.sh");

    assert!(bat_file.exists());
    assert!(sh_file.exists());

    let bat_content = fs::read_to_string(&bat_file).unwrap();
    assert!(bat_content.contains("Pumpkin Server Wrapper"));
    assert!(bat_content.contains(".redstonereboot_restart"));
    assert!(bat_content.contains("pumpkin-X64-Windows.exe"));

    let sh_content = fs::read_to_string(&sh_file).unwrap();
    assert!(sh_content.contains("pumpkin-X64-Linux"));
    assert!(sh_content.contains(".redstonereboot_restart"));
}

#[test]
fn test_backend_auto_switch_and_state() {
    let dir = tempdir().unwrap();
    let data_folder = dir.path().join("RedstoneReboot");
    fs::create_dir_all(&data_folder).unwrap();

    let cfg = PluginConfig::default();
    let backend = BackendManager::new(&cfg.backend, data_folder.clone());

    assert_eq!(backend.configured_type(), BackendType::Auto);
    assert_eq!(backend.effective_type(), BackendType::LocalScript);
    assert_eq!(backend.state(), BackendState::Generated);
    assert!(data_folder.join("run.bat").exists());

    fs::write(data_folder.join(".redstonereboot_wired"), "1").unwrap();
    let wired_backend = BackendManager::new(&cfg.backend, data_folder);
    assert_eq!(wired_backend.state(), BackendState::Full);
    assert!(wired_backend.summary().contains("Active Wrapper Loop"));
}

#[test]
fn test_backend_arm_restart_marker() {
    let dir = tempdir().unwrap();
    let data_folder = dir.path().join("RedstoneReboot");
    fs::create_dir_all(&data_folder).unwrap();

    let cfg = PluginConfig::default();
    let backend = BackendManager::new(&cfg.backend, data_folder.clone());

    assert!(!data_folder.join(".redstonereboot_restart").exists());
    backend.arm_restart_marker();
    assert!(data_folder.join(".redstonereboot_restart").exists());
}

#[test]
fn test_status_includes_backend() {
    let dir = tempdir().unwrap();
    let log_file = dir.path().join("restarts.log");
    let cfg_path = dir.path().join("config.yml");

    let cfg = PluginConfig::default();
    cfg.save_to_path(&cfg_path).unwrap();

    let history = RestartHistory::new(log_file);
    let backend = BackendManager::new(&cfg.backend, dir.path().to_path_buf());
    let manager = Arc::new(RestartManager::new(cfg, history, backend));
    let processor = CommandProcessor::new(Arc::clone(&manager), Some(cfg_path));

    let mut sender = BufferSender::new();
    processor.dispatch(&mut sender, "Cyrusbye720", &["status"]);
    assert!(sender.messages.iter().any(|m| m.contains("Backend:")));
}

#[test]
fn test_config_load_and_migrate_from_v1() {
    let dir = tempdir().unwrap();
    let config_path = dir.path().join("config.yml");

    let legacy_yaml = r#"
general:
  plugin-prefix: "§c[OldPrefix] "
  debug-mode: true
  strict-validation: false
scheduled-restarts:
  enabled: true
  times:
    - "04:00"
  timezone: "UTC"
  days:
    - "ALL"
  warning-time: 120
config-version: 1
"#;
    fs::write(&config_path, legacy_yaml).unwrap();

    let migrated = PluginConfig::load_and_migrate(&config_path).unwrap();
    assert_eq!(migrated.config_version, 4);
    assert_eq!(migrated.general.plugin_prefix, "§c[OldPrefix] ");
    assert!(migrated.general.debug_mode);
    assert_eq!(migrated.scheduled_restarts.times, vec!["04:00"]);
    assert_eq!(migrated.advanced.history_file, "logs/redstonereboot/restarts.log");
    assert!(migrated.advanced.check_updates);
    assert!(migrated.advanced.backup_on_migration);

    let backup_file = dir.path().join("config.yml.v1.backup");
    assert!(backup_file.exists());
    let backup_content = fs::read_to_string(&backup_file).unwrap();
    assert!(backup_content.contains("config-version: 1"));

    let reloaded = PluginConfig::load_from_path(&config_path).unwrap();
    assert_eq!(reloaded.config_version, 4);
    assert_eq!(reloaded.advanced.history_file, "logs/redstonereboot/restarts.log");
}

#[test]
fn test_config_load_and_migrate_from_v3() {
    let dir = tempdir().unwrap();
    let config_path = dir.path().join("config.yml");

    let v3_yaml = r#"
general:
  plugin-prefix: "§8[§cRedstone§8] §aReboot"
advanced:
  shutdown-delay-ticks: 40
  backup-on-migration: true
config-version: 3
"#;
    fs::write(&config_path, v3_yaml).unwrap();

    let migrated = PluginConfig::load_and_migrate(&config_path).unwrap();
    assert_eq!(migrated.config_version, 4);
    assert_eq!(migrated.advanced.shutdown_delay_ticks, 40);
    assert_eq!(migrated.advanced.history_file, "logs/redstonereboot/restarts.log");

    let backup_file = dir.path().join("config.yml.v3.backup");
    assert!(backup_file.exists());
}

#[test]
fn test_updater_is_newer_version() {
    use redstonereboot_pumpkin::updater::is_newer_version;

    assert!(is_newer_version("1.6.6", "1.6.7"));
    assert!(is_newer_version("1.6.7", "1.6.8"));
    assert!(is_newer_version("1.6.7", "1.7.0"));
    assert!(is_newer_version("1.6.7", "2.0.0"));
    assert!(is_newer_version("v1.6.6", "1.6.7"));
    assert!(is_newer_version("1.6.7-beta", "1.6.8"));

    assert!(!is_newer_version("1.6.7", "1.6.7"));
    assert!(!is_newer_version("1.6.7", "1.6.6"));
    assert!(!is_newer_version("2.0.0", "1.6.7"));
    assert!(!is_newer_version("1.7.0", "1.6.7"));
}

#[test]
fn test_updater_parse_response_json() {
    use redstonereboot_pumpkin::updater::UpdateResponse;

    let json_data = r#"{
        "project": {"name": "RedstoneReboot"},
        "current_version": "1.6.7",
        "latest_version": "1.6.8",
        "has_update": true,
        "update_type": "patch",
        "platform": "pumpkin",
        "download": {
            "url": "https://demonz.org/api/projects/download/redstonereboot?platform=pumpkin",
            "direct_url": "https://github.com/DemonZ-Development/RedstoneReboot/releases/download/v1.6.8/redstonereboot_pumpkin.wasm",
            "file_name": "redstonereboot_pumpkin.wasm"
        },
        "changelog": {
            "latest": {
                "version": "1.6.8",
                "notes": "Bug fixes and enhancements"
            }
        }
    }"#;

    let parsed: UpdateResponse = serde_json::from_str(json_data).unwrap();
    assert_eq!(parsed.latest_version, "1.6.8");
    assert!(parsed.has_update);
    assert_eq!(parsed.update_type.as_deref(), Some("patch"));
    assert_eq!(parsed.platform.as_deref(), Some("pumpkin"));
    let download = parsed.download.unwrap();
    assert_eq!(
        download.direct_url.as_deref(),
        Some("https://github.com/DemonZ-Development/RedstoneReboot/releases/download/v1.6.8/redstonereboot_pumpkin.wasm")
    );
    let changelog = parsed.changelog.unwrap();
    assert_eq!(changelog.latest.unwrap().version.as_deref(), Some("1.6.8"));
}
