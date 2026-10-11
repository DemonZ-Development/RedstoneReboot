use crate::backend::BackendManager;
use crate::config::PluginConfig;
use crate::history::RestartHistory;
use crate::schedule::RestartScheduleCalculator;
use chrono::{DateTime, Utc};
use chrono_tz::Tz;
use std::sync::{Arc, Mutex, RwLock};

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RestartReason {
    Scheduled,
    Manual,
    ScheduledApi,
    Emergency,
    Custom(String),
}

impl RestartReason {
    pub fn display_name(&self) -> &str {
        match self {
            Self::Scheduled => "Scheduled",
            Self::Manual => "Manual",
            Self::ScheduledApi => "API Scheduled",
            Self::Emergency => "Emergency",
            Self::Custom(name) => name.as_str(),
        }
    }
}

#[derive(Debug, Clone, PartialEq)]
pub enum TickAction {
    None,
    WarningAlert {
        seconds_remaining: u32,
        reason: RestartReason,
        initiator: String,
    },
    ExecuteShutdown {
        reason: RestartReason,
        initiator: String,
    },
}

#[derive(Debug, Clone)]
pub struct ManagerStatus {
    pub is_restart_in_progress: bool,
    pub seconds_remaining: i64,
    pub reason: Option<RestartReason>,
    pub initiator: Option<String>,
    pub next_scheduled: Option<DateTime<Tz>>,
    pub timezone: String,
}

struct RestartState {
    is_in_progress: bool,
    seconds_remaining: i64,
    current_reason: RestartReason,
    initiator: String,
    next_scheduled_restart: Option<DateTime<Tz>>,
}

pub struct RestartManager {
    config: Arc<RwLock<PluginConfig>>,
    history: Arc<Mutex<RestartHistory>>,
    state: Arc<Mutex<RestartState>>,
    backend: Arc<RwLock<BackendManager>>,
}

impl RestartManager {
    pub fn new(config: PluginConfig, history: RestartHistory, backend: BackendManager) -> Self {
        let initial_tz =
            RestartScheduleCalculator::resolve_timezone(&config.scheduled_restarts.timezone);
        let now = Utc::now().with_timezone(&initial_tz);

        let next_scheduled = if config.scheduled_restarts.enabled {
            RestartScheduleCalculator::calculate_next_restart(
                now,
                &config.scheduled_restarts.times,
                &config.scheduled_restarts.days,
            )
        } else {
            None
        };

        let state = RestartState {
            is_in_progress: false,
            seconds_remaining: -1,
            current_reason: RestartReason::Scheduled,
            initiator: "System".to_string(),
            next_scheduled_restart: next_scheduled,
        };

        Self {
            config: Arc::new(RwLock::new(config)),
            history: Arc::new(Mutex::new(history)),
            state: Arc::new(Mutex::new(state)),
            backend: Arc::new(RwLock::new(backend)),
        }
    }

    pub fn schedule_restart(
        &self,
        delay_seconds: u32,
        reason: RestartReason,
        initiator: &str,
    ) -> bool {
        let mut state = self.state.lock().unwrap();

        if state.is_in_progress
            && state.seconds_remaining >= 0
            && (state.seconds_remaining as u32) <= delay_seconds
        {
            return false;
        }

        let delay_clamped = delay_seconds.min(63072000);
        state.is_in_progress = true;
        state.seconds_remaining = delay_clamped as i64;
        state.current_reason = reason.clone();
        state.initiator = initiator.to_string();

        let _ = self
            .history
            .lock()
            .unwrap()
            .record("SCHEDULED", reason.display_name(), initiator);

        true
    }

    pub fn perform_immediate_restart(&self, reason: RestartReason, initiator: &str) {
        let mut state = self.state.lock().unwrap();
        state.is_in_progress = true;
        state.seconds_remaining = 0;
        state.current_reason = reason.clone();
        state.initiator = initiator.to_string();

        let _ = self
            .history
            .lock()
            .unwrap()
            .record("IMMEDIATE", reason.display_name(), initiator);
    }

    pub fn cancel_restart(&self, cancelled_by: &str) -> bool {
        let mut state = self.state.lock().unwrap();
        if !state.is_in_progress {
            return false;
        }

        state.is_in_progress = false;
        state.seconds_remaining = -1;

        let _ = self
            .history
            .lock()
            .unwrap()
            .record("CANCEL", "Cancelled", cancelled_by);

        true
    }

    pub fn tick(&self) -> TickAction {
        let cfg = self.config.read().unwrap().clone();
        let mut state = self.state.lock().unwrap();

        if state.is_in_progress {
            let remaining = state.seconds_remaining;

            if remaining <= 0 {
                state.is_in_progress = false;
                state.seconds_remaining = -1;
                let reason = state.current_reason.clone();
                let initiator = state.initiator.clone();
                return TickAction::ExecuteShutdown { reason, initiator };
            }

            let u_rem = remaining as u32;
            let should_alert = cfg.alerts.enabled && cfg.alerts.warning_times.contains(&u_rem);
            state.seconds_remaining -= 1;

            if should_alert {
                return TickAction::WarningAlert {
                    seconds_remaining: u_rem,
                    reason: state.current_reason.clone(),
                    initiator: state.initiator.clone(),
                };
            }

            return TickAction::None;
        }

        if !cfg.scheduled_restarts.enabled {
            return TickAction::None;
        }

        let tz = RestartScheduleCalculator::resolve_timezone(&cfg.scheduled_restarts.timezone);
        let now = Utc::now().with_timezone(&tz);

        if state.next_scheduled_restart.is_none() {
            state.next_scheduled_restart = RestartScheduleCalculator::calculate_next_restart(
                now,
                &cfg.scheduled_restarts.times,
                &cfg.scheduled_restarts.days,
            );
        }

        if let Some(target) = state.next_scheduled_restart {
            let diff_seconds = (target - now).num_seconds();
            let warn_threshold = cfg.scheduled_restarts.warning_time as i64;

            if diff_seconds <= warn_threshold {
                let countdown_seconds = diff_seconds.max(0) as u32;
                state.is_in_progress = true;
                state.seconds_remaining = countdown_seconds as i64;
                state.current_reason = RestartReason::Scheduled;
                state.initiator = "Scheduled System".to_string();

                let _ = self.history.lock().unwrap().record(
                    "SCHEDULED",
                    RestartReason::Scheduled.display_name(),
                    "Scheduled System",
                );

                let next_search = target + chrono::Duration::seconds(1);
                state.next_scheduled_restart = RestartScheduleCalculator::calculate_next_restart(
                    next_search,
                    &cfg.scheduled_restarts.times,
                    &cfg.scheduled_restarts.days,
                );

                if countdown_seconds == 0 {
                    state.is_in_progress = false;
                    state.seconds_remaining = -1;
                    return TickAction::ExecuteShutdown {
                        reason: RestartReason::Scheduled,
                        initiator: "Scheduled System".to_string(),
                    };
                }

                state.seconds_remaining -= 1;

                if cfg.alerts.enabled && cfg.alerts.warning_times.contains(&countdown_seconds) {
                    return TickAction::WarningAlert {
                        seconds_remaining: countdown_seconds,
                        reason: RestartReason::Scheduled,
                        initiator: "Scheduled System".to_string(),
                    };
                }
            }
        }

        TickAction::None
    }

    pub fn recalculate_next_restart(&self) {
        let cfg = self.config.read().unwrap().clone();
        let mut state = self.state.lock().unwrap();

        if !cfg.scheduled_restarts.enabled {
            state.next_scheduled_restart = None;
            return;
        }

        let tz = RestartScheduleCalculator::resolve_timezone(&cfg.scheduled_restarts.timezone);
        let now = Utc::now().with_timezone(&tz);

        state.next_scheduled_restart = RestartScheduleCalculator::calculate_next_restart(
            now,
            &cfg.scheduled_restarts.times,
            &cfg.scheduled_restarts.days,
        );
    }

    pub fn reload(&self, new_config: PluginConfig) {
        let data_folder = self.backend.read().unwrap().data_folder().to_path_buf();
        let new_backend = BackendManager::new(&new_config.backend, data_folder);
        *self.backend.write().unwrap() = new_backend;
        {
            let mut cfg_lock = self.config.write().unwrap();
            *cfg_lock = new_config;
        }
        self.recalculate_next_restart();
    }

    pub fn status(&self) -> ManagerStatus {
        let cfg = self.config.read().unwrap();
        let state = self.state.lock().unwrap();

        ManagerStatus {
            is_restart_in_progress: state.is_in_progress,
            seconds_remaining: state.seconds_remaining,
            reason: if state.is_in_progress {
                Some(state.current_reason.clone())
            } else {
                None
            },
            initiator: if state.is_in_progress {
                Some(state.initiator.clone())
            } else {
                None
            },
            next_scheduled: state.next_scheduled_restart,
            timezone: cfg.scheduled_restarts.timezone.clone(),
        }
    }

    pub fn backend(&self) -> Arc<RwLock<BackendManager>> {
        Arc::clone(&self.backend)
    }

    pub fn backend_summary(&self) -> String {
        self.backend.read().unwrap().summary()
    }

    pub fn config(&self) -> Arc<RwLock<PluginConfig>> {
        Arc::clone(&self.config)
    }

    pub fn history(&self) -> Arc<Mutex<RestartHistory>> {
        Arc::clone(&self.history)
    }
}
