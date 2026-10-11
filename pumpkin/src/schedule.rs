use chrono::{DateTime, Datelike, Duration, NaiveTime, TimeZone, Weekday};
use chrono_tz::Tz;
use std::collections::HashSet;
use std::str::FromStr;

pub struct RestartScheduleCalculator;

impl RestartScheduleCalculator {
    pub fn parse_time(raw: &str) -> Option<NaiveTime> {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            return None;
        }

        if let Ok(t) = NaiveTime::parse_from_str(trimmed, "%H:%M:%S") {
            return Some(t);
        }

        if let Ok(t) = NaiveTime::parse_from_str(trimmed, "%H:%M") {
            return Some(t);
        }

        if let Ok(t) = NaiveTime::parse_from_str(trimmed, "%k:%M") {
            return Some(t);
        }

        let parts: Vec<&str> = trimmed.split(':').collect();
        if parts.len() == 2 {
            let hour: u32 = parts[0].trim().parse().ok()?;
            let minute: u32 = parts[1].trim().parse().ok()?;
            return NaiveTime::from_hms_opt(hour, minute, 0);
        }
        if parts.len() == 3 {
            let hour: u32 = parts[0].trim().parse().ok()?;
            let minute: u32 = parts[1].trim().parse().ok()?;
            let second: u32 = parts[2].trim().parse().ok()?;
            return NaiveTime::from_hms_opt(hour, minute, second);
        }

        None
    }

    pub fn parse_days(configured_days: &[String]) -> HashSet<Weekday> {
        if configured_days.is_empty() {
            return Self::all_days();
        }

        let mut days = HashSet::new();
        for day_str in configured_days {
            let upper = day_str.trim().to_uppercase();
            if upper == "ALL" {
                return Self::all_days();
            }

            match upper.as_str() {
                "MONDAY" | "MON" => {
                    days.insert(Weekday::Mon);
                }
                "TUESDAY" | "TUE" | "TUES" => {
                    days.insert(Weekday::Tue);
                }
                "WEDNESDAY" | "WED" => {
                    days.insert(Weekday::Wed);
                }
                "THURSDAY" | "THU" | "THUR" | "THURS" => {
                    days.insert(Weekday::Thu);
                }
                "FRIDAY" | "FRI" => {
                    days.insert(Weekday::Fri);
                }
                "SATURDAY" | "SAT" => {
                    days.insert(Weekday::Sat);
                }
                "SUNDAY" | "SUN" => {
                    days.insert(Weekday::Sun);
                }
                _ => {}
            }
        }

        if days.is_empty() {
            Self::all_days()
        } else {
            days
        }
    }

    fn all_days() -> HashSet<Weekday> {
        let mut set = HashSet::new();
        set.insert(Weekday::Mon);
        set.insert(Weekday::Tue);
        set.insert(Weekday::Wed);
        set.insert(Weekday::Thu);
        set.insert(Weekday::Fri);
        set.insert(Weekday::Sat);
        set.insert(Weekday::Sun);
        set
    }

    pub fn resolve_timezone(name: &str) -> Tz {
        Tz::from_str(name.trim()).unwrap_or(chrono_tz::UTC)
    }

    pub fn calculate_next_restart(
        now: DateTime<Tz>,
        configured_times: &[String],
        configured_days: &[String],
    ) -> Option<DateTime<Tz>> {
        if configured_times.is_empty() {
            return None;
        }

        let allowed_days = Self::parse_days(configured_days);
        let mut times: Vec<NaiveTime> = configured_times
            .iter()
            .filter_map(|s| Self::parse_time(s))
            .collect();

        if times.is_empty() {
            return None;
        }

        times.sort();
        times.dedup();

        let tz = now.timezone();
        let base_date = now.date_naive();

        for day_offset in 0..=14 {
            let candidate_date = base_date + Duration::days(day_offset);
            if !allowed_days.contains(&candidate_date.weekday()) {
                continue;
            }

            for time in &times {
                let candidate_naive = candidate_date.and_time(*time);
                let candidate_opt = tz
                    .from_local_datetime(&candidate_naive)
                    .single()
                    .or_else(|| tz.from_local_datetime(&candidate_naive).earliest());

                if let Some(candidate) = candidate_opt.filter(|c| *c > now) {
                    return Some(candidate);
                }
            }
        }

        None
    }
}
