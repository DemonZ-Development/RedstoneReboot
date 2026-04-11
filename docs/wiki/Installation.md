# Installation Guide

## Bukkit / Spigot / Paper / Purpur / Compatible Forks

### Requirements

- Minecraft `1.9` through `1.21.1`
- Java `8+` for older servers
- Java `17+` for modern servers
- Optional: PlaceholderAPI, LuckPerms

### File

- `RedstoneReboot-Bukkit-<version>.jar`

### Steps

1. Download the Bukkit build from your release channel.
2. Stop the server.
3. Place the jar in `plugins/`.
4. Start the server.
5. Edit `plugins/RedstoneReboot/config.yml`.
6. Run `/reboot status` to verify the plugin loaded.

---

## Folia

### Requirements

- Folia `1.20+`
- Java `17+`

### File

- `RedstoneReboot-Folia-<version>.jar`

### Steps

1. Download the Folia build.
2. Place it in `plugins/`.
3. Start the server.
4. Edit `plugins/RedstoneReboot/config.yml`.
5. Run `/reboot status`.

### Note

The Folia build uses the dedicated Folia scheduler bridge rather than the standard Bukkit scheduler path.

---

## Fabric

### Requirements

- Fabric server `1.20.1+`
- Java `17+`
- Fabric API

### File

- `RedstoneReboot-Fabric-<version>.jar`

### Steps

1. Place the RedstoneReboot Fabric jar into `mods/`.
2. Place Fabric API into `mods/`.
3. Start the server.
4. Edit `config/redstonereboot.properties`.

---

## Forge

### Requirements

- Forge server `1.20.4+`
- Java `17+`

### File

- `RedstoneReboot-Forge-<version>.jar`

### Steps

1. Place the Forge jar into `mods/`.
2. Start the server.
3. Edit `config/redstonereboot.properties`.

---

## NeoForge

### Requirements

- NeoForge server `1.20.4+`
- Java `17+`

### File

- `RedstoneReboot-NeoForge-<version>.jar`

### Steps

1. Place the NeoForge jar into `mods/`.
2. Start the server.
3. Edit `config/redstonereboot.properties`.

---

## Quick Checks

### Plugin Side

- `/reboot status` should return platform, timezone, and next restart info.
- `plugins/RedstoneReboot/config.yml` should exist after first startup.

### Mod Side

- The mod should load on the dedicated server without command-registration errors.
- `config/redstonereboot.properties` should exist after first startup.

---

## Troubleshooting

### Wrong File Variant

Use the Bukkit file for Bukkit-family servers, the Folia file for Folia, and the loader-specific mod file for Fabric/Forge/NeoForge.

### Commands Missing

- Plugin deployments: verify the plugin loaded successfully and `plugin.yml` was processed.
- Mod deployments: verify server startup completed and command registration logs contain no errors.

### PlaceholderAPI Not Working

- PlaceholderAPI only applies to Bukkit-family plugin deployments.
- Install PlaceholderAPI separately.
- Reload PlaceholderAPI after installation if needed.
