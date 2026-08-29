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

## New in 1.6.0 — Unified Dev API + Discord Message Pipeline

All platforms now expose `RedstoneRebootAPI` (no Bukkit cast needed):

```java
import dev.demonz.redstonereboot.common.api.RedstoneRebootAPI;
import dev.demonz.redstonereboot.common.api.RestartListener;
import dev.demonz.redstonereboot.common.api.MessageContext;
import dev.demonz.redstonereboot.common.manager.RestartReason;

if (!RedstoneRebootAPI.isAvailable()) return;
RedstoneRebootAPI api = RedstoneRebootAPI.getInstance();

// listen to lifecycle
api.registerListener(new RestartListener() {
    @Override public void onRestartScheduled(int seconds, RestartReason reason, String initiator) {
        getLogger().info("restart in " + seconds + "s via " + initiator);
    }
    @Override public void onRestartCancelled(String prevReason, String prevInitiator) {
        getLogger().info("restart cancelled");
    }
});

// fire restarts without touching RestartManager directly
api.scheduleRestart(120, RestartReason.API, "MyPlugin");
api.cancelRestart();
api.getSecondsUntilRestart();
```

### Message API — forward alerts anywhere

```java
import dev.demonz.redstonereboot.common.api.MessageAdapter;
import dev.demonz.redstonereboot.common.api.MessageContext;
import dev.demonz.redstonereboot.common.api.DiscordWebhookAdapter;

// 1) lambda adapter (e.g. your JDA/Dis4J bot)
api.registerMessageAdapter(context -> {
    String plain = context.toPlainText(); // legacy codes stripped
    String platform = context.getPlatformName();
    // yourBot.sendEmbed(platform, plain, context.getReason());
    myDiscordChannel.sendMessage("[" + platform + "] " + plain).queue();
});

// 2) built-in webhook — no bot needed, add to config.yml instead:
// integrations:
//   discord:
//     enabled: true
//     webhook-url: "https://discord.com/api/webhooks/123/abc"
//     username: "RedstoneReboot"
// Or register programmatically:
api.registerDiscordWebhook("https://discord.com/api/webhooks/...", "MyBot");
api.registerMessageAdapter(new DiscordWebhookAdapter(logger, webhookUrl));

// 3) filter if needed
api.registerMessageAdapter(new MessageAdapter() {
    public void onMessage(MessageContext ctx) {
        if (ctx.getType() == MessageContext.Type.FINAL_ALERT) {
            // only final
        }
    }
});
```

`MessageContext.getType()` values: `SCHEDULED_ALERT`, `FINAL_ALERT`, `CANCELLED`, `EMERGENCY`, `POSTPONED`, `GENERIC_CHAT`. Use `LegacyTextUtil.stripLegacyFormatting()` or `ctx.toPlainText()` for Discord-safe text.

### Diagnostic Dump — `DumpGenerator`

```java
import dev.demonz.redstonereboot.common.api.DumpGenerator;

var core = RedstoneRebootAPI.getInstance().getCore();
Path dump = DumpGenerator.generate(core, core.getDataFolder());
// dump-20260829-140533.txt contains sanitized config, backend, env, history, adapters
```

Or in-game: `/reboot dump` (perm `redstonereboot.dump`) creates `plugins/RedstoneReboot/dump-*.txt` and mirrors a preview to Discord adapters if registered. Tokens are masked (`p***ab`, `${env.*}` left intact).

---

## Implementation Notes

- The API is available on **all** platforms (Bukkit/Paper/Folia/Fabric/Forge/NeoForge) via `RedstoneRebootAPI.getInstance()` after `onEnable`.
- Bukkit `RedstoneRebootPlugin#getCore()` / `#getRestartManager()` etc. remain but prefer `RedstoneRebootAPI`.
- Fabric, Forge, NeoForge: obtain `api.getCore().getPlatform()` for platform-agnostic checks.
- Always null-check `RedstoneRebootAPI.isAvailable()` if RedstoneReboot is an optional soft dependency.

---

<div align="center">

**RedstoneReboot** · Multi-Platform Minecraft Server Restart Engine  
*Maintained by [DemonZ Development](https://demonzdevelopment.online)*

</div>
