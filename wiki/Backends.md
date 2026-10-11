# Backend Guide

<div align="center">

<img src="https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/logo.png" alt="RedstoneReboot Logo" width="96" />

</div>

When a server stops, the operating system terminates the process. An external supervisor or script must boot it back up. RedstoneReboot integrates with host supervisors, panels, and loop scripts.

***

## Backend Overview

| Backend | Mechanism | Platforms | Target Environment |
|---|---|---|---|
| `AUTO` | Environment detection | Pumpkin MC & Modern | Chooses `DEPEND_ON_HOST` or `LOCAL_SCRIPT` based on environment variables |
| `DEPEND_ON_HOST` | Server stop | All Platforms | Game hosting panels (Pterodactyl, Bloom, PebbleHost), Docker, Kubernetes |
| `LOCAL_SCRIPT` | Wrapper loop | All Platforms | Standalone servers using `run.bat` (Windows) or `run.sh` (Linux) |
| `SYSTEMD` | Service manager | Java Platforms | Linux services managed by systemd |
| `DOCKER` | Container policy | Java Platforms | Standalone Docker containers (`restart: always`) |
| `PTERODACTYL` | Client API | Java Platforms | Remote Pterodactyl panel instances |

***

## How Each Mode Works

### 1. AUTO Mode
Available in Pumpkin MC and modern editions.

On boot, RedstoneReboot checks for container variables (`PTERODACTYL=1`, `CONTAINER=1`, `DOCKER=1`, `KUBERNETES_SERVICE_HOST`) and container files (`/.dockerenv`).
• Container found: switches to `DEPEND_ON_HOST`.
• Active wrapper found (`REDSTONEREBOOT_ACTIVE=1` or `.redstonereboot_wired`): switches to `LOCAL_SCRIPT`.
• Neither found: switches to `LOCAL_SCRIPT` and writes starter wrapper scripts.

### 2. DEPEND_ON_HOST Mode
Use this for cloud game panels and container systems.

1. RedstoneReboot warns players, saves chunk data, and runs `stop`.
2. The panel daemon (Wings / Pterodactyl) or Docker container restart policy detects the process exit and relaunches Pumpkin.
3. No external scripts needed.

### 3. LOCAL_SCRIPT Mode
Use this for standalone desktop or dedicated servers.

1. You start the server using `run.bat` (Windows) or `run.sh` (Linux).
2. When a reboot triggers via `/reboot now` or a scheduled timer, RedstoneReboot touches `.redstonereboot_restart` and stops Pumpkin.
3. The script detects `.redstonereboot_restart`, deletes it, pauses 5 seconds, and starts the server again.
4. If an operator types `/stop`, no marker exists, and the script exits without restarting.
5. The script checks `%~dp0`, parent directories, and ancestor folders so you can run it from the server root or `plugins/data/RedstoneReboot/`.

### 4. SYSTEMD Mode
Use this for Linux dedicated servers where systemd manages Minecraft.

• On reboot, RedstoneReboot triggers a systemctl restart on your unit.
• Configured with `systemd-service=<unit-name>` in `restart-backends.properties`.

### 5. DOCKER Mode
Use this for Docker instances where container exit codes trigger a restart.

• Halts the server process so Docker container restart policies reboot the container.

### 6. PTERODACTYL Mode
Direct API integration with the Pterodactyl panel.

• Sends restart calls to the Pterodactyl Client API using your API key and server ID.

***

## Pumpkin MC & WebAssembly Sandboxing

Pumpkin sandboxes WASM plugins inside Wasmtime:
• Filesystem access: plugins can only access their data folder (`plugins/data/RedstoneReboot/`).
• Script generation: RedstoneReboot creates `run.bat` and `run.sh` inside `plugins/data/RedstoneReboot/`.
• Script execution: You can run `run.bat` directly from the data folder or copy it to the server root.

***

## Configuration Reference

### Java Platforms (`restart-backends.properties`)
```properties
backends-enabled=false
active-backend=DEPEND_ON_HOST
lockout-duration-seconds=300
ptero-url=
ptero-token=
ptero-id=
systemd-service=minecraft
localscript-file=start.sh
```

### Pumpkin MC (`config.yml`)
```yaml
backend:
  active-backend: "AUTO"
  auto-generate-script: true
  script-name: "run.bat"
  lockout-duration-seconds: 300
```

***

## Status & Diagnostics

Run `/reboot status` or `/reboot doctor` to inspect the backend state:
• `FULL`: Verified with an active wrapper or verified panel controller.
• `GENERATED`: Script exists on disk, but the server was launched without a loop wrapper.
• `DEPEND_ON_HOST`: Host panel or container handles relaunch.
• `ASSISTED`: Partial verification.

***

<div align="center">

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonz.org)*

</div>
