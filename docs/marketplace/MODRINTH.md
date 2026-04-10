# RedstoneReboot — Modrinth Project Page

<!-- Modrinth Markdown Description -->

<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**Advanced Multi-Platform Server Restart Engine**

![Minecraft](https://img.shields.io/badge/MC-1.9_--_1.21.1-22c55e?style=flat-square)
![Java](https://img.shields.io/badge/Java-17+-f97316?style=flat-square)
![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)

</div>

---

## About

RedstoneReboot is the most advanced restart management system for Minecraft servers. It provides intelligent scheduling, real-time performance monitoring, multi-channel alerts, and a comprehensive developer API.

Built for **production environments** — from single-server setups to massive networks.

## Supported Platforms

| Platform | Versions | Loader Type |
|----------|----------|-------------|
| Bukkit / Spigot / Paper | 1.9 — 1.21.1 | Plugin |
| Folia | 1.20+ | Plugin |
| Fabric | 1.20.4+ | Mod |
| Forge | 1.20.4+ | Mod |
| NeoForge | 1.20.4+ | Mod |

## Features

### 🕐 Intelligent Scheduling
Configure multiple daily restart windows with **full timezone support**. Filter by day-of-week, set custom warning intervals, and let the system handle the rest.

### 📊 Performance Monitoring
Real-time TPS and memory tracking with automatic restart triggers. Configurable thresholds and consecutive-check requirements prevent false positives.

### 🚨 Emergency Restart
When TPS drops below critical levels (default: 12.0) or memory exceeds 95%, RedstoneReboot immediately:
1. Warns all players with emergency alerts
2. Saves all world data
3. Gracefully shuts down the server

### 🔔 Multi-Channel Alerts
- **Chat Messages** — Formatted with color codes
- **Screen Titles** — Big text with subtitle
- **Action Bar** — Persistent bottom-screen text
- **Sounds** — Configurable Minecraft sounds

### 🔗 Integrations
- **PlaceholderAPI** — 10+ placeholders for scoreboards, MOTDs, tab lists
- **LuckPerms** — Permission integration via reflection
- **Developer API** — Events, hooks, and programmatic restart control

## Installation

### Plugin (Bukkit/Paper/Folia)
1. Download the Bukkit JAR from the Files tab
2. Place in `plugins/` folder
3. Start server — config auto-generates
4. Edit `plugins/RedstoneReboot/config.yml`

### Mod (Fabric/Forge/NeoForge)
1. Download the appropriate mod JAR from the Files tab
2. Place in `mods/` folder (Fabric requires Fabric API)
3. Start server — config generates at `config/redstonereboot/`

## Commands

| Command | Permission |
|---------|-----------|
| `/reboot` | `redstonereboot.use` |
| `/reboot now [delay]` | `redstonereboot.restart.now` |
| `/reboot schedule <seconds>` | `redstonereboot.restart.schedule` |
| `/reboot cancel` | `redstonereboot.restart.cancel` |
| `/reboot status` | `redstonereboot.status` |
| `/reboot reload` | `redstonereboot.config.reload` |

## Links

- 📖 [Wiki](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/wiki/Home.md)
- 🛠️ [Developer API](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 🐛 [Issue Tracker](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)
- 💬 [Discord](https://discord.gg/GYsTt96ypf)
- 🌐 [DemonZ Development](https://demonzdevelopment.online)

---

*Made with ❤️ by [DemonZ Development](https://demonzdevelopment.online)*
