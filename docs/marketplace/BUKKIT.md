# RedstoneReboot — Bukkit/Plugin Directory Copy

<!-- Bukkit-oriented marketplace copy -->

<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**The Most Advanced Multi-Platform Minecraft Server Restart Engine**

[![bStats](https://img.shields.io/bstats/players/30751?label=bStats%20Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

</div>

---

## 🔥 Why RedstoneReboot?

RedstoneReboot is a **production-grade restart management plugin** for Bukkit-family servers. It gives server administrators precise control over restart scheduling, health-based automation, and a pluggable backend handoff system — from single survival servers to multi-node networks.

---

## 📦 Scope

This page serves the plugin builds for Bukkit, Spigot, Paper, and Folia:

| 🖥️ Platform | Distribution Type | 📄 File |
|-----------|------------------|------|
| **Bukkit / Spigot / Paper / Purpur / Pufferfish** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |

> [!NOTE]
> Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub releases.

---

## ✨ Key Capabilities

### 🕐 Intelligent Scheduling
- Multiple restart windows per day with timezone-aware timing
- Day-of-week filtering and configurable warning countdowns

### 📊 Health Monitoring & Emergency Fail-safes
- TPS and memory threshold monitoring with consecutive-check protection
- Dedicated emergency thresholds for critical situations
- Graceful stop handling with world-save delay

### 🔌 Backend Handoff System
- **SHUTDOWN_ONLY** — graceful shutdown for external restarters
- **LOCALSCRIPT** — auto-generated wrapper script restart loop
- **SYSTEMD** / **DOCKER** / **PTERODACTYL** — native environment integration
- Hot-reload: edit `restart-backends.properties` and `/reboot reload`

### 🔔 Rich Alerts & Integrations
- Chat, titles, action bar, and sounds
- **PlaceholderAPI**: 8 placeholders for scoreboards, tab lists, MOTD
- **LuckPerms**: full permission resolution
- **bStats**: anonymous metrics ([view](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

## 📋 Supported Versions

- **Bukkit-family servers**: `1.9` through `1.21.1` *(Java 17+ for modern, Java 8+ for legacy).*
- **Folia**: `1.20+` *(Java 17+).*

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

- 📖 [**Complete Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/wiki/Home.md)
- 💻 [**GitHub Repository**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot)
- 🛠️ [**Developer API Docs**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 📊 [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- 🐛 [**Bug Tracker & Issues**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)

---

<div align="center">

Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)

</div>
