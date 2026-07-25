# RedstoneReboot Developer API

<div align="center">

<img src="https://raw.githubusercontent.com/DemonZ-Development/RedstoneReboot/main/assets/logo.png" alt="RedstoneReboot Logo" width="96" />

</div>

Integration guide for Bukkit-side plugins that need to interact with RedstoneReboot state or trigger restart actions.

---

## Scope

The documented integration path accesses the `RedstoneRebootPlugin` instance exposed at runtime on Bukkit, Paper, and Folia servers.

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

`RedstoneRebootPlugin#getRestartManager()` provides access to the shared restart controller.

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

`RedstoneRebootPlugin#getServerLoadMonitor()` exposes the health monitor when monitoring is enabled.

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

`RedstoneRebootPlugin#getAlertManager()` can be used to send player-facing notifications.

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

`RedstoneRebootPlugin#getPermissionManager()` exposes permission check helpers.

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

## Implementation Notes

- The API is available on Bukkit, Paper, and Folia server runtimes.
- Fabric, Forge, and NeoForge builds operate as standalone server mod modules.
- Always null-check the plugin instance before accessing managers if RedstoneReboot is an optional soft dependency.

---

<div align="center">

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonzdevelopment.online)*

</div>
