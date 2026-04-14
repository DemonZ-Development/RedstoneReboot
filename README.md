<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**The Most Advanced Multi-Platform Minecraft Server Restart Engine**

[![CI](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/actions/workflows/ci.yml/badge.svg)](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/sdemonzdevelopment-spec/RedstoneReboot?color=green&label=latest)](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/releases/latest)
[![License](https://img.shields.io/github/license/sdemonzdevelopment-spec/RedstoneReboot)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk)](https://adoptium.net/)
[![bStats](https://img.shields.io/bstats/players/30751?label=bStats%20Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

**Bukkit** · **Paper** · **Purpur** · **Folia** · **Fabric** · **Forge** · **NeoForge**

</div>

---

## 🔥 Overview

RedstoneReboot is a **production-grade server lifecycle engine** that gives server administrators complete control over restart scheduling, health-based automation, and multi-platform backend handoff.

Whether you're running a single Paper server, a Folia network, or a fleet of modded Fabric/Forge servers behind Pterodactyl — RedstoneReboot provides the reliability, intelligence, and configurability you need.

### Key Capabilities

| Feature | Description |
|---------|-------------|
| 🕐 **Intelligent Scheduling** | Multiple daily restart windows with timezone awareness and day-of-week filters |
| 📊 **Health Monitoring** | Real-time TPS and memory tracking with consecutive-check protection against false positives |
| 🚑 **Emergency Fail-safes** | Automatic emergency restarts when critical TPS or memory thresholds are breached |
| 🔔 **Rich Alerts** | Chat messages, titles, action bar, and configurable sounds with countdown warnings |
| 🔌 **Backend Handoff** | Delegate restart execution to Pterodactyl, Systemd, Docker, or local scripts |
| 🔄 **Hot-Reload** | Change backend config and `/reboot reload` — no full server restart needed |
| 📈 **bStats Metrics** | Anonymous usage telemetry via [bStats](https://bstats.org/plugin/bukkit/RedstoneReboot/30751) |
| 🧩 **PlaceholderAPI** | 8 placeholders for scoreboards, tab lists, and MOTD plugins |

---

## 📦 Quick Start

### Plugin Install (Bukkit / Paper / Folia)

1. Download the correct JAR from [Releases](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/releases/latest).
2. Place it in your `plugins/` folder.
3. Start the server — config files are generated automatically.
4. Edit `plugins/RedstoneReboot/config.yml` and `plugins/RedstoneReboot/restart-backends.properties`.
5. Run `/reboot reload` to apply changes.

### Mod Install (Fabric / Forge / NeoForge)

1. Download the correct mod JAR.
2. Place it in your `mods/` folder (Fabric requires Fabric API).
3. Start the server.
4. Edit `config/redstonereboot.properties` and `config/restart-backends.properties`.
5. Run `/reboot reload` to apply changes.

---

## 🖥️ Supported Platforms

| Platform | Type | Minecraft Versions | Java |
|----------|------|--------------------|------|
| Bukkit / Spigot / Paper / Purpur | Plugin | 1.9 – 1.21.1 | 8+ (legacy), 17+ (modern) |
| Folia | Plugin | 1.20+ | 17+ |
| Fabric | Mod | 1.20.1+ | 17+ |
| Forge | Mod | 1.20.4+ | 17+ |
| NeoForge | Mod | 1.20.4+ | 17+ |

---

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/reboot` | `redstonereboot.use` | Show plugin status and help |
| `/reboot status` | `redstonereboot.status` | View restart schedule and countdown |
| `/reboot info` | `redstonereboot.status` | Show server health diagnostics |
| `/reboot now [delay]` | `redstonereboot.restart.now` | Trigger a restart with optional countdown |
| `/reboot schedule <seconds>` | `redstonereboot.restart.schedule` | Schedule a future restart |
| `/reboot cancel` | `redstonereboot.restart.cancel` | Cancel a pending restart |
| `/reboot doctor` | `redstonereboot.doctor` | Run backend and environment diagnostics |
| `/reboot reload` | `redstonereboot.config.reload` | Hot-reload all configuration files |

---

## 🔗 PlaceholderAPI Integration

RedstoneReboot integrates with [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) on Bukkit-family servers.

| Placeholder | Output |
|-------------|--------|
| `%redstonereboot_next_restart%` | `2026-04-15 06:00:00 Asia/Kolkata` |
| `%redstonereboot_time_until%` | `2h 30m` |
| `%redstonereboot_status%` | `Normal operation` or `Restart in progress` |
| `%redstonereboot_reason%` | `Scheduled Restart` or `None` |
| `%redstonereboot_tps%` | `19.8` |
| `%redstonereboot_memory%` | `62.4%` |
| `%redstonereboot_version%` | `1.3.3` |
| `%redstonereboot_timezone%` | `Asia/Kolkata` |

> **MOTD Compatible** — v1.3.3+ includes null-safety fixes for server-list MOTD plugins.

---

## ⚙️ Backend System

RedstoneReboot separates the **"when to restart"** from the **"how to restart"** through its backend handoff system. Configure `restart-backends.properties` to choose:

| Backend | Use Case |
|---------|----------|
| `SHUTDOWN_ONLY` | Default — graceful shutdown, external process manager restarts |
| `LOCALSCRIPT` | Auto-generated wrapper script handles restart loop |
| `SYSTEMD` | Linux servers managed by systemd services |
| `DOCKER` | Docker containers with restart policies |
| `PTERODACTYL` | Pterodactyl panel API sends restart signals |

**Hot-reload**: Edit the backend config and run `/reboot reload` — no full server restart required.

---

## 📖 Documentation

- 📚 [**Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/wiki/Home.md) — Installation, configuration, backends, and troubleshooting
- 🛠️ [**Developer API**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md) — Bukkit API for plugin developers
- 📊 [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751) — Server usage statistics
- 💬 [**Discord**](https://discord.gg/GYsTt96ypf) — Support and community
- 📸 [**Instagram**](https://instagram.com/demonzdevelopment) — Updates and announcements

---

## 🏗️ Building from Source

```bash
git clone https://github.com/sdemonzdevelopment-spec/RedstoneReboot.git
cd RedstoneReboot
./gradlew build
```

Requires **Java 21+** for building (NeoForge toolchain requirement).

Output JARs are located in `<module>/build/libs/`.

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development setup, coding standards, and PR guidelines.

---

## 📄 License

This project is licensed under the terms in the [LICENSE](LICENSE) file.

---

<div align="center">

Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)

</div>
