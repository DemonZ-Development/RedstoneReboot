<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**Automated scheduled restarts and health monitoring for Paper, Folia, Fabric, Forge, and NeoForge servers.**

</div>

---

## What is RedstoneReboot?

RedstoneReboot manages routine restarts and monitors server performance. It handles scheduled daily reboots, watches TPS and memory for emergency restarts, broadcasts customizable countdown warnings across chat, titles, action bar, and sounds, delays shutdown for world saving, and coordinates process reboots with your hosting panel or environment.

---

## Features

- **Scheduled Restarts**: Set multiple daily restart times with timezone support and day-of-week filters.
- **Performance Watchdog**: Continuously tracks TPS and RAM usage. Consecutive check thresholds prevent false alarms.
- **Emergency Restarts**: Triggers a reboot countdown if TPS remains critically low or memory stays near exhaustion.
- **Player Alerts**: Configurable countdown warnings in chat, titles, action bar, and note block sounds.
- **World Save Protection**: Configurable delay to ensure chunks save before the process exits.
- **Backend Integrations**: Supports host panels, Pterodactyl API, Docker policies, systemd, and local restart scripts.
- **Hot Reload**: Update settings live using `/reboot reload`.
- **PlaceholderAPI Support**: 8 placeholders for tab lists, scoreboards, and MOTDs on Bukkit and Folia servers.

---

## How Restarts Work (Backends)

A Minecraft plugin cannot boot itself back up from inside a terminated Java process. When the restart countdown finishes, RedstoneReboot shuts down the server cleanly. Bringing the server back online requires an external watcher:

- **DEPEND_ON_HOST** *(Default)*: The server stops cleanly, and your game hosting panel (Pterodactyl, Bloom, PebbleHost, Bisect, etc.) or container supervisor boots it back up. No startup flags or API keys needed.
- **LOCALSCRIPT**: Uses an auto-generated wrapper script (`redstonereboot-start.sh` / `.bat`) with a restart loop for local machines or dedicated servers.
- **SYSTEMD**: Coordinates restarts through Linux systemd services.
- **DOCKER**: Works with Docker container restart policies (`restart: always`).
- **PTERODACTYL**: Sends power restart signals directly through the Pterodactyl Client API.

### What does the ASSISTED state mean in `/reboot doctor`?

When you enable an external supervisor backend (`LOCALSCRIPT`, `SYSTEMD`, or `DOCKER`), RedstoneReboot checks for proof that an outside restart loop is actually watching the process. It checks for `-Dredstonereboot.active=true` in your Java startup arguments or `REDSTONEREBOOT_ACTIVE=1` in your environment.

If that flag is missing, `/reboot doctor` flags the state as **ASSISTED**. This safety check stops the server from shutting down into an unmonitored state where it stays offline forever.

If your game host already boots stopped servers automatically, you do not need supervisor wiring or startup flags. Keep `backends-enabled=false` or set `active-backend=DEPEND_ON_HOST` in `restart-backends.properties`.

---

## Platform Compatibility & Downloads

Download the jar built for your loader:

| Platform | Type | File Name | Java Version | Supported MC |
|---|---|---|---|---|
| **Paper / Purpur / Spigot** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` | Java 17+ / Java 25 (26.x+) | 1.9 to 26.3+ |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` | Java 17+ / Java 25 (26.x+) | 1.20 to 26.3+ |
| **Fabric** | Mod | `RedstoneReboot-Fabric-<version>.jar` | Java 17 (1.20.1) / Java 25 (26.x+) | 1.20.1, 26.1, 26.2, 26.3+ |
| **Forge** | Mod | `RedstoneReboot-Forge-<version>.jar` | Java 17 (1.20.4) / Java 25 (26.x+) | 1.20.4, 26.1, 26.2, 26.3+ |
| **NeoForge** | Mod | `RedstoneReboot-NeoForge-<version>.jar` | Java 21 (1.21.1) / Java 25 (26.x+) | 1.21.1, 26.1, 26.2, 26.3+ |

---

## Installation

### Paper, Purpur, Spigot, Folia
1. Place the plugin jar into your server's `plugins/` directory.
2. Start the server to generate configuration files.
3. Edit `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
4. Run `/reboot reload` to apply your changes.

### Fabric, Forge, NeoForge
1. Place the mod jar into your server's `mods/` directory (Fabric requires Fabric API).
2. Start the server to generate configuration files.
3. Edit `config/redstonereboot/redstonereboot.properties` and `config/redstonereboot/restart-backends.properties`.
4. Run `/reboot reload` to apply your changes.

---

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/reboot` | `redstonereboot.use` | View current status and command overview |
| `/reboot now [seconds]` | `redstonereboot.now` | Trigger a restart countdown immediately |
| `/reboot schedule <seconds>` | `redstonereboot.schedule` | Schedule a restart after a set delay |
| `/reboot cancel` | `redstonereboot.cancel` | Cancel an active countdown |
| `/reboot status` | `redstonereboot.status` | Display the next scheduled restart time |
| `/reboot info` | `redstonereboot.info` | Display live TPS, memory, and uptime |
| `/reboot doctor` | `redstonereboot.doctor` | Run backend and environment diagnostics |
| `/reboot reload` | `redstonereboot.reload` | Reload configuration files live |

---

## PlaceholderAPI (Bukkit & Folia)

| Placeholder | Example Output | Description |
|---|---|---|
| `%redstonereboot_next_restart%` | `2026-10-06 06:00:00 Europe/London` | Next scheduled restart time |
| `%redstonereboot_time_until%` | `2h 30m` | Time remaining until next reboot |
| `%redstonereboot_status%` | `Normal operation` | Active status or countdown info |
| `%redstonereboot_reason%` | `Scheduled Restart` | Initiator or trigger reason |
| `%redstonereboot_tps%` | `19.8` | Current server TPS |
| `%redstonereboot_memory%` | `62.4%` | Current RAM consumption percentage |
| `%redstonereboot_version%` | `1.6.5` | Installed plugin version |
| `%redstonereboot_timezone%` | `Europe/London` | Active configured timezone |

---

## Links & Community

- [Documentation Wiki](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [Source Code](https://github.com/DemonZ-Development/RedstoneReboot)
- [Issue Tracker](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [Discord Server](https://discord.gg/GYsTt96ypf)
- [bStats Metrics](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.

---

Maintained by [**DemonZ Development**](https://demonz.org)
