## RedstoneReboot 1.6.6

A quality of life patch release protecting restart history across modpack updates.

### Highlights

- Moved default restart history logging from the config/plugin folder to a dedicated subfolder at `logs/redstonereboot/restarts.log` so modpack updates wiping `config/` preserve your restart logs without cluttering the root logs folder.
- Added a configurable `history-file` setting across all platforms (Paper, Purpur, Folia, Fabric, Forge, NeoForge).
- Bumped configuration versions (Bukkit v4, Mod Platforms v3) with automatic migration to preserve existing user setups seamlessly.
- Added automatic legacy migration: existing `restarts.log` files found in the data directory are safely relocated on first boot.
- Added automatic directory creation to ensure custom log paths create missing folders reliably.

### Requirements

- Bukkit, Paper, Spigot, and Purpur: Java 17+
- Folia: Java 17+
- Fabric 1.20.1: Java 17+ (Fabric 26.x: Java 25)
- Forge 1.20.4: Java 17+ (Forge 26.x: Java 25)
- NeoForge 1.21.1: Java 21+ (NeoForge 26.x: Java 25)
