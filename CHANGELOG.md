# Changelog

All notable changes to RedstoneReboot are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

---

## [1.4.0] — 2026-05-26

### Added
- Periodic update checker (re-checks Modrinth every 6 hours instead of once on startup)
- Re-entrant execution guard in executeRestart() preventing duplicate backend calls from GC upsets
- Config validation for default-permission-level (must be 0-4)
- Migration guide in default config.yml header with wiki references
- Supported timezones documentation in wiki
- Cache for Folia environment detection to avoid repeated class lookups

### Changed
- Default timezone from `Asia/Kolkata` to `Europe/London` (more neutral default)
- ReloadPluginState(): wrapped in try-catch with automatic state restoration on failure
- cleanup() now sends cancelled alert to players (was silent during reload)
- PterodactylBackend timeout/connection errors now return FAILED instead of UNKNOWN (avoids lockout on transient errors)
- Update checker log levels: failures now log at WARNING instead of FINE
- Forge build.gradle migrated from deprecated buildscript block to modern plugins DSL
- Init order in onEnable(): metrics + monitoring now start after core.onEnable()

### Removed
- Unused config keys: permissions.luckperms.default-permission, permissions.luckperms.admin-permission, advanced.async-operations, advanced.thread-pool-size

### Changed
- `ServerLoadMonitor` changed from `runRepeatingAsync` to `runRepeating` to avoid calling Bukkit/Adventure APIs off the main thread
- `RestartManager.executeRestart()` now runs blocking backend calls (`prepare`/`execute`) async, dispatching result handling back to the main thread — prevents blocking the tick thread on Pterodactyl HTTP calls
- `UpdateChecker` periodic task handle is now stored and cancelled on `onDisable()` — no longer leaks repeating tasks across `/reboot reload`
- `ConfigManager.reloadConfig()` validates the new config before swapping the reference — on validation failure, the old config remains active
- `BackendConfig` default `localscript-file` changed from `"start.sh"` to `""` so `LocalScriptBackend` uses its OS-based default (`.bat` on Windows, `.sh` on Linux)
- Mod-platform `.properties` config values now clamped: `tpsThreshold`/`emergencyTpsThreshold` 0–20, `memoryThreshold`/`emergencyMemoryThreshold` 0–100, `checkInterval`/`consecutiveChecks` min 1, `emergencyDelay`/`shutdownDelayTicks` min 0, `defaultPermissionLevel` 0–4
- `triggerEmergencyRestart(String)` now delegates to new overload `triggerEmergencyRestart(String, RestartReason)` for correct restart reason propagation

### Fixed
- bStats json-simple dependency missing from Bukkit JAR shading filter (caused NoClassDefFoundError at runtime)
- lockoutEndTime missing volatile modifier (visibility issue across threads)
- HttpURLConnection resource leak in UpdateChecker.java — connection now always disconnected in finally block
- LocalScriptBackend catching `Throwable` (incl. OutOfMemoryError) instead of `Exception`
- AlertManager `translateAlternateColorCodes` returning null causing NPE in `LEGACY_SERIALIZER.deserialize()`
- MinecraftTPSUtil static fields race condition — `tickTimesField`/`reflectionFailed` now synchronized
- PterodactylBackend API failures logged at invisible `FINE` level — changed to `WARNING`
- PterodactylBackend HttpClient connection pool leaked on `/reboot reload` — added `cleanup()` lifecycle
- ServerEventListener TOCTOU race on `getNextScheduledRestart()` — result now cached locally
- RedstoneRebootPlugin recovery catch blocks silently swallowed exceptions — now logged at WARNING
- RedstoneRebootPlugin `taskScheduler` field not volatile — could be observed as null from async thread
- RedstoneRebootPlugin `getPlatformName()` misleading `(Scheduler Adapter)` suffix — removed
- CommandProcessor `processNow`/`processSchedule` misleading error messages — lockout/controller-pending now reported
- Added `isControllerRestartPending()` to RestartManager for transparent lockout diagnostics
- `String.format` calls across 10 files now use `Locale.ROOT` to avoid comma decimal separators in EU locales
- Config.yml examples cleaned up to reflect current state
- Scheduled restart re-triggers after cancellation — `checkScheduledRestarts()` now recalculates from `triggeredTime.plusSeconds(1)` instead of `currentTime()`, so the same occurrence is skipped on cancel
- Periodic update check task leaks across `/reboot reload` — `UpdateChecker` now stores the `ScheduledTaskHandle` and `core.onDisable()` calls `stopPeriodicChecks()`

## [1.3.3] — 2026-04-15

### Fixed
- All wiki links throughout the project pointed to the GitHub Wiki tab (which was empty). Fixed to point to the actual `wiki/` folder in the repository (`/blob/main/wiki/Home.md`)
- Discord badge in README used a placeholder server ID (`1234567890`) — removed the broken badge
- `System.out.println` in `ServerPlatform.sendPostponedAlert()` replaced with proper `Logger.warning()`
- Added `.gitattributes` to enforce consistent LF line endings and prevent Git warnings on Windows

### Added
- bStats metrics integration (Bukkit, plugin ID `30751`) with custom charts for backend, scheduling, monitoring, platform
- PlaceholderAPI null-safety for MOTD compatibility — all 8 placeholders work in server-list pings
- `/reboot doctor` added to in-game help menu
- bStats status line in startup integration banner
- Dedicated wiki pages: [Permissions](wiki/Permissions.md), [Placeholders](wiki/Placeholders.md), [FAQ](wiki/FAQ.md)
- GitHub community files: PR template, Security policy, Code of Conduct, Funding links, Changelog

### Changed
- Full README rewrite with badges, quick start guide, PlaceholderAPI table, and backend system docs
- All marketplace docs rewritten (SpigotMC, Modrinth, Hangar, Bukkit) with PlaceholderAPI tables, bStats, backend docs
- Release copy refreshed for Discord and Instagram
- Comprehensive Javadoc coverage across all core interfaces and public classes
- `.gitignore` hardened with `*.env`, `out/`, `*.class`, `release-artifacts/` exclusions
- CI workflow updated with JUnit test summary reporting
- Wiki updated with correct mod config paths and integration links
- `BackendConfig` Javadoc corrected from `.yml` to `.properties`
