# RedstoneReboot (Bukkit Plugins)

**Scheduled restarts and performance monitoring for Paper, Purpur, Spigot, and Folia servers.**

---

## Overview

RedstoneReboot automates Minecraft server restarts to keep performance consistent and free up memory leaks. It schedules fixed daily reboots, watches TPS and RAM for emergency restart triggers, broadcasts configurable countdowns, ensures world data saves before shutdown, and coordinates with your hosting panel or supervisor.

This CurseForge project distributes plugin builds for Paper, Purpur, Spigot, and a dedicated region-threaded build for Folia. For server-side mods on Fabric, Forge, or NeoForge, see the [Minecraft Mods listing](https://www.curseforge.com/minecraft/mc-mods/redstonereboot) on CurseForge.

---

## Supported Plugin Platforms & Minecraft Versions

| Platform | Type | File Name | Supported MC | Java Runtime |
|---|---|---|---|---|
| **Paper / Purpur / Spigot** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` | `1.9` to `26.3+` | Java 17+ / Java 25 (26.x+) |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` | `1.20+` to `26.3+` | Java 17+ / Java 25 (26.x+) |

---

## Key Features

- **Automated Scheduling**: Configure multiple restart times per day with timezone support and day-of-week selection.
- **Server Health Watchdog**: Monitors TPS and memory continuously. Consecutive check thresholds avoid unnecessary restarts during temporary lag spikes.
- **Emergency Restart Mode**: Automatically starts a restart countdown when TPS drops critically or memory stays full.
- **Player Notifications**: Configurable alerts in chat, titles, action bar, and note block chimes.
- **Save Delay**: Waits for world saving before stopping the server.
- **Panel & Supervisor Integration**: Works with host panels, Docker policies, systemd units, and local scripts.
- **Hot Reload**: Reload configuration files without restarting via `/reboot reload`.
- **PlaceholderAPI**: 8 placeholders for MOTDs, tab lists, and scoreboards.
- **Permissions**: Full LuckPerms and Bukkit permission support.

---

## How Restarts Work

A plugin runs inside the Minecraft server process. When the server shuts down, Java terminates. An external supervisor brings the server back online:

- **DEPEND_ON_HOST** *(Default)*: RedstoneReboot stops the server cleanly. Your hosting panel (Pterodactyl, Bloom, PebbleHost, Bisect, etc.) or container supervisor automatically reboots the process. No extra flags or API tokens required.
- **LOCALSCRIPT**: Uses an auto-generated script (`redstonereboot-start.sh` or `.bat`) with a loop for dedicated servers and local testing.
- **SYSTEMD**: Coordinates restarts through Linux systemd service units.
- **DOCKER**: Works with Docker restart policies (`restart: always`).
- **PTERODACTYL**: Sends power signals directly through the Pterodactyl Client API.

### What does ASSISTED mean in `/reboot doctor`?

If you configure a supervisor backend (`LOCALSCRIPT`, `SYSTEMD`, or `DOCKER`), RedstoneReboot checks for proof that an outside watcher is running (`-Dredstonereboot.active=true` in startup flags or `REDSTONEREBOOT_ACTIVE=1` in the environment).

Without this flag, `/reboot doctor` reports the **ASSISTED** state to prevent shutting down on a host without an active restart loop.

If your game host already reboots stopped servers automatically, you do not need supervisor wiring. Set `backends-enabled=false` or `active-backend=DEPEND_ON_HOST` in `restart-backends.properties`.

---

## Installation

1. Place the plugin jar into your server's `plugins/` directory.
2. Start the server to generate configuration files.
3. Edit `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
4. Run `/reboot reload` to apply your changes live.

---

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/reboot` | `redstonereboot.use` | Overview and status |
| `/reboot now [delay]` | `redstonereboot.now` | Trigger a restart countdown |
| `/reboot schedule <seconds>` | `redstonereboot.schedule` | Schedule a future restart |
| `/reboot cancel` | `redstonereboot.cancel` | Cancel an active countdown |
| `/reboot status` | `redstonereboot.status` | Check next scheduled restart |
| `/reboot info` | `redstonereboot.info` | Show server health metrics |
| `/reboot doctor` | `redstonereboot.doctor` | Run backend and environment diagnostics |
| `/reboot reload` | `redstonereboot.reload` | Reload configuration files live |

---

## PlaceholderAPI

| Placeholder | Description |
|---|---|
| `%redstonereboot_next_restart%` | Next scheduled restart date and time |
| `%redstonereboot_time_until%` | Time remaining until next reboot |
| `%redstonereboot_status%` | Current server state or countdown status |
| `%redstonereboot_reason%` | Reason or initiator of current restart |
| `%redstonereboot_tps%` | Current server TPS |
| `%redstonereboot_memory%` | Current RAM consumption percentage |
| `%redstonereboot_version%` | Installed plugin version |
| `%redstonereboot_timezone%` | Configured timezone |

---

## Links

- [Documentation Wiki](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [Source Code](https://github.com/DemonZ-Development/RedstoneReboot)
- [Issue Tracker](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [Discord Support](https://discord.gg/GYsTt96ypf)
- [Minecraft Mods Listing](https://www.curseforge.com/minecraft/mc-mods/redstonereboot)
- [bStats Metrics](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- [DemonZ Development](https://demonz.org)

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.
