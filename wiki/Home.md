# RedstoneReboot Wiki Home

<div align="center">
<img src="../assets/logo.png" alt="RedstoneReboot" width="128" />

Repository-backed documentation for installing, configuring, and operating RedstoneReboot.
</div>

---

## Start Here

- [Wiki Index](README.md)
- [Installation](Installation.md)
- [Configuration](Configuration.md)
- [Backends](Backends.md)
- [Permissions](Permissions.md)
- [Placeholders](Placeholders.md)
- [FAQ](FAQ.md)
- [Developer API](../docs/api/README.md)

## What RedstoneReboot Covers

- Scheduled restarts with timezone and weekday rules
- Manual and emergency restart flows
- Countdown alerts and status commands
- Restart backend handoff
- Cross-platform support for plugin and mod environments

## Platform Summary

| Platform | Type | Primary Config |
|----------|------|----------------|
| Bukkit / Spigot / Paper / Purpur | Plugin | `plugins/RedstoneReboot/config.yml` |
| Folia | Plugin | `plugins/RedstoneReboot/config.yml` |
| Fabric | Mod | `config/redstonereboot.properties` |
| Forge | Mod | `config/redstonereboot.properties` |
| NeoForge | Mod | `config/redstonereboot.properties` |

Backend handoff is configured separately through `restart-backends.properties`.

## Integrations

- **PlaceholderAPI**: 8 placeholders for scoreboards, tab lists, and MOTD — see [Placeholders](Placeholders.md)
- **LuckPerms**: Full permission resolution — see [Permissions](Permissions.md)
- **bStats**: Anonymous usage metrics at [bstats.org/plugin/bukkit/RedstoneReboot/30751](https://bstats.org/plugin/bukkit/RedstoneReboot/30751)

## Repository Docs

- [Root README](../README.md)
- [Contributing Guide](../CONTRIBUTING.md)
- [Marketplace Copy](../docs/marketplace/SPIGOT.md)
- [Release Copy](../docs/release/DISCORD.md)

## Support

- Issues: <https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues>
- Discussions: <https://github.com/sdemonzdevelopment-spec/RedstoneReboot/discussions>
- Discord: <https://discord.gg/GYsTt96ypf>
