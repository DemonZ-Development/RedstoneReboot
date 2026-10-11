![RedstoneReboot](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/banner.png)

# RedstoneReboot for Pumpkin MC

**Scheduled restarts, countdown alerts, and restart management for Pumpkin Minecraft servers.**

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

### LOCAL_SCRIPT Mode (Standalone Desktop & Dedicated Servers)
Operating systems exit halted applications by default. On standalone machines without a panel supervisor, automated rebooting requires a loop wrapper script:
• RedstoneReboot automatically generates `run.bat` (Windows) and `run.sh` (Linux) in your server root on first startup.
• Always start your server using `run.bat` (Windows) or `run.sh` (Linux) rather than double clicking the raw `pumpkin.exe` binary.
• When a scheduled reboot or `/reboot now` occurs, RedstoneReboot places a `.redstonereboot_restart` marker file and halts Pumpkin.
• The script detects the marker, clears it, counts down 5 seconds, and automatically launches Pumpkin again.
• When an administrator stops the server using `/stop`, no marker is created, and the script exits normally without restarting.

***

## How the run.bat / run.sh Wrapper Script Works

When running on standalone servers in `LOCAL_SCRIPT` mode, RedstoneReboot creates and uses wrapper scripts to achieve automated restarts:

### Windows (`run.bat`)
```bat
@echo off
title Pumpkin Server Wrapper
set REDSTONEREBOOT_ACTIVE=1

if exist "%~dp0pumpkin-X64-Windows.exe" cd /d "%~dp0"
if exist "%~dp0pumpkin.exe" cd /d "%~dp0"

set PUMPKIN_EXE=
if exist "pumpkin-X64-Windows.exe" set PUMPKIN_EXE=pumpkin-X64-Windows.exe
if exist "pumpkin.exe" set PUMPKIN_EXE=pumpkin.exe

:start
echo Starting Pumpkin Server...
%PUMPKIN_EXE%

if not exist ".redstonereboot_restart" goto end
del /f /q ".redstonereboot_restart" >nul 2>&1
echo Server stopped by RedstoneReboot. Restarting in 5 seconds... (Press Ctrl+C to cancel)
timeout /t 5
goto start

:end
echo Server shut down normally. Exiting.
pause
exit /b 0
```

### Linux (`run.sh`)
```bash
#!/bin/bash
export REDSTONEREBOOT_ACTIVE=1
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

PUMPKIN_BIN=""
if [ -f "./pumpkin-X64-Linux" ]; then
    PUMPKIN_BIN="./pumpkin-X64-Linux"
elif [ -f "./pumpkin" ]; then
    PUMPKIN_BIN="./pumpkin"
fi

while true; do
    echo "Starting Pumpkin Server..."
    $PUMPKIN_BIN
    if [ ! -f ".redstonereboot_restart" ]; then
        echo "Server shut down normally. Exiting."
        exit 0
    fi
    rm -f ".redstonereboot_restart" 2>/dev/null
    echo "Server stopped by RedstoneReboot. Restarting in 5 seconds... (Press Ctrl+C to cancel)"
    sleep 5
done
```

### Key Behaviors of the Script Loop:
• **Restart vs Stop Detection**: A scheduled reboot or `/reboot now` command creates `.redstonereboot_restart`. The script sees this marker, removes it, waits 5 seconds, and restarts. If `/stop` is used, no marker exists, and the wrapper exits cleanly.  
• **Path Resolution**: The script automatically finds the Pumpkin executable in current and parent directories.  
• **Safety Timeout**: A 5 second cooldown prevents boot loops if the server stops unexpectedly.  

***

## Installation & Setup

### Requirements
• Pumpkin server binary (`pumpkin-X64-Windows.exe`, `pumpkin-X64-Linux`, or source build)  
• Plugin file (`redstonereboot_pumpkin.wasm`)  

### Step 1: Install Plugin
1. Place `redstonereboot_pumpkin.wasm` in your server's `plugins/` directory.
2. Launch the server once so Pumpkin loads the plugin.
3. Type `y` in the server console when prompted to grant plugin permissions (`fs.read.data`, `fs.write.data`, `sys.env`). Pumpkin records this approval in `plugins/permission_cache.json`.

### Step 2: Choose Launch Method
• **Game Panels and Docker Containers**: Start your server normally through the panel interface. RedstoneReboot detects the environment and uses `DEPEND_ON_HOST`.  
• **Standalone Servers**: Launch your server with `run.bat` (Windows) or `run.sh` (Linux) to enable automated looping in `LOCAL_SCRIPT` mode.

### Step 3: Configure
Edit `plugins/data/RedstoneReboot/config.yml` to set restart schedules, warning countdown intervals, timezone, and sound cues. Reload changes anytime using `/reboot reload`.

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

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonz.org)*
