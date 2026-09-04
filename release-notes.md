## RedstoneReboot 1.6.2

This patch makes the Fabric, Forge, and NeoForge packages safe to publish after full loader startup testing.

### Highlights

- Uses loader-mapped Minecraft APIs for accurate live TPS readings on every mod platform.
- Prevents older loader-specific Modrinth builds from being reported as updates.
- Builds Forge against the recommended 49.2.0 release with the correct Java 17 toolchain.
- Corrects the NeoForge 1.21.1 mod descriptor and development runtime packaging.
- Restricts each mod artifact to the Minecraft and loader generation it was built and tested against.
- Retains the backend, scheduling, alert, Discord webhook, and developer API behavior from 1.6.1.

### Requirements

- Bukkit, Paper, Spigot, and Purpur: Java 17+
- Folia: Java 17+
- Fabric 1.20.1: Java 17+
- Forge 1.20.4: Java 17+
- NeoForge 1.21.1: Java 21+
