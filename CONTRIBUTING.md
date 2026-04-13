# Contributing to RedstoneReboot

This repository builds multiple platform targets from one shared restart engine. Keep changes scoped, tested, and documented.

## Prerequisites

- Java `21+` for a full local build
- Git
- A test server for the platform you touched when behavior changes need runtime verification

## Setup

```bash
git clone https://github.com/sdemonzdevelopment-spec/RedstoneReboot.git
cd RedstoneReboot
./gradlew build
```

Artifacts are written under each module's `build/libs/` directory.

## Repository Layout

```text
RedstoneReboot/
|- common/      Shared restart engine, scheduling, backend, and command logic
|- bukkit/      Bukkit, Spigot, Paper, and compatible plugin entrypoint
|- folia/       Folia plugin entrypoint and scheduler integration
|- fabric/      Fabric server module
|- forge/       Forge server module
|- neoforge/    NeoForge server module
|- wiki/        Repository-backed user documentation
|- docs/api/    Developer integration docs
|- docs/release/ Marketing and release copy
|- docs/marketplace/ Store listing copy
`- assets/      Images and branding assets
```

## Workflow

1. Create a branch for the change.
2. Keep shared logic in `common` unless a platform-specific API is required.
3. Update platform modules only where the platform adapter actually differs.
4. Run `./gradlew build`.
5. Test on a server when the change affects commands, scheduling, alerts, loader startup, or shutdown behavior.

## Code Guidelines

- Keep `common` free of direct platform imports.
- Match existing Java style and package structure.
- Prefer small, explicit changes over broad refactors.
- Use the logger style already present in the module you are editing.
- Update tests when constructor signatures or shared logic change.

## Documentation Expectations

If your change affects users, operators, or integrators:

- update the relevant page in `wiki/`
- update `docs/api/` when the Bukkit integration surface changes
- keep README links and command/config examples aligned with the code

## Pull Requests

Good pull requests usually include:

- what changed
- why it changed
- how it was tested
- any config, command, or migration impact

## Need Help

- Issues: <https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues>
- Discussions: <https://github.com/sdemonzdevelopment-spec/RedstoneReboot/discussions>
- Discord: <https://discord.gg/GYsTt96ypf>
