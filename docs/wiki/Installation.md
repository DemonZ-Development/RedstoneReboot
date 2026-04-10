# 📥 Installation Guide

## Bukkit / Spigot / Paper

### Requirements
- **Server**: Spigot, Paper, or any Spigot fork (1.9 — 1.21.1)
- **Java**: Java 8+ for MC 1.9-1.16 · Java 17+ for MC 1.17+
- **Optional**: PlaceholderAPI, LuckPerms

### Steps

1. Download `RedstoneReboot-Bukkit-1.0.0.jar` from the [latest release](https://github.com/DemonZDevelopment/RedstoneReboot/releases/latest)
2. Stop your server
3. Place the JAR in your server's `plugins/` directory
4. Start the server
5. A default `plugins/RedstoneReboot/config.yml` will be generated
6. Edit the config to match your server's timezone, restart schedule, and preferences
7. Run `/reboot reload` or restart the server

### Verify Installation
Run `/reboot status` — you should see the plugin version, timezone, and next scheduled restart.

---

## Folia

### Requirements
- **Server**: Folia 1.20+
- **Java**: Java 17+

### Steps
Same as Bukkit, but download the `RedstoneReboot-Folia-1.0.0.jar` variant instead.

> **Note**: Folia uses region-threaded scheduling. RedstoneReboot's Folia module uses the `AsyncScheduler` to ensure thread-safe operation.

---

## Fabric

### Requirements
- **Server**: Fabric Server 1.20.4+
- **Java**: Java 17+
- **Dependencies**: Fabric API

### Steps
1. Download `RedstoneReboot-Fabric-1.0.0.jar` from [Releases](https://github.com/DemonZDevelopment/RedstoneReboot/releases/latest)
2. Place the JAR in your server's `mods/` directory alongside `fabric-api`
3. Start the server
4. Configuration is generated at `config/redstonereboot/config.yml`

---

## Forge

### Requirements
- **Server**: Forge Server 1.20.4+
- **Java**: Java 17+

### Steps
1. Download `RedstoneReboot-Forge-1.0.0.jar` from [Releases](https://github.com/DemonZDevelopment/RedstoneReboot/releases/latest)
2. Place the JAR in your server's `mods/` directory
3. Start the server
4. Configuration is generated at `config/redstonereboot/config.yml`

---

## NeoForge

### Requirements
- **Server**: NeoForge Server 1.20.4+
- **Java**: Java 17+

### Steps
1. Download `RedstoneReboot-NeoForge-1.0.0.jar` from [Releases](https://github.com/DemonZDevelopment/RedstoneReboot/releases/latest)
2. Place the JAR in your server's `mods/` directory
3. Start the server
4. Configuration is generated at `config/redstonereboot/config.yml`

---

## Troubleshooting

### Plugin not loading?
- Check the server console for errors — RedstoneReboot has strict config validation by default
- Ensure you're using the correct JAR for your platform (Bukkit vs Fabric vs Forge)
- Verify your Java version meets the minimum requirement

### Commands not registering?
- On Bukkit: Ensure `plugin.yml` exists inside the JAR
- On modded: Commands are registered on server start — check for initialization errors

### PlaceholderAPI not working?
- Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) (**Bukkit only**)
- Enable in config: `placeholders.enabled: true`
- Run `/papi reload` after installation
