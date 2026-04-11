# RedstoneReboot Developer API

<div align="center">
<img src="../../assets/logo.png" alt="RedstoneReboot" width="96" />

Current integration surface for Bukkit-side plugins that want to read RedstoneReboot state or trigger restart actions.
</div>

---

## Scope

The currently documented integration path is the Bukkit plugin instance exposed at runtime.

RedstoneReboot currently exposes useful managers and status objects through `RedstoneRebootPlugin`, but this repository does **not** currently ship the custom `RestartEvent` API that older draft docs referenced.

---

## Basic Hook

Add RedstoneReboot as a soft dependency in your `plugin.yml`:

```yaml
softdepend: [RedstoneReboot]
```

Fetch the plugin instance at runtime:

```java
import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("RedstoneReboot")) {
            return;
        }

        RedstoneRebootPlugin reboot = (RedstoneRebootPlugin) getServer()
            .getPluginManager()
            .getPlugin("RedstoneReboot");

        if (reboot != null) {
            getLogger().info("Hooked into RedstoneReboot " + reboot.getDescription().getVersion());
        }
    }
}
```

---

## Restart Manager

`RedstoneRebootPlugin#getRestartManager()` gives access to the shared restart controller.

### Schedule a Restart

```java
import dev.demonz.redstonereboot.common.manager.RestartReason;

RedstoneRebootPlugin reboot = getRebootPlugin();

boolean scheduled = reboot.getRestartManager().scheduleRestart(
    300,
    RestartReason.API,
    "MyPlugin"
);
```

### Cancel a Restart

```java
boolean cancelled = reboot.getRestartManager().cancelRestart();
```

### Read Restart Status

```java
boolean inProgress = reboot.getRestartManager().isRestartInProgress();
int secondsLeft = reboot.getRestartManager().getSecondsUntilRestart();
var nextRestart = reboot.getRestartManager().getNextScheduledRestart();
var info = reboot.getRestartManager().getRestartInfo();
```

### Restart Reasons

Available reasons include:

- `SCHEDULED`
- `SCHEDULED_API`
- `MANUAL`
- `EMERGENCY_TPS`
- `EMERGENCY_MEMORY`
- `API`
- `UNKNOWN`

---

## Server Health Monitor

`RedstoneRebootPlugin#getServerLoadMonitor()` exposes the plugin-side health monitor when monitoring is enabled.

```java
var monitor = reboot.getServerLoadMonitor();

if (monitor != null) {
    double tps = monitor.getLastTPS();
    double memory = monitor.getLastMemoryUsage();
    boolean healthy = monitor.isHealthy();
}
```

---

## Alert Manager

`RedstoneRebootPlugin#getAlertManager()` can be used to send the same player-facing alerts RedstoneReboot uses internally.

```java
import dev.demonz.redstonereboot.common.manager.RestartReason;

var alerts = reboot.getAlertManager();

alerts.sendRestartAlert(60, RestartReason.API);
alerts.sendFinalRestartAlert(RestartReason.API);
alerts.sendRestartCancelledAlert();
alerts.sendEmergencyAlert("Custom emergency condition");
alerts.sendAlert(
    "§cCustom warning message",
    "§cRestart Alert",
    "§eCustom subtitle"
);
```

---

## Permission Manager

`RedstoneRebootPlugin#getPermissionManager()` exposes Bukkit/LuckPerms-aware permission helpers.

```java
var permissions = reboot.getPermissionManager();

boolean canRestartNow = permissions.canRestartNow(player);
boolean canSchedule = permissions.canScheduleRestart(player);
boolean canCancel = permissions.canCancelRestart(player);
boolean canReload = permissions.canReloadConfig(player);
boolean canViewStatus = permissions.canViewStatus(player);
boolean isAdmin = permissions.hasAdminPermission(player);
boolean receivesNotifications = permissions.shouldReceiveNotifications(player);
boolean hasLuckPerms = permissions.isLuckPermsAvailable();
```

---

## Config Access

`RedstoneRebootPlugin#getConfigManager()` exposes the plugin configuration wrapper.

```java
var config = reboot.getConfigManager();

String timezone = config.getTimezone();
boolean monitoringEnabled = config.isMonitoringEnabled();
boolean emergencyEnabled = config.isEmergencyRestartEnabled();
int warningTime = config.getScheduledWarningTime();
```

---

## Notes

- The documented API above is Bukkit-side.
- Plugin deployments use `plugins/RedstoneReboot/config.yml`.
- Fabric, Forge, and NeoForge builds are standalone server modules and do not expose the same Bukkit integration surface.
- If RedstoneReboot is not installed or not enabled, always null-check the plugin instance before using any managers.
