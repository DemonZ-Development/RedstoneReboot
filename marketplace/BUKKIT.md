<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**Scheduled restarts and performance monitoring for Paper, Purpur, and Folia servers.**

</div>

---

## Overview

RedstoneReboot automates server restarts to maintain consistent performance and memory health. It manages scheduled reboot intervals, monitors TPS and RAM for emergency restart triggers, broadcasts countdown notices to players, saves world data before shutdown, and coordinates with your hosting environment.

---

## Downloads

| Platform | Type | File Name | Supported MC |
|---|---|---|---|
| **Paper / Purpur / Spigot** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` | 1.9 to 26.3+ |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` | 1.20 to 26.3+ |

> Mod variants for Fabric, Forge, and NeoForge are distributed separately on Modrinth and GitHub releases.

---

## Features

- **Routine Schedules**: Define multiple reboot times with timezone support and day-of-week filters.
- **Performance Watchdog**: Real-time TPS and RAM monitoring with consecutive check thresholds to prevent false alarms.
- **Emergency Restarts**: Triggers an alert countdown if TPS drops critically or RAM remains exhausted.
- **Player Notices**: Configurable warnings across chat, titles, action bar, and note block sounds.
- **Save Delay**: Waits for world saving before stopping the server.
- **Hot Reload**: Update configuration files live via `/reboot reload`.
- **Integrations**: Supports LuckPerms permission resolution, PlaceholderAPI, and bStats metrics.

---

## How Restarts Work

A plugin runs inside the Minecraft server process. When the server shuts down, Java terminates. An external watcher brings the server back online:

- **DEPEND_ON_HOST** *(Default)*: The server stops cleanly. Your game hosting panel (Pterodactyl, Bloom, PebbleHost, Bisect, etc.) or container supervisor boots it back up. No extra flags or API keys needed.
- **LOCALSCRIPT**: Uses an auto-generated script (`redstonereboot-start.sh` or `.bat`) with a restart loop for local machines or dedicated servers.
- **SYSTEMD**, **DOCKER**, and **PTERODACTYL**: Explicit backend integrations for Linux services, Docker policies, and Pterodactyl panel API power calls.

### What does ASSISTED mean in `/reboot doctor`?

If you configure a supervisor backend (`LOCALSCRIPT`, `SYSTEMD`, or `DOCKER`), RedstoneReboot checks for proof that an outside restart loop is running (`-Dredstonereboot.active=true` in startup arguments or `REDSTONEREBOOT_ACTIVE=1` in the environment).

Without this flag, `/reboot doctor` reports the **ASSISTED** state to protect against shutting down on a host without an active restart loop.

If your game host already reboots stopped servers automatically, you do not need supervisor wiring. Keep `backends-enabled=false` or set `active-backend=DEPEND_ON_HOST` in `restart-backends.properties`.

---

## Installation

1. Place the plugin jar into your server's `plugins/` directory.
2. Start the server to generate configuration files.
3. Edit `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
4. Run `/reboot reload` to apply your changes.

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
- [bStats Metrics](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.

---

Maintained by [**DemonZ Development**](https://demonz.org)
