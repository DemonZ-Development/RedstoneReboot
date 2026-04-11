package dev.demonz.redstonereboot.common.platform;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.monitor.PlatformLoadMonitor;
import dev.demonz.redstonereboot.common.scheduler.JavaPlatformScheduler;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

/**
 * Shared bootstrap for loader-based platforms that do not yet have a native
 * scheduler and command bridge like the Bukkit implementation.
 */
public abstract class AbstractBootstrapServerPlatform implements ServerPlatform {

    private final Logger logger;
    private final String platformName;
    private final String minecraftVersion;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private PlatformTaskScheduler scheduler;
    private PlatformLoadMonitor loadMonitor;
    protected volatile RedstoneRebootCore core;

    protected AbstractBootstrapServerPlatform(Logger logger, String platformName, String minecraftVersion) {
        this.logger = logger;
        this.platformName = platformName;
        this.minecraftVersion = minecraftVersion;
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected final void startCore(PlatformTaskScheduler scheduler, PlatformConfig config) {
        if (started.compareAndSet(false, true)) {
            this.scheduler = scheduler;
            core = new RedstoneRebootCore(this, scheduler, config);
        }
    }

    protected final SimplePlatformConfig loadSimpleConfig(Path configPath) {
        SimplePlatformConfig config = new SimplePlatformConfig();

        try {
            if (!Files.exists(configPath)) {
                createDefaultConfig(configPath);
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            }

            List<String> scheduledTimes = splitCsv(props.getProperty("scheduled-times", ""));
            if (!scheduledTimes.isEmpty()) {
                config.setScheduledTimes(scheduledTimes);
            }

            List<String> scheduledDays = splitCsv(props.getProperty("scheduled-days", ""));
            if (!scheduledDays.isEmpty()) {
                config.setScheduledDays(scheduledDays);
            }

            List<Integer> warningTimes = splitIntegerCsv(props.getProperty("warning-times", ""));
            if (!warningTimes.isEmpty()) {
                config.setWarningTimes(warningTimes.stream()
                    .filter(value -> value > 0)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList());
            }

            config.setTimezone(props.getProperty("timezone", config.getTimezone()).trim());

            applyBoolean(props, "scheduled-restarts-enabled", config::setScheduledRestartsEnabled);
            applyInteger(props, "warning-time", config::setScheduledWarningTime);
            applyBoolean(props, "alerts-enabled", config::setAlertsEnabled);

            applyBoolean(props, "monitoring-enabled", config::setMonitoringEnabled);
            applyDouble(props, "tps-threshold", config::setTpsThreshold);
            applyDouble(props, "memory-threshold", config::setMemoryThreshold);
            applyInteger(props, "check-interval", config::setCheckInterval);
            applyInteger(props, "consecutive-checks", config::setConsecutiveChecks);

            applyBoolean(props, "emergency-enabled", config::setEmergencyRestartEnabled);
            applyDouble(props, "emergency-tps-threshold", config::setEmergencyTpsThreshold);
            applyDouble(props, "emergency-memory-threshold", config::setEmergencyMemoryThreshold);
            applyInteger(props, "emergency-delay", config::setEmergencyDelay);

            applyInteger(props, "shutdown-delay-ticks", config::setShutdownDelayTicks);
            applyBoolean(props, "use-op-as-admin", config::setUseOpAsAdminEnabled);
            applyInteger(props, "default-permission-level", config::setDefaultPermissionLevel);
        } catch (IOException exception) {
            logger.warning("Failed to load config: " + exception.getMessage());
        }

        return config;
    }

    protected final void startPlatformMonitoring() {
        stopPlatformMonitoring();
        if (core == null || scheduler == null) {
            return;
        }

        PlatformConfig config = core.getConfig();
        if (!config.isMonitoringEnabled() && !config.isEmergencyRestartEnabled()) {
            return;
        }

        loadMonitor = new PlatformLoadMonitor(logger, this, scheduler, config, core.getRestartManager());
        loadMonitor.startMonitoring();
    }

    protected final void stopPlatformMonitoring() {
        if (loadMonitor != null) {
            loadMonitor.stopMonitoring();
            loadMonitor = null;
        }
    }

    protected final void stopCore() {
        if (started.compareAndSet(true, false)) {
            stopPlatformMonitoring();

            if (core != null) {
                core.onDisable();
                core = null;
            }
            if (scheduler instanceof JavaPlatformScheduler javaScheduler) {
                javaScheduler.shutdown();
                logger.info("Platform scheduler shut down successfully.");
            }
            scheduler = null;
        }
    }

    protected final void registerShutdownHook(String threadName) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopCore, threadName));
    }

    @Override
    public void broadcastMessage(String message) {
        logger.info("[broadcast] " + LegacyTextUtil.stripLegacyFormatting(message));
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        logger.info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
            + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
    }

    @Override
    public void executeConsole(String command) {
        logger.warning("Console execution requested on " + platformName
            + " but no command bridge is available yet: " + command);
    }

    @Override
    public double getTPS() {
        return 20.0;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    private void createDefaultConfig(Path configPath) throws IOException {
        Path parent = configPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Properties props = new Properties();
        props.setProperty("scheduled-restarts-enabled", "false");
        props.setProperty("scheduled-times", "06:00,18:00");
        props.setProperty("scheduled-days", "ALL");
        props.setProperty("timezone", "UTC");
        props.setProperty("warning-time", "300");
        props.setProperty("warning-times", "300,60,30,10,5,4,3,2,1");
        props.setProperty("alerts-enabled", "true");
        props.setProperty("monitoring-enabled", "false");
        props.setProperty("tps-threshold", "18.0");
        props.setProperty("memory-threshold", "85.0");
        props.setProperty("check-interval", "30");
        props.setProperty("consecutive-checks", "3");
        props.setProperty("emergency-enabled", "false");
        props.setProperty("emergency-tps-threshold", "12.0");
        props.setProperty("emergency-memory-threshold", "95.0");
        props.setProperty("emergency-delay", "30");
        props.setProperty("shutdown-delay-ticks", "60");
        props.setProperty("use-op-as-admin", "true");
        props.setProperty("default-permission-level", "2");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            props.store(out, "RedstoneReboot Configuration");
        }
    }

    private void applyBoolean(Properties props, String key, Consumer<Boolean> setter) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return;
        }
        setter.accept(Boolean.parseBoolean(raw.trim()));
    }

    private void applyInteger(Properties props, String key, IntConsumer setter) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return;
        }

        try {
            setter.accept(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException exception) {
            logger.warning("Ignoring invalid integer for '" + key + "': " + raw);
        }
    }

    private void applyDouble(Properties props, String key, DoubleConsumer setter) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return;
        }

        try {
            setter.accept(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException exception) {
            logger.warning("Ignoring invalid decimal for '" + key + "': " + raw);
        }
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .toList();
    }

    private List<Integer> splitIntegerCsv(String value) {
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .map(entry -> {
                try {
                    return Integer.parseInt(entry);
                } catch (NumberFormatException exception) {
                    logger.warning("Ignoring invalid integer in list: " + entry);
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .toList();
    }
}
