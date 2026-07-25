# RedstoneReboot — Modrinth Project Description

<!-- Modrinth Markdown description -->

<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot

**A restart engine for Minecraft servers across every major platform**

[![bStats Servers](https://img.shields.io/bstats/servers/30751?label=Servers&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
[![bStats Players](https://img.shields.io/bstats/players/30751?label=Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

![bStats Chart](https://bstats.org/signatures/bukkit/RedstoneReboot.svg)

</div>

---

## Why RedstoneReboot?

RedstoneReboot handles when, why, and how your server restarts. It combines live health monitoring, flexible scheduling, and a backend system that hands the actual restart off to whatever process manager you already use.

It works for a single survival server or for a whole fleet running behind Pterodactyl.

### Key Capabilities

- **Intelligent Scheduling** — Multiple restart windows per day, with timezone support and day-of-week filtering
- **Health Monitoring** — Live TPS and memory tracking, with consecutive-check logic that avoids false restarts from brief spikes
- **Emergency Fail-safes** — Automatically restarts when TPS or memory crosses a critical threshold
- **Rich Alerts** — Chat, title, action bar, and configurable sound notifications
- **Backend Handoff** — Delegates the restart to Pterodactyl, Systemd, Docker, or a local wrapper script
- **Hot-Reload** — Change the backend config and run `/reboot reload` — no full server restart needed
- **PlaceholderAPI** — 8 placeholders for scoreboards, tab lists, and MOTD plugins (Bukkit builds)
- **bStats Metrics** — Anonymous usage telemetry ([view live stats](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

## The Backend System & Startup Loops

RedstoneReboot keeps "when to restart" separate from "how to restart" by supporting several backends:
* **SHUTDOWN_ONLY** — Graceful shutdown (your external wrapper restarts the process)
* **LOCALSCRIPT** — An auto-generated shell script handles process restarts
* **SYSTEMD** — Service integration on Linux
* **DOCKER** — Container-native restart policies
* **PTERODACTYL** — Direct panel API integration

### Do I need to set up a custom backend?
**If your server is already wrapped in a startup loop script** (a `.sh` or `.bat` file with a `while true` loop, a Docker container set to `restart: always`, or a systemd service), **SHUTDOWN_ONLY works out of the box.** When the restart timer runs out, the engine stops the server cleanly and your script starts it again.

### Why configure a custom backend then?
1. **Clean handoff (Pterodactyl / panels):** Avoid panel desyncs or false offline indicators. Instead of just shutting down, the plugin talks to your panel's API to request a clean power cycle.
2. **Self-healing bootups:** If you don't run a startup loop script, the LOCALSCRIPT backend spawns a new process to bring the server back up.
3. **Crash lockout safety:** Simple loops can get stuck in endless crash loops if a file gets corrupted. Custom backends add safety lockout timers to stop boot hammering.

---

## File Selection

Choose the file that matches your server platform:

### Platform Compatibility
- **Bukkit / Spigot / Paper / Purpur**: 1.9.x to 1.21.x+
- **Folia**: 1.20.1+
- **Fabric**: 1.20.1+
- **Forge**: 1.20.4+
- **NeoForge**: 1.20.4+

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
| Fabric | `1.20.1+` through `26.2+` | Requires Fabric API |
| Forge | `1.20.4+` through `26.2+` | Dedicated server-side mod build |
| NeoForge | `1.21.1+` through `26.2+` | Dedicated server-side mod build |

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
4. Configure `config/redstonereboot.properties` and `config/restart-backends.properties`.
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
| `%redstonereboot_version%` | `1.5.0` |
| `%redstonereboot_timezone%` | `Europe/London` |

> MOTD compatible as of v1.3.3+.

---

## Quick Links

- [**Complete Wiki**](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [**GitHub Repository**](https://github.com/DemonZ-Development/RedstoneReboot)
- [**Developer API Docs**](https://github.com/DemonZ-Development/RedstoneReboot/blob/main/docs/api/README.md)
- [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- [**Bug Reports & Issues**](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [**Discord Support**](https://discord.gg/GYsTt96ypf)

---

Made by [**DemonZ Development**](https://demonzdevelopment.online)

*Minecraft server tooling built by DemonZ Development.*
