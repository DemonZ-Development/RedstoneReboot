use serde::Deserialize;
use std::sync::RwLock;

pub const UPDATES_API_URL: &str = "https://demonz.org/api/projects/redstonereboot/updates?platform=pumpkin";

#[derive(Debug, Clone, Deserialize, PartialEq, Default)]
pub struct UpdateDownload {
    pub url: Option<String>,
    pub direct_url: Option<String>,
    pub file_name: Option<String>,
}

#[derive(Debug, Clone, Deserialize, PartialEq, Default)]
pub struct UpdateChangelogEntry {
    pub version: Option<String>,
    pub notes: Option<String>,
}

#[derive(Debug, Clone, Deserialize, PartialEq, Default)]
pub struct UpdateChangelog {
    pub latest: Option<UpdateChangelogEntry>,
}

#[derive(Debug, Clone, Deserialize, PartialEq, Default)]
pub struct UpdateResponse {
    pub current_version: Option<String>,
    pub latest_version: String,
    pub has_update: bool,
    pub update_type: Option<String>,
    pub platform: Option<String>,
    pub download: Option<UpdateDownload>,
    pub changelog: Option<UpdateChangelog>,
}

static CACHED_UPDATE: RwLock<Option<UpdateResponse>> = RwLock::new(None);

pub fn get_cached_update() -> Option<UpdateResponse> {
    CACHED_UPDATE.read().ok()?.clone()
}

pub fn set_cached_update(update: UpdateResponse) {
    if let Ok(mut lock) = CACHED_UPDATE.write() {
        *lock = Some(update);
    }
}

pub fn is_newer_version(current: &str, candidate: &str) -> bool {
    let curr_nums = parse_version_numbers(current);
    let cand_nums = parse_version_numbers(candidate);

    let max_len = curr_nums.len().max(cand_nums.len());
    for i in 0..max_len {
        let curr = curr_nums.get(i).copied().unwrap_or(0);
        let cand = cand_nums.get(i).copied().unwrap_or(0);
        if cand != curr {
            return cand > curr;
        }
    }
    false
}

fn parse_version_numbers(v: &str) -> Vec<u32> {
    let clean = v.trim().trim_start_matches(|c| c == 'v' || c == 'V');
    let base = clean.split('-').next().unwrap_or(clean);
    base.split('.')
        .filter_map(|part| part.parse::<u32>().ok())
        .collect()
}

#[cfg(target_os = "wasi")]
pub fn fetch_updates(current_version: &str) -> Result<UpdateResponse, String> {
    let target_url = format!("{}&version={}", UPDATES_API_URL, current_version);
    let client = wasi_http_client::Client::new();
    let resp = client
        .get(&target_url)
        .header(
            "User-Agent",
            format!("DemonZDevelopment/RedstoneReboot-Pumpkin/{}", current_version),
        )
        .connect_timeout(std::time::Duration::from_secs(5))
        .send()
        .map_err(|e| format!("HTTP request failed: {}", e))?;

    let status = resp.status();
    if status != 200 {
        return Err(format!("Server returned HTTP {}", status));
    }

    let parsed = resp
        .json::<UpdateResponse>()
        .map_err(|e| format!("Failed to parse JSON response: {}", e))?;

    set_cached_update(parsed.clone());
    Ok(parsed)
}

#[cfg(not(target_os = "wasi"))]
pub fn fetch_updates(_current_version: &str) -> Result<UpdateResponse, String> {
    Err("Update checking over WASI HTTP is only available on wasi targets".to_string())
}

pub fn check_and_log(current_version: &str) {
    match fetch_updates(current_version) {
        Ok(update) => {
            if update.has_update || is_newer_version(current_version, &update.latest_version) {
                let direct_url = update
                    .download
                    .as_ref()
                    .and_then(|d| d.direct_url.as_deref())
                    .or_else(|| {
                        update
                            .download
                            .as_ref()
                            .and_then(|d| d.url.as_deref())
                    })
                    .unwrap_or("https://demonz.org/projects/redstonereboot");

                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    "==========================================",
                );
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    "A new version of RedstoneReboot for Pumpkin is available!",
                );
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    &format!("Current version: {}", current_version),
                );
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    &format!("Latest version:  {}", update.latest_version),
                );
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    &format!("Download: {}", direct_url),
                );
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    "==========================================",
                );
            } else {
                pumpkin_plugin_api::logging::log(
                    pumpkin_plugin_api::logging::LogLevel::Info,
                    &format!("RedstoneReboot is up to date (v{}).", current_version),
                );
            }
        }
        Err(err) => {
            pumpkin_plugin_api::logging::log(
                pumpkin_plugin_api::logging::LogLevel::Warn,
                &format!("RedstoneReboot update check skipped: {}", err),
            );
        }
    }
}
