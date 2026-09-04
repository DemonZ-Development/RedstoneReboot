<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**Scheduled and health-based restarts for plugins and server-side mods**

</div>

---

## Features

<div align="center">

![RedstoneReboot Features](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/features.png)

</div>

RedstoneReboot handles the part of server maintenance that is easy to forget until something goes wrong: planned restarts, warning countdowns, world saves, and a clean handoff to whatever starts the server process again. It can also watch TPS and memory and schedule an emergency restart after repeated unhealthy readings.

- Multiple daily restart times, timezones, and day-of-week filters
- TPS and memory checks with configurable thresholds and consecutive-check protection
- Emergency restart scheduling when the server stays unhealthy
- Countdown messages through chat, titles, the action bar, and sounds
- World-save delay before shutdown
- Restart handoff for host-managed servers, Pterodactyl, systemd, Docker, or a local script
- Configuration reloads through `/reboot reload`
- Eight PlaceholderAPI values on Bukkit-family and Folia servers

---

## Backend System & Startup Loops

RedstoneReboot decides when a restart should happen, then passes the final action to the backend you choose:

- **DEPEND_ON_HOST** — Stops Minecraft cleanly and relies on your panel, container policy, service, or loop script to start it again
- **LOCALSCRIPT** — Uses a generated wrapper script for local process restarts
- **SYSTEMD** — Hands the restart to a Linux systemd service
- **DOCKER** — Works with a container restart policy
- **PTERODACTYL** — Sends a power action through the panel API

The default, **DEPEND_ON_HOST**, needs no API credentials. Use it when your host already restarts the process after a clean shutdown. Choose Pterodactyl or another explicit backend only when you want RedstoneReboot to make that handoff itself. `/reboot doctor` checks the selected backend before you rely on it.

---

## File Selection

Choose the file that matches your server platform:

### Platform Compatibility
- **Bukkit / Spigot / Paper / Purpur**: 1.9 through 26.2+
- **Folia**: 1.20.1 through 26.2+
- **Fabric**: 1.20.1
- **Forge**: 1.20.4 with Forge 49.x
- **NeoForge**: 1.21.1 with NeoForge 21.1.x

---

| Platform | Distribution Type | File |
|-----------|------------------|------|
| **Bukkit / Spigot / Paper / Purpur** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |
| **Fabric** | Mod | `RedstoneReboot-Fabric-<version>.jar` |
| **Forge** | Mod | `RedstoneReboot-Forge-<version>.jar` |
| **NeoForge** | Mod | `RedstoneReboot-NeoForge-<version>.jar` |

---

## Supported Versions

| Platform | Minecraft Versions | Notes |
|----------|--------------------|-------|
| Bukkit-family servers | `1.9` through `26.2+` | Java 8+ *(legacy)*, Java 17+ *(modern)*, Java 25 *(26.x+)* |
| Folia | `1.20+` through `26.2+` | Dedicated region-threaded build |
| Fabric | `1.20.1` | Requires Fabric API |
| Forge | `1.20.4` | Forge 49.x server-side build |
| NeoForge | `1.21.1` | NeoForge 21.1.x server-side build |

---

## Installation

### Plugin Install (Bukkit/Folia)
1. Download the correct plugin file.
2. Place it in `plugins/`.
3. Start the server — config files are generated automatically.
4. Configure `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
5. Run `/reboot reload` to apply.

### Mod Install (Fabric/Forge/NeoForge)
1. Download the correct mod file.
2. Place it in `mods/` (Fabric requires Fabric API).
3. Start the server.
4. Configure `config/redstonereboot/redstonereboot.properties` and `config/redstonereboot/restart-backends.properties`.
5. Run `/reboot reload` to apply.

---

## Commands

| Command | Description |
|---------|-------------|
| `/reboot` | View status and help |
| `/reboot now [delay]` | Trigger a restart countdown |
| `/reboot schedule <seconds>` | Schedule a future restart |
| `/reboot cancel` | Cancel a pending restart |
| `/reboot status` | Show restart schedule status |
| `/reboot info` | Show health information |
| `/reboot doctor` | Run backend & environment diagnostics |
| `/reboot reload` | Hot-reload all configuration |

---

## PlaceholderAPI (Bukkit Builds)

| Placeholder | Example Output |
|-------------|----------------|
| `%redstonereboot_next_restart%` | `2026-04-15 06:00:00 Europe/London` |
| `%redstonereboot_time_until%` | `2h 30m` |
| `%redstonereboot_status%` | `Normal operation` |
| `%redstonereboot_reason%` | `Scheduled Restart` |
| `%redstonereboot_tps%` | `19.8` |
| `%redstonereboot_memory%` | `62.4%` |
| `%redstonereboot_version%` | `1.6.2` |
| `%redstonereboot_timezone%` | `Europe/London` |

> MOTD compatible as of v1.3.3+.

---

## Quick Links

- [**Complete Wiki**](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [**GitHub Repository**](https://github.com/DemonZ-Development/RedstoneReboot)
- [**Developer API Docs**](https://github.com/DemonZ-Development/RedstoneReboot/blob/main/wiki/Developer-API.md)
- [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- [**Bug Reports & Issues**](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [**Discord Support**](https://discord.gg/GYsTt96ypf)

---

## Live Telemetry

<div align="center">

[![bStats Chart](https://bstats.org/signatures/bukkit/RedstoneReboot.svg)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

</div>

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.

---

Made by [**DemonZ Development**](https://demonzdevelopment.online)
