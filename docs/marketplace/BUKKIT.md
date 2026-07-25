# RedstoneReboot — Bukkit/Plugin Directory Copy

<!-- Bukkit-oriented marketplace copy -->

<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**A restart engine for Bukkit-family Minecraft servers**

[![bStats](https://img.shields.io/bstats/players/30751?label=bStats%20Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

</div>

---

## Why RedstoneReboot?

RedstoneReboot is a restart management plugin for Bukkit-family servers. It gives administrators control over restart scheduling, health-based automation, and a backend handoff system that works for anything from a single survival server to a multi-node network.

---

## Scope

This page serves the plugin builds for Bukkit, Spigot, Paper, and Folia:

| Platform | Distribution Type | File |
|-----------|------------------|------|
| **Bukkit / Spigot / Paper / Purpur / Pufferfish** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |

> [!NOTE]
> Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub releases.

---

## Features

- **Scheduling**: Multiple daily restart times with timezone support and day-of-week filtering
- **Health Checks & Emergency Restarts**: Real-time TPS and memory tracking with consecutive checks to prevent false triggers
- **Graceful Shutdown**: World save delay before server stop

### Backend Handoff System
- **DEPEND_ON_HOST** — graceful shutdown for external restarters
- **LOCALSCRIPT** — auto-generated wrapper script restart loop
- **SYSTEMD** / **DOCKER** / **PTERODACTYL** — native environment integration
- Hot-reload: edit `restart-backends.properties` and `/reboot reload`
- **Do I need a custom backend?** If your server runs inside a loop script, Docker container with `restart: always`, or systemd service, **DEPEND_ON_HOST works out of the box.** The engine stops the server cleanly and your host supervisor starts it again.
- **Why configure a custom backend then?**
  1. *Clean handoff (Pterodactyl / panels)*: Avoid panel desyncs or false offline indicators by requesting a clean power cycle through the panel's API.
  2. *Self-healing bootups*: The LOCALSCRIPT backend spawns a new process to bring the server back up if you don't run a loop script.
  3. *Crash lockout safety*: Custom backends add safety lockout timers to stop endless boot hammering if files get corrupted.

### Rich Alerts & Integrations
- Chat, titles, action bar, and sounds
- **PlaceholderAPI**: 8 placeholders for scoreboards, tab lists, MOTD
- **LuckPerms**: full permission resolution
- **bStats**: anonymous metrics ([view](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

### Platform Compatibility
- **Bukkit / Spigot / Paper / Purpur**: 1.9.x to 26.2+
- **Folia**: 1.20.1+ to 26.2+
- **Fabric**: 1.20.1+ to 26.2+
- **Forge**: 1.20.4+ to 26.2+
- **NeoForge**: 1.21.1+ to 26.2+ (Java 21+)

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
- [**Developer API Docs**](https://github.com/DemonZ-Development/RedstoneReboot/blob/main/docs/api/README.md)
- [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- [**Bug Tracker & Issues**](https://github.com/DemonZ-Development/RedstoneReboot/issues)

---

Made by [**DemonZ Development**](https://demonzdevelopment.online)
