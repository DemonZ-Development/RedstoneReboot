# Configuration Reference

This page documents the main plugin configuration and the shared backend configuration.

## Main Plugin Config

For Bukkit-family and Folia builds, the main file is:

```text
plugins/RedstoneReboot/config.yml
```

Mod loaders expose equivalent values through their own generated config format.

### General

```yaml
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
  timezone: "Asia/Kolkata"
  days:
    - "ALL"
  warning-time: 300
```

- `times`: `HH:MM` in 24-hour format
- `timezone`: any valid Java `ZoneId`
- `days`: `ALL` or weekday names such as `MONDAY`
- `warning-time`: how early the countdown can begin

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
    default-permission: "redstonereboot.use"
    admin-permission: "redstonereboot.admin"
  fallback:
    use-op-as-admin: true
    default-level: 2
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
  async-operations: true
  thread-pool-size: 2
  metrics-enabled: true
  shutdown-delay-ticks: 60
```

The current shared runtime actively reads `shutdown-delay-ticks` for plugin shutdown timing. The other keys are reserved for broader runtime behavior and future expansion.

## Backend Config

Backend handoff uses a separate file named:

```text
restart-backends.properties
```

Example:

```properties
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

- `lockout-duration-seconds` is used when backend state becomes uncertain and RedstoneReboot temporarily suppresses new restart requests.
- `systemd-service` is used by the `SYSTEMD` backend.
- `localscript-file` is used by the `LOCALSCRIPT` backend.
- Pterodactyl requires the URL, token, and server identifier properties.

If backend behavior is unclear, run `/reboot doctor` and compare the reported backend state with the detected environment.
