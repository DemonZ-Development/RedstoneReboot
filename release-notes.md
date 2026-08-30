## RedstoneReboot 1.6.1

This maintenance release improves cross-platform build reliability and corrects message delivery through the public API and Discord webhook bridge.

### Highlights

- Isolates Fabric, Forge, NeoForge, Paper, and Spigot dependency repositories while keeping ForgeGradle's MCP tooling resolvable.
- Uses explicit Java 17 and Java 21 toolchains in CI and releases.
- Runs cross-loader Gradle tasks serially to avoid generated-workspace contention.
- Honors `MessageAdapter` filters for scheduled and postponed notifications.
- Delivers Bukkit-family webhook/API alerts even when no players are online.
- Prevents duplicate countdown chat messages in Discord and improves failure diagnostics.
- Publishes exactly one runtime JAR per platform plus SHA-256 checksums after validating the release tag against the project version.

### Requirements

- Bukkit, Paper, Spigot, and Purpur: Java 17+
- Folia: Java 17+
- Fabric 1.20.1: Java 17+
- Forge 1.20.4: Java 17+
- NeoForge 1.21.1: Java 21+
