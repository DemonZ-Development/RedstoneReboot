# Changelog

All notable changes to RedstoneReboot are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

---

## [1.3.2] — 2026-04-14

### Added
- bStats metrics integration (Bukkit builds, plugin ID `30751`) with custom charts for active backend, scheduling, monitoring, and platform type
- PlaceholderAPI null-safety for MOTD compatibility — all 8 placeholders now work reliably in server-list pings
- `/reboot doctor` added to in-game help menu
- bStats status line in startup integration banner
- Dedicated wiki pages for Permissions, Placeholders, and FAQ
- GitHub community files: PR template, Security policy, Code of Conduct, Funding links (Discord + Instagram), Changelog

### Changed
- Full README rewrite with badges, quick start guide, PlaceholderAPI table, and backend system docs
- All marketplace docs rewritten (SpigotMC, Modrinth, Hangar, Bukkit) with PlaceholderAPI tables, bStats badges, and backend handoff documentation
- Release copy refreshed for Discord and Instagram
- Comprehensive Javadoc coverage across all core interfaces and public classes
- `System.out.println` in `ServerPlatform.sendPostponedAlert()` replaced with proper `Logger.warning()`
- `.gitignore` hardened with `*.env`, `out/`, `*.class`, `release-artifacts/` exclusions
- CI workflow updated with JUnit test summary reporting via `mikepenz/action-junit-report@v4`
- Wiki updated with correct mod config paths (`config/redstonereboot.properties`), bStats documentation, and integration links

### Fixed
- `/reboot doctor` missing from in-game `sendHelp()` output
- Misleading Javadoc in `BackendConfig` referencing `.yml` instead of `.properties`
- PlaceholderAPI expansion failing silently during server-list MOTD pings due to null state
- Raw `System.out.println` in core `ServerPlatform` interface default method
