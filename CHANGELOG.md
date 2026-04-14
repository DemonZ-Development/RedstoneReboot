# Changelog

All notable changes to RedstoneReboot are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/).

---

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
