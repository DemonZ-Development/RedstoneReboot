# RedstoneReboot

**Scheduled and health-based restarts for Bukkit/Paper plugins and server-side mods**

---

## Features

RedstoneReboot manages automated restarts, performance monitoring, and backend process integration across single servers and server networks.

- **Scheduling** — Multiple daily restart times with timezone support and day-of-week filters
- **Health Checks** — Real-time TPS and memory tracking with consecutive checks to prevent false triggers
- **Emergency Restarts** — Triggers restarts if TPS drops or memory usage exceeds safety limits
- **Notifications** — Countdown alerts via chat, titles, action bar, and sounds
- **Backend Handoff** — Delegates process restarts to Pterodactyl API, systemd, Docker, or custom scripts
- **Hot Reload** — Apply backend configuration changes using `/reboot reload`
- **PlaceholderAPI** — 8 placeholders for scoreboards, tab lists, and MOTD plugins (Bukkit/Folia)

---

## Backend System & Startup Loops

RedstoneReboot separates "when to restart" from "how to restart":

- **DEPEND_ON_HOST** — Clean shutdown (your panel, Docker policy, or script restarts the process)
- **LOCALSCRIPT** — Auto-generated shell script handles process restarts
- **SYSTEMD** — System service integration on Linux
- **DOCKER** — Container restart policy integration
- **PTERODACTYL** — Direct panel API power actions

### Do I need a custom backend?
If your server runs inside a loop script, Docker container with `restart: always`, or systemd service, **DEPEND_ON_HOST works out of the box.** When the countdown ends, the engine shuts down the server cleanly and your supervisor starts it again.

---

## Supported Platforms & Minecraft Versions

| Platform | Minecraft Versions | Java Runtime | Notes |
| :--- | :--- | :--- | :--- |
| **Bukkit / Spigot / Paper / Purpur** | `1.9` through `26.3+` | Java 17+ / Java 25 (26.x+) | Universal plugin artifact |
| **Folia** | `1.20+` through `26.3+` | Java 17+ / Java 25 (26.x+) | Dedicated region-threaded build |
| **Fabric** | `1.20.1` (legacy) / `26.1` – `26.3+` (modern) | Java 17 / Java 25 (26.x+) | Requires Fabric API |
| **Forge** | `1.20.4` (legacy) / `26.1` – `26.3+` (modern) | Java 17 / Java 25 (26.x+) | Dedicated server-side mod build |
| **NeoForge** | `1.21.1` (legacy) / `26.1` – `26.3+` (modern) | Java 21 / Java 25 (26.x+) | Dedicated server-side mod build |

---

## Commands

| Command | Description |
| :--- | :--- |
| `/reboot` | View status and help |
| `/reboot now [delay]` | Trigger a restart countdown |
| `/reboot schedule <seconds>` | Schedule a future restart |
| `/reboot cancel` | Cancel a pending restart |
| `/reboot status` | Show restart schedule status |
| `/reboot info` | Show health information |
| `/reboot doctor` | Run backend & environment diagnostics |
| `/reboot reload` | Hot-reload all configuration |

---

## Links & Community

- [**GitHub Repository**](https://github.com/DemonZ-Development/RedstoneReboot)
- [**Documentation & Wiki**](https://github.com/DemonZ-Development/RedstoneReboot/wiki)
- [**Issue Tracker**](https://github.com/DemonZ-Development/RedstoneReboot/issues)
- [**Discord Support**](https://discord.gg/GYsTt96ypf)
- [**Official Website**](https://demonz.org)
