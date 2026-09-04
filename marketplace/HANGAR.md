<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**Planned and health-based restarts for Bukkit-family and Folia servers**

</div>

---

## What it does

RedstoneReboot schedules routine restarts, warns players before shutdown, saves worlds, and hands the final restart to your hosting environment. It can also watch TPS and memory, then schedule an emergency restart only after the server stays below your configured limits for several checks.

The standard build supports Bukkit-family servers. Folia has a separate build that uses its region-threaded scheduler.

---

## Hangar Scope

This Hangar page serves the plugin builds for Paper and its forks:

| Platform | Distribution Type | File |
|-----------|------------------|------|
| **Paper / Purpur / Pufferfish / Spigot** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |

> [!NOTE]
> Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub releases.

---

## Features

- **Scheduling**: Multiple daily restart times with timezone support and day-of-week filtering
- **Health Checks & Emergency Restarts**: Real-time TPS and memory tracking with consecutive checks to prevent false triggers
- **Graceful Shutdown**: World save delay before server stop

### Restart backends

- **DEPEND_ON_HOST** stops Minecraft cleanly and lets your panel, service, container policy, or loop script start it again
- **LOCALSCRIPT** uses a generated wrapper script for a local process
- **SYSTEMD**, **DOCKER**, and **PTERODACTYL** hand the restart to those environments

DEPEND_ON_HOST is the default and does not need panel credentials. Run `/reboot doctor` after choosing another backend to check its setup. Backend settings can be reloaded with `/reboot reload`.

### Rich Alerts & Integrations
- Chat messages, titles, action bar, and configurable sounds
- **PlaceholderAPI**: 8 placeholders for scoreboards, tab lists, and MOTD plugins
- **LuckPerms**: full permission resolution with group support
- **bStats**: anonymous usage metrics ([view](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

### Platform compatibility

- **Bukkit / Spigot / Paper / Purpur**: 1.9 through 26.2+
- **Folia**: 1.20.1 through 26.2+

---

## Installation

1. Download the correct file for your platform.
2. Place it in `plugins/`.
3. Start the server — config files are generated automatically.
4. Edit `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
5. Run `/reboot reload` to apply changes.

---

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/reboot` | `redstonereboot.use` | Show plugin status and help |
| `/reboot now [delay]` | `redstonereboot.restart.now` | Start a countdown-based restart |
| `/reboot schedule <seconds>` | `redstonereboot.restart.schedule` | Schedule a future restart |
| `/reboot cancel` | `redstonereboot.restart.cancel` | Cancel a pending restart |
| `/reboot status` | `redstonereboot.status` | View timing and restart details |
| `/reboot info` | `redstonereboot.status` | View monitored server health |
| `/reboot doctor` | `redstonereboot.doctor` | Run backend & environment diagnostics |
| `/reboot reload` | `redstonereboot.config.reload` | Hot-reload configuration |

---

## PlaceholderAPI

| Placeholder | Output |
|-------------|--------|
| `%redstonereboot_next_restart%` | Next restart date/time |
| `%redstonereboot_time_until%` | Time remaining |
| `%redstonereboot_status%` | Current status |
| `%redstonereboot_reason%` | Restart reason |
| `%redstonereboot_tps%` | Last TPS |
| `%redstonereboot_memory%` | Memory usage % |
| `%redstonereboot_version%` | Plugin version |
| `%redstonereboot_timezone%` | Configured timezone |

---

## Quick Links

- [**Complete Wiki**](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [**GitHub Repository**](https://github.com/DemonZ-Development/RedstoneReboot)
- [**Developer API Docs**](https://github.com/DemonZ-Development/RedstoneReboot/blob/main/wiki/Developer-API.md)
- [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- [**Bug Tracker & Issues**](https://github.com/DemonZ-Development/RedstoneReboot/issues)

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.

---

Made by [**DemonZ Development**](https://demonzdevelopment.online)
