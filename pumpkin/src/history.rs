use chrono::{DateTime, Utc};
use std::collections::VecDeque;
use std::fs::{self, OpenOptions};
use std::io::{self, BufRead, BufReader, Write};
use std::path::{Path, PathBuf};

const MAX_HISTORY_ENTRIES: usize = 50;

#[derive(Debug, Clone, PartialEq)]
pub struct HistoryEntry {
    pub time: DateTime<Utc>,
    pub event_type: String,
    pub reason: String,
    pub initiator: String,
}

impl HistoryEntry {
    pub fn new(time: DateTime<Utc>, event_type: &str, reason: &str, initiator: &str) -> Self {
        Self {
            time,
            event_type: event_type.to_string(),
            reason: reason.to_string(),
            initiator: initiator.to_string(),
        }
    }

    pub fn to_log_line(&self) -> String {
        format!(
            "{} | {} | reason={} | initiator={}",
            self.time.format("%Y-%m-%d %H:%M:%S"),
            self.event_type,
            self.reason,
            self.initiator
        )
    }

    pub fn to_display_string(&self) -> String {
        format!(
            "{}  {}  ({}, by {})",
            self.time.format("%Y-%m-%d %H:%M:%S"),
            self.event_type,
            self.reason,
            self.initiator
        )
    }

    pub fn parse_log_line(line: &str) -> Option<Self> {
        let parts: Vec<&str> = line.split('|').map(|s| s.trim()).collect();
        if parts.len() < 4 {
            return None;
        }

        let time_str = parts[0];
        let event_type = parts[1];
        let reason_part = parts[2];
        let initiator_part = parts[3];

        let naive_time =
            chrono::NaiveDateTime::parse_from_str(time_str, "%Y-%m-%d %H:%M:%S").ok()?;
        let time = DateTime::<Utc>::from_naive_utc_and_offset(naive_time, Utc);

        let reason = reason_part
            .strip_prefix("reason=")
            .unwrap_or(reason_part)
            .trim();
        let initiator = initiator_part
            .strip_prefix("initiator=")
            .unwrap_or(initiator_part)
            .trim();

        Some(Self::new(time, event_type, reason, initiator))
    }
}

pub struct RestartHistory {
    log_file: PathBuf,
    entries: VecDeque<HistoryEntry>,
}

impl RestartHistory {
    pub fn new(log_file: PathBuf) -> Self {
        Self::new_with_data_folder(log_file, None)
    }

    pub fn new_with_data_folder(log_file: PathBuf, data_folder: Option<&Path>) -> Self {
        Self::migrate_legacy_path(&log_file, data_folder);

        let mut history = Self {
            log_file,
            entries: VecDeque::with_capacity(MAX_HISTORY_ENTRIES),
        };

        history.load_existing_log();
        history
    }

    pub fn set_log_file(&mut self, new_path: PathBuf) {
        self.log_file = new_path;
    }

    fn migrate_legacy_path(target: &Path, data_folder: Option<&Path>) {
        if target.exists() {
            return;
        }

        if let Some(folder) = data_folder {
            let legacy_data = folder.join("restarts.log");
            if legacy_data.exists() && legacy_data != target {
                if let Some(parent) = target.parent() {
                    let _ = fs::create_dir_all(parent);
                }
                if fs::rename(&legacy_data, target).is_ok() {
                    return;
                }
            }
        }

        let legacy_root = Path::new("logs").join("restarts.log");
        if legacy_root.exists() && legacy_root != target {
            if let Some(parent) = target.parent() {
                let _ = fs::create_dir_all(parent);
            }
            let _ = fs::rename(&legacy_root, target);
        }
    }

    fn load_existing_log(&mut self) {
        if !self.log_file.exists() {
            return;
        }

        if let Ok(file) = fs::File::open(&self.log_file) {
            let reader = BufReader::new(file);
            for line in reader.lines().map_while(Result::ok) {
                if let Some(entry) = HistoryEntry::parse_log_line(&line) {
                    self.push_entry(entry);
                }
            }
        }
    }

    fn push_entry(&mut self, entry: HistoryEntry) {
        if self.entries.len() >= MAX_HISTORY_ENTRIES {
            self.entries.pop_front();
        }
        self.entries.push_back(entry);
    }

    pub fn record(
        &mut self,
        event_type: &str,
        reason: &str,
        initiator: &str,
    ) -> io::Result<HistoryEntry> {
        let entry = HistoryEntry::new(Utc::now(), event_type, reason, initiator);
        self.push_entry(entry.clone());

        if let Some(parent) = self.log_file.parent() {
            fs::create_dir_all(parent)?;
        }

        let mut file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&self.log_file)?;

        writeln!(file, "{}", entry.to_log_line())?;
        Ok(entry)
    }

    pub fn recent(&self, limit: usize) -> Vec<HistoryEntry> {
        let count = if limit == 0 || limit > self.entries.len() {
            self.entries.len()
        } else {
            limit
        };

        let skip_count = self.entries.len().saturating_sub(count);
        self.entries.iter().skip(skip_count).cloned().collect()
    }

    pub fn log_file_path(&self) -> &Path {
        &self.log_file
    }
}
