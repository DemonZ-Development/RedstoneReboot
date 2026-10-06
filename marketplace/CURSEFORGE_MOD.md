# RedstoneReboot (Mods)

**Scheduled restarts and performance monitoring for Fabric, Forge, and NeoForge servers.**

---

## Overview

RedstoneReboot automates Minecraft server restarts to keep performance consistent and free up memory leaks. It schedules fixed daily reboots, watches TPS and RAM for emergency restart triggers, broadcasts configurable countdowns, ensures world data saves before shutdown, and coordinates with your hosting panel or supervisor.

This CurseForge project distributes dedicated server-side mod builds for Fabric, Forge, and NeoForge. Players do not need to install this mod on their clients to connect. For Bukkit, Spigot, Paper, Purpur, or Folia plugins, see the [Bukkit Plugins listing](https://www.curseforge.com/minecraft/bukkit-plugins/redstonereboot) on CurseForge.

---

## Supported Mod Loaders & Minecraft Versions

| Platform | Minecraft Versions | Java Runtime | Mod File |
|---|---|---|---|
| **Fabric** | `1.20.1`, `26.1`, `26.2`, `26.3+` | Java 17 (1.20.1) / Java 25 (26.x+) | `RedstoneReboot-Fabric-*.jar` / `RedstoneReboot-Modern-Fabric-*.jar` |
| **Forge** | `1.20.4`, `26.1`, `26.2`, `26.3+` | Java 17 (1.20.4) / Java 25 (26.x+) | `RedstoneReboot-Forge-*.jar` / `RedstoneReboot-Modern-Forge-*.jar` |
| **NeoForge** | `1.21.1`, `26.1`, `26.2`, `26.3+` | Java 21 (1.21.1) / Java 25 (26.x+) | `RedstoneReboot-NeoForge-*.jar` / `RedstoneReboot-Modern-NeoForge-*.jar` |

---

## Key Features

- **Automated Scheduling**: Configure multiple restart times per day with timezone support and day-of-week selection.
- **Server Health Watchdog**: Monitors TPS and memory continuously. Consecutive check thresholds avoid unnecessary restarts during temporary lag spikes.
- **Emergency Restart Mode**: Automatically starts a restart countdown when TPS drops critically or memory stays full.
- **Player Notifications**: Configurable alerts in chat, titles, action bar, and note block chimes.
- **Save Delay**: Waits for world saving before stopping the server.
- **Panel & Supervisor Integration**: Works with host panels, Docker policies, systemd units, and local scripts.
- **Hot Reload**: Reload configuration files without restarting via `/reboot reload`.
- **Permissions**: Integrates with Vanilla server operator levels and mod permission managers.

---

## How Restarts Work

A Minecraft server process cannot boot itself back up once Java exits. RedstoneReboot cleanly stops the server, and an external supervisor brings it back online:

- **DEPEND_ON_HOST** *(Default)*: RedstoneReboot stops the server cleanly. Your hosting panel (Pterodactyl, Bloom, PebbleHost, Bisect, etc.) or container supervisor automatically reboots the process. No extra flags or API tokens required.
- **LOCALSCRIPT**: Uses an auto-generated script (`redstonereboot-start.sh` or `.bat`) with a loop for dedicated servers and local testing.
- **SYSTEMD**: Coordinates restarts through Linux systemd service units.
- **DOCKER**: Works with Docker restart policies (`restart: always`).
- **PTERODACTYL**: Sends power signals directly through the Pterodactyl Client API.

### What does ASSISTED mean in `/reboot doctor`?

If you configure a supervisor backend (`LOCALSCRIPT`, `SYSTEMD`, or `DOCKER`), RedstoneReboot checks for proof that an outside watcher is running (`-Dredstonereboot.active=true` in startup flags or `REDSTONEREBOOT_ACTIVE=1` in the environment).

Without this flag, `/reboot doctor` reports the **ASSISTED** state to prevent shutting down on a host without an active restart loop.

If your game host already reboots stopped servers automatically, you do not need supervisor wiring. Set `backends-enabled=false` or `active-backend=DEPEND_ON_HOST` in `config/redstonereboot/restart-backends.properties`.

---

## Installation

1. Drop the matching mod jar for your loader and Minecraft version into your server's `mods/` directory.
2. Launch the server to generate configuration files in `config/redstonereboot/`.
3. Configure `redstonereboot.properties` and `restart-backends.properties` as desired.
4. Run `/reboot reload` to apply updates without restarting.

---

## Commands

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

## Links

- [Documentation Wiki](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [Source Code](https://github.com/DemonZ-Development/RedstoneReboot)
- [Issue Tracker](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [Discord Support](https://discord.gg/GYsTt96ypf)
- [Bukkit Plugin Listing](https://www.curseforge.com/minecraft/bukkit-plugins/redstonereboot)
- [DemonZ Development](https://demonz.org)

---

## Sponsored by Nexeu Hosting

[![nexeu-sponsor](https://whodoesntloveavatars.s3.fra.databucket.eu/assets/promo.png)](https://nexeu.zip/)

Server hosting for this project is sponsored by Nexeu Hosting.
