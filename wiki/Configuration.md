# Configuration Reference

<div align="center">

![RedstoneReboot Logo](https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/logo.png)

</div>

This page documents the main plugin configuration and the shared backend configuration.

## Main Plugin Config

For Bukkit-family and Folia builds, the main file is:

```text
plugins/RedstoneReboot/config.yml
```

Mod loaders (Fabric, Forge, NeoForge) expose equivalent values through `config/redstonereboot.properties`.

### General

```yaml
config-version: 3

general:
  plugin-prefix: "&8[&cRedstone&8] &aReboot"
  debug-mode: false
  strict-validation: true
```

- `plugin-prefix`: prefix used in command and alert messages
- `debug-mode`: enables extra logging where supported
- `strict-validation`: fails startup on invalid config values

### Scheduled Restarts

```yaml
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
```

- `times`: `HH:MM` in 24-hour format
- `timezone`: any valid Java `ZoneId`
- `days`: `ALL` or weekday names such as `MONDAY`
- `warning-time`: how early the countdown can begin

> **Supported Timezones:** Any valid Java `ZoneId` — e.g. `Europe/London`, `America/New_York`, `Asia/Singapore`, `UTC`, `Australia/Sydney`. Avoid short abbreviations like `IST` or `EST`; use full `Region/City` format. See [Java SE timezone list](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html#of(java.lang.String, java.util.Map)) for all valid IDs.

### Alerts

```yaml
alerts:
  enabled: true
  warning-times: [3600, 1800, 900, 600, 300, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1]

  chat:
    enabled: true
    format: "&8[&cRedstone&8] &eServer will restart in &c{time}&e!"

  title:
    enabled: true
    main-title: "&cServer Restart"
    sub-title: "&ein &c{time}"

  actionbar:
    enabled: true
    format: "&8[&cRedstone&8] &eRestart in: &c{time}"

  sound:
    enabled: true
    sound-name: "BLOCK_NOTE_BLOCK_PLING"
```

### Monitoring

```yaml
monitoring:
  enabled: true
  tps-threshold: 18.0
  memory-threshold: 85.0
  check-interval: 30
  consecutive-checks: 3
```

When enabled, RedstoneReboot samples server health and can trigger restart flows after repeated bad readings.

### Emergency Restart

```yaml
emergency:
  enabled: true
  tps-threshold: 12.0
  memory-threshold: 95.0
  delay: 30
```

### Permissions

```yaml
permissions:
  luckperms:
    integration-enabled: true
  fallback:
    use-op-as-admin: true
    default-level: 2
    public-permissions-enabled: true
```

- `default-level` is primarily relevant for non-Bukkit command environments

### PlaceholderAPI

```yaml
placeholders:
  enabled: true
```

Only Bukkit-family plugin deployments use PlaceholderAPI integration.

### Advanced

```yaml
advanced:
  metrics-enabled: true
  shutdown-delay-ticks: 60
```

- `metrics-enabled`: enables anonymous usage statistics via [bStats](https://bstats.org/plugin/bukkit/RedstoneReboot/30751) (Bukkit builds only). Set to `false` to opt out.
- `shutdown-delay-ticks`: ticks to wait after saving worlds before executing the shutdown sequence.

## Mod Loader Config (redstonereboot.properties)

Mod platforms (Fabric, Forge, NeoForge) use a standard Java `.properties` file located at `config/redstonereboot.properties` instead of `config.yml`. It supports equivalent options mapped as flat key-value pairs:

```properties
# Scheduled Restarts
scheduled-restarts-enabled=false
scheduled-times=06:00,18:00
scheduled-days=ALL
timezone=UTC
warning-time=300
warning-times=300,60,30,10,5,4,3,2,1

# Alerts & Customization
alerts-enabled=true
plugin-prefix=§8[§cRedstone§8] §aReboot
chat-alerts-enabled=true
chat-alert-format=§8[§cRedstone§8] §eServer will restart in §c{time}§e!
title-alerts-enabled=true
title-main-text=§c⚡ Server Restart
title-sub-text=§ein §c{time}
actionbar-alerts-enabled=true
actionbar-format=§8[§cRedstone§8] §eRestart in: §c{time}

# Health Monitoring
monitoring-enabled=false
tps-threshold=18.0
memory-threshold=85.0
check-interval=30
consecutive-checks=3

# Emergency System
emergency-enabled=false
emergency-tps-threshold=12.0
emergency-memory-threshold=95.0
emergency-delay=30

# Advanced Settings
shutdown-delay-ticks=60

# Command Permissions
use-op-as-admin=true
default-permission-level=2
public-permissions-enabled=true
```

### Property Description

- **`scheduled-restarts-enabled`**: Global switch to enable scheduled restarts.
- **`scheduled-times`**: A comma-separated list of 24-hour times (e.g. `06:00,12:00,18:00`).
- **`scheduled-days`**: Comma-separated list of weekdays or `ALL`.
- **`timezone`**: Any valid Java `ZoneId` string (e.g. `Europe/London`, `America/New_York`, or `UTC`).
- **`warning-time`**: Countdown in seconds for the primary restart scheduler warning.
- **`warning-times`**: Comma-separated countdown thresholds (in seconds) to broadcast warnings to players.
- **`alerts-enabled`**: Master toggle for all player-facing broadcast alerts.
- **`plugin-prefix`**: Prefix string applied to broadcast announcements. Color codes can use either `&` or `§`.
- **`chat-alerts-enabled`**: If true, countdown warnings are sent in player chat.
- **`chat-alert-format`**: Chat format template. Supports `{time}` (formatted duration) and `{reason}`.
- **`title-alerts-enabled`**: If true, title and subtitle alerts are displayed on player screens.
- **`title-main-text`**: The main title display text.
- **`title-sub-text`**: The subtitle display text. Supports `{time}`.
- **`actionbar-alerts-enabled`**: If true, displays the warning in the player's action bar.
- **`actionbar-format`**: Action bar message format. Supports `{time}`.
- **`monitoring-enabled`**: Toggle background server performance checking.
- **`tps-threshold` / `memory-threshold`**: Performance metrics limits triggering automated restarts.
- **`check-interval`**: Time in seconds between load monitor checks.
- **`consecutive-checks`**: Number of continuous out-of-bounds checks before initiating a health restart.
- **`emergency-enabled`**: Toggle emergency restart system.
- **`emergency-tps-threshold` / `emergency-memory-threshold`**: Severe limit triggers that bypass standard consecutive checks.
- **`emergency-delay`**: Time in seconds to delay emergency restarts.
- **`shutdown-delay-ticks`**: Delay in ticks after world saves before termination.
- **`use-op-as-admin`**: If true, operator level 4 gives full administrator access.
- **`default-permission-level`**: Minimum OP level required to use command structures.
- **`public-permissions-enabled`**: Enables standard non-op players to use diagnostics/status commands like `/reboot status`.

## Backend Config

Backend handoff uses a separate file named:

```text
restart-backends.properties
```

Example:

```properties
backends-enabled=false
active-backend=SHUTDOWN_ONLY
lockout-duration-seconds=300

ptero-url=
ptero-token=
ptero-id=

systemd-service=minecraft
localscript-file=start.sh
```

### Supported `active-backend` values

- `SHUTDOWN_ONLY`
- `PTERODACTYL`
- `SYSTEMD`
- `DOCKER`
- `LOCALSCRIPT`

### Notes

- **`backends-enabled`**: Must be `true` for backend processing to occur; otherwise defaults to `SHUTDOWN_ONLY`.
- **Environment Variables**: Sensitive properties support dynamic resolution using `${env.VAR_NAME:-fallback}` (e.g., `ptero-token=${env.PTERO_API_KEY}`). This works for variables prefixed with `REBOOT_`, `PTERO_`, `MINECRAFT_`, or `JAVA_`.
- **Supervisor Wiring Requirements**: Supervisor backends (`LOCALSCRIPT`, `SYSTEMD`, and `DOCKER`) require wiring proof so they know they are running under their respective wrappers or supervisors. You **must** pass the system property `-Dredstonereboot.active=true` or set the environment variable `REDSTONEREBOOT_ACTIVE=1` in your container, systemd service unit, or startup script to satisfy this check, otherwise restarts will fail and postpone. (The auto-generated LocalScript wrapper templates export this environment variable automatically).
- `lockout-duration-seconds` is used when backend state becomes uncertain and RedstoneReboot temporarily suppresses new restart requests.
- `systemd-service` is used by the `SYSTEMD` backend.
- `localscript-file` is used by the `LOCALSCRIPT` backend.
- Pterodactyl requires the URL, token, and server identifier properties.

If backend behavior is unclear, run `/reboot doctor` and compare the reported backend state with the detected environment.
