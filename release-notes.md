## RedstoneReboot 1.6.5

A maintenance and documentation release clarifying host-managed restart workflows.

### Highlights

- Clarified default restart mode messaging: `DEPEND_ON_HOST` is the standard mode for panel environments (Pterodactyl, Bloom, PebbleHost, Bisect, Docker) where the host automatically restarts stopped processes.
- Improved `/reboot doctor` diagnostics to explain what `ASSISTED` means on supervisor backends and confirm container-managed environments safely.
- Updated documentation and marketplace listings across Modrinth, CurseForge, Hangar, SpigotMC, and Bukkit.
- Split CurseForge documentation into dedicated listings for Minecraft Mods and Bukkit Plugins.

### Requirements

- Bukkit, Paper, Spigot, and Purpur: Java 17+
- Folia: Java 17+
- Fabric 1.20.1: Java 17+ (Fabric 26.x: Java 25)
- Forge 1.20.4: Java 17+ (Forge 26.x: Java 25)
- NeoForge 1.21.1: Java 21+ (NeoForge 26.x: Java 25)
