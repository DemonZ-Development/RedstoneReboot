<div align="center">

![RedstoneReboot](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot for Pumpkin MC

**Scheduled restarts, countdown alerts, and restart management for Pumpkin Minecraft servers.**

</div>

***

## Overview

RedstoneReboot schedules restarts, alerts players before downtime, and saves world data on Pumpkin servers. The plugin compiles to WebAssembly (`wasm32-wasip2`) and works with Docker containers, game control panels, and standalone machines.

***

## Features

• Schedules: Set reboot times by hour, timezone, and day of the week.  
• Alerts: Send countdown warnings via chat, titles, action bar, and note block sounds.  
• Backends: Connect to Docker containers, Pterodactyl panels, or local loop scripts.  
• Loop scripts: Generates `run.bat` and `run.sh` so your server boots after shutdown.  
• Data safety: Saves chunks and player data before stopping Pumpkin.  
• Marker check: Differentiates `/reboot` commands from `/stop` so manual shutdowns stay off.  
• Commands: Manage reboots with `/reboot now`, `/reboot cancel`, `/reboot status`, and `/reboot history`.  
• Logging: Writes restart timestamps and trigger reasons to `logs/redstonereboot/restarts.log`.  

***

## Backend Modes

Operating systems terminate stopped server processes. A supervisor or loop script must restart the server.

### AUTO Mode (Default)
RedstoneReboot inspects the environment on boot:
• Container or panel detected: selects `DEPEND_ON_HOST`.
• Standalone machine detected: selects `LOCAL_SCRIPT` and writes starter scripts.

### DEPEND_ON_HOST Mode
Use this for game hosting panels and containers (Docker, Pterodactyl, Bloom, PebbleHost):
• RedstoneReboot saves chunks, warns players, and stops Pumpkin.
• The panel daemon or Docker restart policy boots the server back up.
• Requires no extra scripts.

### LOCAL_SCRIPT Mode
Use this for standalone desktop or dedicated servers:
• RedstoneReboot writes `run.bat` (Windows) and `run.sh` (Linux) to `plugins/data/RedstoneReboot/`.
• A reboot command touches `.redstonereboot_restart` and halts Pumpkin.
• The script finds the marker, deletes it, waits 5 seconds, and launches the server.
• When an operator types `/stop`, no marker exists, and the script exits.

***

## Installation

### Requirements
• Pumpkin server binary (`pumpkin-X64-Windows.exe`, `pumpkin-X64-Linux`, or source build)  
• Plugin file (`redstonereboot_pumpkin.wasm`)  

### 1. Install
1. Put `redstonereboot_pumpkin.wasm` in your `plugins/` directory.
2. Start the server.
3. Type `y` in console when Pumpkin asks to approve permissions (`fs.read.data`, `fs.write.data`, `sys.env`). Pumpkin saves this choice to `plugins/permission_cache.json`.

### 2. Launch
• Panels and Docker: start your server through the panel interface.
• Standalone servers: run `run.bat` (Windows) or `run.sh` (Linux) from `plugins/data/RedstoneReboot/` or your server root.

### 3. Configure
Edit `plugins/data/RedstoneReboot/config.yml` to change schedules, alerts, sounds, and timezone. Apply edits with `/reboot reload`.

***

## Commands & Permissions

| Command | Permission | Description |
|---|---|---|
| `/reboot` | `redstonereboot.use` | Overview and active status |
| `/reboot status` | `redstonereboot.use` | Check next scheduled restart and active backend |
| `/reboot now [delay]` | `redstonereboot.use` | Trigger an immediate or delayed restart countdown |
| `/reboot schedule <seconds>` | `redstonereboot.use` | Schedule a countdown after specified seconds |
| `/reboot cancel` | `redstonereboot.use` | Cancel an active reboot countdown |
| `/reboot history` | `redstonereboot.use` | View recent server restarts and reasons |
| `/reboot reload` | `redstonereboot.use` | Reload configuration from disk without restarting |
| `/reboot help` | `redstonereboot.use` | Display available command syntax |

***

## Configuration (`config.yml`)

```yaml
general:
  plugin-prefix: "§8[§cRedstone§8] §aReboot"
  debug-mode: false
  strict-validation: true

scheduled-restarts:
  enabled: true
  times:
    - "06:00"
    - "12:00"
    - "18:00"
    - "00:00"
  timezone: "Europe/London"
  days:
    - "ALL"
  warning-time: 300

alerts:
  enabled: true
  warning-times:
    - 3600
    - 1800
    - 900
    - 600
    - 300
    - 180
    - 120
    - 60
    - 30
    - 15
    - 10
    - 5
    - 4
    - 3
    - 2
    - 1
  chat:
    enabled: true
    format: "§8[§cRedstone§8] §eServer will restart in §c{time}§e!"
  title:
    enabled: true
    main-title: "§c⚡ Server Restart"
    sub-title: "§ein §c{time}"
  actionbar:
    enabled: true
    format: "§8[§cRedstone§8] §eRestart in: §c{time}"
  sound:
    enabled: true
    sound-name: "BLOCK_NOTE_BLOCK_PLING"

advanced:
  history-file: "logs/redstonereboot/restarts.log"
  shutdown-delay-ticks: 60

backend:
  active-backend: "AUTO"
  auto-generate-script: true
  script-name: "run.bat"
  lockout-duration-seconds: 300

config-version: 4
```

***

<div align="center">

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonz.org)*

</div>
