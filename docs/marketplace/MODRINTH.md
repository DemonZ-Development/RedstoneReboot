# RedstoneReboot — Modrinth Project Description

<!-- Modrinth Markdown description -->

<div align="center">

![RedstoneReboot Banner](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**The Most Advanced Multi-Platform Minecraft Server Restart Engine**

<br/>

</div>

---

## 🔥 Why RedstoneReboot?

RedstoneReboot isn't just a restart plugin — it's a **professional-grade server lifecycle engine** built for production environments. From single-server setups to massive networks, it delivers reliability, intelligence, and elegance.

It supports:
- 🕐 **Intelligent Scheduling** (timezone support, daily intervals)
- 📊 **Real-Time Performance Tracking** (TPS and memory monitors)
- 🔔 **Rich Alerts** (chat, title, action bar, sound)
- 🚑 **Emergency Fail-safes** (automatic triggers on critical states)

---

## 📦 File Selection

Choose the file that matches your exact server platform.

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
| Bukkit-family servers | `1.9` through `1.21.1` | Run on Java 8+ *(1.9-1.16)*, Java 17+ *(1.17+)* |
| Folia | `1.20+` | Uses dedicated Region-Threaded build |
| Fabric | `1.20.1+` | Requires Fabric API |
| Forge | `1.20.4+` | Server-side mod |
| NeoForge | `1.20.4+` | Server-side mod |

---

## ⚙️ Installation

### Plugin Install (Bukkit/Folia)
1. Download the correct plugin file.
2. Place it in `plugins/`.
3. Start the server.
4. Configure `plugins/RedstoneReboot/config.yml`.

### Mod Install (Fabric/Forge/NeoForge)
1. Download the correct mod file.
2. Place it in `mods/`.
3. For Fabric, ensure Fabric API is present.
4. Start the server.
5. Configure `config/redstonereboot.properties`.

---

## 🎮 Commands & Integrations

- **/reboot** — View status and help
- **/reboot now [delay]** — Trigger a restart countdown
- **/reboot schedule <seconds>** — Schedule a future restart
- **/reboot cancel** — Cancel a pending restart
- **/reboot status** — Show restart schedule status
- **/reboot info** — Show health information
- **/reboot reload** — Reload configuration

Seamlessly integrates with **PlaceholderAPI** (`%redstonereboot_time_until%`, `%redstonereboot_next_restart%`, etc.) and handles **LuckPerms** permissions organically.

---

## 🔗 Quick Links

- 📖 [**Complete Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/wiki)
- 💻 [**GitHub Repository**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot)
- 🛠️ [**Developer API Docs**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 🐛 [**Bug Reports & Issues**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)
- 💬 [**Discord Support**](https://discord.gg/GYsTt96ypf)

---

<div align="center">

Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)

</div>
