# RedstoneReboot — Hangar Project Description

<!-- Paper/Folia-focused marketplace copy -->

<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**The Most Advanced Multi-Platform Minecraft Server Restart Engine**

[![bStats](https://img.shields.io/bstats/players/30751?label=bStats%20Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

</div>

---

## 🔥 Why RedstoneReboot?

RedstoneReboot is a **production-grade restart management plugin** for modern Bukkit-based servers, with dedicated support for both standard Paper-style schedulers and **Folia's region-threaded model**.

It gives server administrators precise control over restart scheduling, health-based automation, and backend handoff — from a single survival server to a multi-node network behind Pterodactyl.

---

## 📦 Hangar Scope

This Hangar page serves the plugin builds for Paper and its forks:

| 🖥️ Platform | Distribution Type | 📄 File |
|-----------|------------------|------|
| **Paper / Purpur / Pufferfish / Spigot** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |

> [!NOTE]
> Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub releases.

---

## ✨ Key Capabilities

### 🕐 Intelligent Scheduling
- Multiple restart windows per day with timezone-aware timing
- Day-of-week filtering (e.g., weekdays only)
- Configurable warning countdowns with granular alert thresholds

### 📊 Health Monitoring & Emergency Fail-safes
- TPS and memory threshold monitoring with consecutive-check protection
- Dedicated emergency thresholds for critical situations
- Graceful stop handling with world-save delay

### 🔌 Backend Handoff System
- **SHUTDOWN_ONLY** — graceful shutdown for external restarters
- **LOCALSCRIPT** — auto-generated wrapper script restart loop
- **SYSTEMD** / **DOCKER** / **PTERODACTYL** — native environment integration
- Hot-reload: edit `restart-backends.properties` and `/reboot reload`
- **Do I need a custom backend?** If your server is already wrapped in a startup loop script (like a `.sh` or `.bat` file with a `while true` loop, a Docker container set to `restart: always`, or a systemd service), **`SHUTDOWN_ONLY` works out of the box!** When the restart timer runs out, the engine gracefully stops the server, and your script starts it back up automatically.
- **Why configure a custom backend then?** 
  1. *Intelligent Handoff (Pterodactyl / Panels)*: Avoid panel desyncs or false offline indicators by requesting a clean power cycle via the panel's API.
  2. *Self-Healing Bootups*: The `LOCALSCRIPT` backend spawns a new process to boot the server back up automatically if you don't run a loop script.
  3. *Crash Lockout Safety*: Implements safety lockout timers to prevent endless boot hammering if files get corrupted.

### 🔔 Rich Alerts & Integrations
- Chat messages, titles, action bar, and configurable sounds
- **PlaceholderAPI**: 8 placeholders for scoreboards, tab lists, and MOTD plugins
- **LuckPerms**: full permission resolution with group support
- **bStats**: anonymous usage metrics ([view](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

### 🌍 Platform Compatibility
- **Bukkit / Spigot / Paper / Purpur**: 1.9.x to 26.x
- **Folia**: 1.20.1+ to 26.x
- **Fabric**: 1.20.1+ to 26.x
- **Forge**: 1.20.4+ to 26.x
- **NeoForge**: 1.21.1+ to 26.x (Java 21+)

---

## ⚙️ Installation

1. Download the correct file for your platform.
2. Place it in `plugins/`.
3. Start the server — config files are generated automatically.
4. Edit `plugins/RedstoneReboot/config.yml` and `restart-backends.properties`.
5. Run `/reboot reload` to apply changes.

---

## 🎮 Commands

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

## 📊 PlaceholderAPI

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

## 🔗 Quick Links

- 📖 [**Complete Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/wiki)
- 💻 [**GitHub Repository**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot)
- 🛠️ [**Developer API Docs**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 📊 [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- 🐛 [**Bug Tracker & Issues**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)

---

Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)
