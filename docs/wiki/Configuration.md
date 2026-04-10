# ⚙️ Configuration Reference

Complete reference for `plugins/RedstoneReboot/config.yml`.

---

## General Settings

```yaml
general:
  # Plugin prefix used in all messages (supports § and & color codes)
  plugin-prefix: "§8[§cRedstone§8] §aReboot"

  # Enable verbose debug logging to console
  debug-mode: false

  # Crash plugin on startup if config has invalid values
  strict-validation: true
```

---

## Scheduled Restarts

```yaml
scheduled-restarts:
  enabled: true

  # 24-hour format HH:MM — add as many as needed
  times:
    - "06:00"
    - "12:00"
    - "18:00"
    - "00:00"

  # Java ZoneId — full list: https://docs.oracle.com/javase/8/docs/api/java/time/ZoneId.html
  # Common: Asia/Kolkata, UTC, America/New_York, Europe/London, Asia/Tokyo
  timezone: "Asia/Kolkata"

  # Which days to run scheduled restarts
  # Options: ALL, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
  days:
    - "ALL"

  # How many seconds before restart to begin countdown warnings
  warning-time: 300
```

---

## Alert System

```yaml
alerts:
  enabled: true

  # Seconds before restart when warnings fire
  warning-times: [3600, 1800, 900, 600, 300, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1]

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
```

---

## Server Monitoring

```yaml
monitoring:
  enabled: true
  tps-threshold: 18.0        # Trigger restart if TPS below this
  memory-threshold: 85.0     # Trigger restart if memory above this %
  check-interval: 30         # Seconds between checks
  consecutive-checks: 3      # Bad readings required before action
```

---

## Emergency Restart

```yaml
emergency:
  enabled: true
  tps-threshold: 12.0        # Critical TPS — immediate action
  memory-threshold: 95.0     # Critical memory — immediate action
  delay: 30                  # Seconds of warning before emergency restart
```

---

## Permission Integration

```yaml
permissions:
  luckperms:
    integration-enabled: true
    default-permission: "redstonereboot.use"
    admin-permission: "redstonereboot.admin"
  fallback:
    use-op-as-admin: true
```

---

## PlaceholderAPI

```yaml
placeholders:
  enabled: true
```

All placeholders are automatically registered when PlaceholderAPI is detected. No additional configuration needed.

---

## Advanced

```yaml
advanced:
  async-operations: true
  thread-pool-size: 2
  metrics-enabled: true
  backup-config-on-reload: true
  max-restart-history: 50
```
