# RedstoneReboot — Bukkit/Plugin Directory Copy

<!-- Bukkit-oriented marketplace copy -->

<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/sdemonzdevelopment-spec/RedstoneReboot/main/assets/banner.png)

# ⚡ RedstoneReboot

**The Most Advanced Multi-Platform Minecraft Server Restart Engine**

<br/>

</div>

---

## 🔥 Why RedstoneReboot?

RedstoneReboot is a restart-management plugin for modern Bukkit-based servers. It helps server owners schedule restarts, warn players before shutdown, and react to poor server health with automatic restart logic.

If you need scheduled restarts, warning broadcasts, or automated restart decisions based on TPS and memory pressure, this project is aimed at production server administration.

---

## 📦 Scope

This Bukkit Page exclusively serves the plugin builds built for Spigot and Paper families:

| 🖥️ Platform | Distribution Type | 📄 File |
|-----------|------------------|------|
| **Bukkit / Spigot / Paper / Purpur / Pufferfish**| Plugin | `RedstoneReboot-Bukkit-<version>.jar` |
| **Folia** | Plugin | `RedstoneReboot-Folia-<version>.jar` |

> [!NOTE]  
> Fabric, Forge, and NeoForge mod variants are distributed separately on Modrinth and GitHub releases.

---

## ✨ Main Capabilities

### 🕐 Scheduled Restart Automation
- Multiple restart windows per day
- Timezone-aware scheduling
- Day-of-week filtering
- Configurable warning countdowns

### 📊 Server Health Monitoring
- TPS threshold monitoring
- Memory usage threshold monitoring
- Consecutive-check protection against false positives
- Automatic restart scheduling when conditions stay degraded

### 🚑 Emergency Restart Flow
- Dedicated emergency thresholds
- Emergency player alerts
- Graceful stop handling

### 🔔 Player Alerting
- Chat messages
- Titles
- Action bar notifications
- Configurable sounds

*(Supports PlaceholderAPI and LuckPerms integrations out of the box).*

---

## 📋 Supported Versions

- **Bukkit-family servers**: `1.9` through `1.21.1` *(Requires Java 17+ for modern setups, Java 8+ for legacy setups).*
- **Folia**: `1.20+` *(Requires Java 17+).*

---

## ⚙️ Installation

1. Download the correct file for your platform.
2. Place it in `plugins/`.
3. Start the server.
4. Edit `plugins/RedstoneReboot/config.yml`.
5. Run `/reboot status` after startup to verify.

---

## 🎮 Commands

| Command | Purpose |
|---------|---------|
| `/reboot` | Show plugin status and help |
| `/reboot now [delay]` | Start a countdown-based restart |
| `/reboot schedule <seconds>` | Schedule a future restart |
| `/reboot cancel` | Cancel a pending restart |
| `/reboot status` | View timing and restart details |
| `/reboot info` | View monitored server health |
| `/reboot reload` | Reload configuration |

---

## 🔗 Quick Links

- 📖 [**Complete Wiki**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/wiki)
- 💻 [**GitHub Repository**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot)
- 🛠️ [**Developer API Docs**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/blob/main/docs/api/README.md)
- 🐛 [**Bug Tracker & Issues**](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues)

---

<div align="center">

Crafted with ❤️ by [**DemonZ Development**](https://demonzdevelopment.online)

</div>
