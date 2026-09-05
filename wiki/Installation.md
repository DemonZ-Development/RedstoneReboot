# Installation Guide

<div align="center">

<img src="https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/logo.png" alt="RedstoneReboot Logo" width="96" />

</div>

Use the build that matches your server platform. Do not mix Bukkit, Folia, Fabric, Forge, or NeoForge artifacts.

## Platform Matrix

| Platform | Artifact | Minecraft | Runtime Java |
|----------|----------|-----------|--------------|
| Bukkit / Spigot / Paper / Purpur and compatible forks | `RedstoneReboot-Bukkit-<version>.jar` | API `1.13+`; maintainer-tested on `26.1` and `26.2` | Java `17` minimum; follow the server requirement |
| Folia | `RedstoneReboot-Folia-<version>.jar` | Use the dedicated artifact and verify your Folia build | Follow the server requirement; minimum Java `17` |
| Fabric | `RedstoneReboot-Fabric-<version>.jar` | `1.20.1` | Java `17+`, Fabric API required |
| Forge | `RedstoneReboot-Forge-<version>.jar` | `1.20.4`, Forge `49.x` | Java `17+` |
| NeoForge | `RedstoneReboot-NeoForge-<version>.jar` | `1.21.1`, NeoForge `21.1.x` | Java `21+` |

The 26.1 and 26.2 mod ports are separate development artifacts with `mc26.1` or `mc26.2` in their filenames. They require Java 25. See [the port build instructions](../modern/README.md); do not install the stable mod jars on 26.x.

## Bukkit, Spigot, Paper, and Similar Servers

1. Download the Bukkit-family jar from the release page.
2. Stop the server.
3. Place the jar in `plugins/`.
4. Start the server once so RedstoneReboot can generate its files.
5. Edit `plugins/RedstoneReboot/config.yml`.
6. If you want managed restart handoff, edit `plugins/RedstoneReboot/restart-backends.properties`.
7. Run `/reboot status`.
8. Run `/reboot doctor` if you are configuring a backend.

## Folia

1. Download the Folia jar.
2. Place it in `plugins/`.
3. Start the server once.
4. Edit `plugins/RedstoneReboot/config.yml`.
5. Edit `plugins/RedstoneReboot/restart-backends.properties` if you need backend handoff.
6. Run `/reboot status` and `/reboot doctor`.

Folia uses its own scheduler adapter internally. You do not need a separate Folia-specific config file.

## Fabric

1. Download the Fabric jar.
2. Place it in `mods/`.
3. Install Fabric API for the same Minecraft version.
4. Start the server once.
5. Edit the loader-generated RedstoneReboot config.
6. Configure backend handoff if you want restart ownership beyond a normal stop.

## Forge

1. Download the Forge jar.
2. Place it in `mods/`.
3. Start the server once.
4. Edit the loader-generated RedstoneReboot config.
5. Configure backend handoff if needed.

## NeoForge

1. Download the NeoForge jar.
2. Place it in `mods/`.
3. Start the server once.
4. Edit the loader-generated RedstoneReboot config.
5. Configure backend handoff if needed.

## First Startup Checklist

- the platform-specific RedstoneReboot config file exists
- the backend config file exists if you plan to use restart handoff
- `/reboot status` reports the expected timezone and schedule
- `/reboot doctor` shows the backend you intended to use

## Common Mistakes

### Wrong artifact for the server

Use the Bukkit-family jar for Bukkit-compatible servers, the Folia jar for Folia, and the loader-specific mod jar for Fabric, Forge, or NeoForge.

### Commands missing

- On plugin builds, verify the plugin enabled cleanly and `plugin.yml` registered the command.
- On mod builds, verify the dedicated server finished startup and the loader registered the command tree.

### Restart backend not taking ownership

Run `/reboot doctor` and compare the active backend with the environment RedstoneReboot detected. Backend state and mismatch warnings are explained in [Backends.md](Backends.md).

---

<div align="center">

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonzdevelopment.online)*

</div>
