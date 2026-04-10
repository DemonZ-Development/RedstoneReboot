# 🛠️ RedstoneReboot Developer API

<div align="center">
<img src="../../assets/logo.png" alt="RedstoneReboot" width="96" />

**Build powerful integrations with RedstoneReboot's comprehensive API**
</div>

---

## 📦 Adding as a Dependency

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.DemonZDevelopment</groupId>
    <artifactId>RedstoneReboot</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle (Groovy)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.DemonZDevelopment:RedstoneReboot:1.0.0'
}
```

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.DemonZDevelopment:RedstoneReboot:1.0.0")
}
```

---

## 🔌 Getting the API Instance

```java
import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;

public class MyPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        // Check if RedstoneReboot is installed
        if (getServer().getPluginManager().isPluginEnabled("RedstoneReboot")) {
            RedstoneRebootPlugin reboot = (RedstoneRebootPlugin) 
                getServer().getPluginManager().getPlugin("RedstoneReboot");
            
            getLogger().info("Hooked into RedstoneReboot v" + 
                reboot.getDescription().getVersion());
        }
    }
}
```

> **Important**: Add `RedstoneReboot` to your `plugin.yml` as a `softdepend`:
> ```yaml
> softdepend: [RedstoneReboot]
> ```

---

## 📡 Events

### RestartEvent

Fired when a restart is initiated. **Cancellable**.

```java
import dev.demonz.redstonereboot.bukkit.events.RestartEvent;

@EventHandler
public void onRestart(RestartEvent event) {
    // Get restart details
    RestartReason reason = event.getReason();
    String initiator = event.getInitiator();
    int delay = event.getDelaySeconds();
    
    getLogger().info("Restart triggered by " + initiator + 
        " for reason: " + reason.getDisplayName() + 
        " in " + delay + " seconds");
    
    // Cancel if needed
    if (someCondition) {
        event.setCancelled(true, "MyPlugin: Not now!");
    }
}
```

### RestartReason Enum

| Value | Display Name | Trigger |
|-------|-------------|---------|
| `SCHEDULED` | Scheduled Restart | Automatic time-based restart |
| `MANUAL` | Manual Restart | `/reboot now` or `/reboot schedule` |
| `EMERGENCY_TPS` | Emergency - Low TPS | TPS below emergency threshold |
| `EMERGENCY_MEMORY` | Emergency - High Memory | Memory above emergency threshold |
| `API` | API Restart | Triggered by another plugin via API |
| `UNKNOWN` | Unknown | Fallback reason |

---

## 🔁 Programmatic Restart Control

### Schedule a Restart
```java
RedstoneRebootPlugin plugin = getRebootPlugin();

// Schedule restart in 300 seconds
plugin.getRestartManager().scheduleRestart(
    300,                              // delay in seconds
    RestartManager.RestartReason.API,  // reason
    "MyPlugin"                        // initiator name
);
```

### Cancel a Restart
```java
boolean cancelled = plugin.getRestartManager().cancelRestart();
if (cancelled) {
    getLogger().info("Successfully cancelled the pending restart");
}
```

### Check Restart Status
```java
// Is a restart currently counting down?
boolean inProgress = plugin.getRestartManager().isRestartInProgress();

// When is the next scheduled restart?
LocalDateTime nextRestart = plugin.getRestartManager().getNextScheduledRestart();

// Get full restart info map
Map<String, Object> info = plugin.getRestartManager().getRestartInfo();
```

---

## 📊 Performance Monitoring API

```java
ServerLoadMonitor monitor = plugin.getServerLoadMonitor();

if (monitor != null) {
    // Current TPS (capped at 20.0)
    double tps = monitor.getLastTPS();
    
    // Memory usage as percentage
    double memoryPercent = monitor.getLastMemoryUsage();
    
    // Overall health check
    boolean healthy = monitor.isHealthy();
    
    getLogger().info(String.format(
        "Server Health — TPS: %.1f | Memory: %.1f%% | Healthy: %s",
        tps, memoryPercent, healthy ? "YES" : "NO"
    ));
}
```

---

## 🔔 Alert API

```java
AlertManager alerts = plugin.getAlertManager();

// Send a custom restart alert
alerts.sendRestartAlert(60, RestartManager.RestartReason.API);

// Send an emergency alert
alerts.sendEmergencyAlert("Custom emergency: database offline!");

// Send cancellation notice
alerts.sendRestartCancelledAlert();
```

---

## 🔐 Permission API

```java
PermissionManager perms = plugin.getPermissionManager();

// Check specific permissions
boolean canRestart = perms.canRestartNow(player);
boolean isAdmin = perms.hasAdminPermission(player);
boolean getsNotified = perms.shouldReceiveNotifications(player);

// LuckPerms availability
boolean hasLuckPerms = perms.isLuckPermsAvailable();
```

---

## 💡 Full Example Plugin

```java
package com.example.myintegration;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.events.RestartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MyIntegration extends JavaPlugin implements Listener {
    private RedstoneRebootPlugin reboot;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("RedstoneReboot")) {
            reboot = (RedstoneRebootPlugin) getServer().getPluginManager()
                .getPlugin("RedstoneReboot");
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("RedstoneReboot integration active!");
        }
    }

    @EventHandler
    public void onRestart(RestartEvent event) {
        // Protect players in dungeons
        if (isDungeonActive()) {
            event.setCancelled(true, "MyIntegration: Dungeon in progress");
            getLogger().info("Blocked restart — dungeon is active");
        }
    }

    private boolean isDungeonActive() {
        return false; // Your logic here
    }
}
```

---

<div align="center">

**Need help?** Open an issue on [GitHub](https://github.com/sdemonzdevelopment-spec/RedstoneReboot/issues) or join our [Discord](https://discord.gg/GYsTt96ypf).

</div>
