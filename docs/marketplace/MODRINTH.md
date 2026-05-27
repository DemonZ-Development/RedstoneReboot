# RedstoneReboot — Modrinth Project Description

<!-- Modrinth Markdown description -->

<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**A Multi-Platform Minecraft Server Restart Engine**

[![bStats Servers](https://img.shields.io/bstats/servers/30751?label=Servers&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
[![bStats Players](https://img.shields.io/bstats/players/30751?label=Players&color=blue)](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

![bStats Chart](https://bstats.org/signatures/bukkit/RedstoneReboot.svg)

</div>

---

## 🔥 Why RedstoneReboot?

RedstoneReboot is a **production-grade server lifecycle engine** that provides precise control over when, why, and how your server restarts. Backed by real-time health monitoring, intelligent scheduling, and a pluggable backend handoff system.

From a single survival server to a fleet behind Pterodactyl — it delivers reliability, intelligence, and elegance.

### Key Capabilities

- 🕐 **Intelligent Scheduling** — Multiple daily restart windows with global timezone support and day-of-week filters
- 📊 **Health Monitoring** — Real-time TPS and memory tracking with consecutive-check false-positive protection
- 🚑 **Emergency Fail-safes** — Automatic restart triggers on critical TPS or memory breach
- 🔔 **Rich Alerts** — Chat, title, action bar, and configurable sound notifications
- 🔌 **Backend Handoff** — Delegate to Pterodactyl, Systemd, Docker, or local wrapper scripts
- 🔄 **Hot-Reload** — Change backend config and `/reboot reload` — no full server restart needed
- 🧩 **PlaceholderAPI** — 8 placeholders for scoreboards, tab lists, and MOTD plugins (Bukkit builds)
- 📈 **bStats Metrics** — Anonymous usage telemetry ([view live stats](https://bstats.org/plugin/bukkit/RedstoneReboot/30751))

---

## 📦 File Selection

Choose the file that matches your server platform:

### 🌍 Platform Compatibility
- **Bukkit / Spigot / Paper / Purpur**: 1.9.x to 1.21.x+
- **Folia**: 1.20.1+
- **Fabric**: 1.20.1+
- **Forge**: 1.20.4+
- **NeoForge**: 1.20.4+

---

| 🖥️ Platform | Distribution Type | 📄 File |
|-----------|------------------|------|
| **Bukkit / Spigot / Paper / Purpur** | Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |
| **Fabric** | Mod | `RedstoneReboot-Fabric-<version>.jar` |
| **Forge** | Mod | `RedstoneReboot-Forge-<version>.jar` |
| **NeoForge** | Mod | `RedstoneReboot-NeoForge-<version>.jar` |

---

## 📋 Supported Versions

| Platform | Minecraft Versions | Notes |
|----------|--------------------|-------|
| Bukkit-family servers | `1.9` through `26.x` | Java 8+ *(legacy)*, Java 17+ *(modern)*, Java 25 *(26.x+)* |
| Folia | `1.20+` through `26.x` | Dedicated region-threaded build |
| Fabric | `1.20.1+` through `26.x` | Requires Fabric API |
| Forge | `1.20.4+` through `26.x` | Dedicated server-side mod build |
| NeoForge | `1.21.1+` through `26.x` | Dedicated server-side mod build |

---

## ⚙️ Installation

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

## 🎮 Commands

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

## 🔗 PlaceholderAPI (Bukkit Builds)

| Placeholder | Example Output |
|-------------|----------------|
| `%redstonereboot_next_restart%` | `2026-04-15 06:00:00 Europe/London` |
| `%redstonereboot_time_until%` | `2h 30m` |
| `%redstonereboot_status%` | `Normal operation` |
| `%redstonereboot_reason%` | `Scheduled Restart` |
| `%redstonereboot_tps%` | `19.8` |
| `%redstonereboot_memory%` | `62.4%` |
| `%redstonereboot_version%` | `1.4.2` |
| `%redstonereboot_timezone%` | `Europe/London` |

> MOTD compatible as of v1.3.3+.

---

## 🔗 Quick Links

- 📖 [**Complete Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/wiki)
- 💻 [**GitHub Repository**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot)
- 🛠️ [**Developer API Docs**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 📊 [**bStats**](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)
- 🐛 [**Bug Reports & Issues**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)
- 💬 [**Discord Support**](https://discord.gg/GYsTt96ypf)

---

<div align="center">

<img src="https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/logo.png" alt="RedstoneReboot Logo" width="128"/>
<br>
Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)
<br>
<i>Premium Minecraft infrastructure, engineered for scale.</i>

</div>
