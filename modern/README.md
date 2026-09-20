# Minecraft 26.1, 26.2, and 26.3 ports

These are the Minecraft 26.1, 26.2, and 26.3 mod builds of RedstoneReboot 1.6.2. Install only the jar matching your loader and exact Minecraft version. All ports require Java 25.

| Minecraft | Fabric Loader / Fabric API | Forge | NeoForge |
| --- | --- | --- | --- |
| 26.1 | 0.19.3+ / 0.145.1+26.1 | 62.0.9 | 26.1.0.19-beta |
| 26.2 | 0.19.3+ / 0.159.0+26.2 | 65.1.3 | 26.2.0.76 |
| 26.3 | 0.19.5+ / 0.160.6+26.3 | 66.0.2 | 26.3.0.7-beta |

NeoForge's exact 26.1 and 26.3 targets use beta loaders. These version numbers do not imply support for minor versions, snapshots, or later releases.

## Build

Use Gradle 9.7.1 with Java 25. From the repository root:

```powershell
gradle -p modern :common:test :fabric:build :forge:build :neoforge:build '-PminecraftVersion=26.1'
gradle -p modern :common:test :fabric:build :forge:build :neoforge:build '-PminecraftVersion=26.2'
gradle -p modern :common:test :fabric:build :forge:build :neoforge:build '-PminecraftVersion=26.3'
```

Artifacts are output to `modern/<loader>/build/<minecraft-version>/libs/`. The shared engine comes from `common/`; the stable modules keep their existing toolchains and Minecraft targets.
