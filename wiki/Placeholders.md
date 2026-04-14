# PlaceholderAPI Placeholders

RedstoneReboot provides a PlaceholderAPI expansion for Bukkit-family and Folia servers.

## Requirements

- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) installed on the server
- `placeholders.enabled: true` in `config.yml` (enabled by default)

## Available Placeholders

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%redstonereboot_next_restart%` | Next scheduled restart date and time | `2026-04-15 06:00:00 Asia/Kolkata` |
| `%redstonereboot_time_until%` | Time remaining until next restart | `2h 30m` |
| `%redstonereboot_status%` | Current server restart status | `Normal operation` or `Restart in progress` |
| `%redstonereboot_reason%` | Reason for the current restart | `Scheduled Restart` or `None` |
| `%redstonereboot_tps%` | Last recorded server TPS | `19.8` |
| `%redstonereboot_memory%` | Current memory usage percentage | `62.4%` |
| `%redstonereboot_version%` | Installed RedstoneReboot version | `1.3.2` |
| `%redstonereboot_timezone%` | Configured timezone | `Asia/Kolkata` |

## Usage Examples

### Scoreboard
```
&7Next Restart: &e%redstonereboot_time_until%
&7TPS: &a%redstonereboot_tps%
&7Memory: &c%redstonereboot_memory%
```

### Tab List
```
&8Server Status: %redstonereboot_status%
```

### MOTD
```
&7Next restart in %redstonereboot_time_until%
```

> **Note:** MOTD compatibility requires RedstoneReboot v1.3.2+ which includes null-safety fixes for server-list pings.

## Notes

- Placeholders that depend on the load monitor (`tps`, `memory`) return default values (`20.0`, `0.0%`) when monitoring is disabled.
- The `time_until` placeholder returns `N/A` when no restart is scheduled and `Soon` when the restart is imminent.
- The `reason` placeholder returns `None` when no restart is in progress.
- PlaceholderAPI integration is only available on Bukkit-family and Folia servers. Mod platforms (Fabric, Forge, NeoForge) do not support PlaceholderAPI.
