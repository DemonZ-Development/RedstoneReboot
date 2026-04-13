<div align="center">

<img src="assets/banner.png" alt="RedstoneReboot Banner" width="100%" />

# RedstoneReboot

Multi-platform restart orchestration for Minecraft servers on Bukkit-family platforms, Folia, Fabric, Forge, and NeoForge.

[Releases](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/releases) |
[Build Status](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/actions) |
[Wiki](wiki/README.md) |
[Developer API](docs/api/README.md) |
[Issues](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues) |
[Discord](https://discord.gg/GYsTt96ypf)

</div>

## Overview

RedstoneReboot manages scheduled, manual, and emergency restarts with a shared core that runs across multiple server platforms. It keeps restart timing, monitoring, and alerting in one place, then hands the actual restart off to a backend that matches your environment.

## Key Features

- Scheduled restarts with timezone-aware daily windows
- Countdown warnings and restart status commands
- TPS and memory monitoring with emergency restart thresholds
- Backend handoff for shutdown-only, systemd, Docker, local scripts, and Pterodactyl
- Diagnostics via `/reboot doctor`
- Shared common engine with platform-specific adapters

## Supported Platforms

| Platform | Artifact | Minecraft | Runtime Java |
|----------|----------|-----------|--------------|
| Bukkit / Spigot / Paper / Purpur and compatible forks | `RedstoneReboot-Bukkit-<version>.jar` | `1.9` to `1.21.1` | Java `8+` on legacy servers, Java `17+` on modern servers |
| Folia | `RedstoneReboot-Folia-<version>.jar` | `1.20+` | Java `17+` |
| Fabric | `RedstoneReboot-Fabric-<version>.jar` | `1.20.1+` | Java `17+` |
| Forge | `RedstoneReboot-Forge-<version>.jar` | `1.20.4+` | Java `17+` |
| NeoForge | `RedstoneReboot-NeoForge-<version>.jar` | `1.20.4+` | Java `17+` |

## Installation

### Plugin builds

1. Download the Bukkit-family or Folia jar from [Releases](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/releases).
2. Place it in your server's `plugins/` directory.
3. Start the server once.
4. Edit `plugins/RedstoneReboot/config.yml`.
5. If you want managed restart handoff, also edit `plugins/RedstoneReboot/restart-backends.properties`.

### Mod builds

1. Download the jar for your loader from [Releases](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/releases).
2. Place it in your server's `mods/` directory.
3. Start the server once.
4. Edit the loader-specific config generated for RedstoneReboot.
5. Configure backend handoff if you want restart ownership beyond a normal stop.

Additional install details live in [wiki/Installation.md](wiki/Installation.md).

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/reboot` or `/reboot help` | Show command help | `redstonereboot.use` |
| `/reboot status` | Show restart state and next scheduled restart | `redstonereboot.status` |
| `/reboot info` | Show current server performance data | `redstonereboot.status` |
| `/reboot doctor` | Show backend and environment diagnostics | `redstonereboot.doctor` |
| `/reboot now [delay]` | Start a manual restart countdown | `redstonereboot.restart.now` |
| `/reboot schedule <seconds>` | Schedule a restart in the future | `redstonereboot.restart.schedule` |
| `/reboot cancel` | Cancel the active countdown | `redstonereboot.restart.cancel` |
| `/reboot reload` | Reload config and backend state | `redstonereboot.config.reload` |

## Backend Handoff

RedstoneReboot separates "when should the server restart?" from "who owns the final restart step?".

Supported backends:

- `SHUTDOWN_ONLY`: normal graceful stop, no automatic startup handoff
- `SYSTEMD`: service-managed restarts on Linux hosts
- `DOCKER`: container-aware restart handoff
- `LOCALSCRIPT`: custom script-driven restart flow
- `PTERODACTYL`: panel-owned restart requests

See [wiki/Backends.md](wiki/Backends.md) for backend setup and [wiki/Configuration.md](wiki/Configuration.md) for config keys.

## Developer API

The Bukkit plugin exposes integration points for other plugins. Current documentation is in [docs/api/README.md](docs/api/README.md).

Typical integrations:

- schedule or cancel a restart through `RestartManager`
- inspect restart status and next scheduled restart
- read health-monitor values when monitoring is enabled
- reuse RedstoneReboot's alert and permission helpers

## Build From Source

The full multi-loader build requires Java `21+` because the NeoForge module targets newer toolchains. Runtime support still varies by platform as listed above.

```bash
git clone https://github.com/sdemonzdevelopment-spec/RedstoneReboot.git
cd RedstoneReboot
./gradlew build
```

Build outputs are written under each module's `build/libs/` directory.

## Documentation Layout

Repository-backed wiki content now lives at the repo root in [wiki/](wiki/README.md), so GitHub can render it directly from the main repository.

- [Wiki Index](wiki/README.md)
- [Installation Guide](wiki/Installation.md)
- [Configuration Reference](wiki/Configuration.md)
- [Backend Guide](wiki/Backends.md)
- [Developer API](docs/api/README.md)
- [Contributing](CONTRIBUTING.md)

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
