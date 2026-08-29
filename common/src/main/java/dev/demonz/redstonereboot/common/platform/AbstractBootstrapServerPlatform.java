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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.logging.Logger;

public abstract class AbstractBootstrapServerPlatform implements ServerPlatform {

    public static final int CURRENT_MOD_CONFIG_VERSION = 2;

    private final Logger logger;
    private final String platformName;
    private final String minecraftVersion;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile PlatformTaskScheduler scheduler;
    private volatile PlatformLoadMonitor loadMonitor;
    private volatile Path runtimeConfigPath;
    private volatile SimplePlatformConfig mutableConfig;
    protected volatile RedstoneRebootCore core;

    protected AbstractBootstrapServerPlatform(Logger logger, String platformName, String minecraftVersion) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.platformName = Objects.requireNonNull(platformName, "platformName must not be null");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion must not be null");
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected final void startCore(PlatformTaskScheduler scheduler, PlatformConfig config, Path dataFolder) {
        if (started.compareAndSet(false, true)) {
            this.scheduler = scheduler;
            core = new RedstoneRebootCore(this, scheduler, config, dataFolder);
            if (config instanceof SimplePlatformConfig simpleConfig) {
                this.mutableConfig = simpleConfig;
            }
        }
    }

    protected final SimplePlatformConfig loadSimpleConfig(Path configPath) {
        this.runtimeConfigPath = configPath;
        SimplePlatformConfig config = new SimplePlatformConfig();

        try {
            if (!Files.exists(configPath)) {
                Path parent = configPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Path legacyPath = parent != null && parent.getParent() != null
                    ? parent.getParent().resolve(configPath.getFileName())
                    : null;
                if (legacyPath != null && Files.exists(legacyPath)) {
                    try {
                        Files.copy(legacyPath, configPath);
                    } catch (Exception ignored) {
                        createDefaultConfig(configPath);
                    }
                } else {
                    createDefaultConfig(configPath);
                }
            }

            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
            } catch (IllegalArgumentException exception) {
                logger.warning("Malformed .properties file at " + configPath + ": " + exception.getMessage() + ". Using defaults.");
                return config;
            }

            int fileVersion = parseConfigVersion(props.getProperty("config-version", "1"));
            if (fileVersion < CURRENT_MOD_CONFIG_VERSION) {
                try {
                    migrateModConfig(props, fileVersion, configPath);
                } catch (Exception migrateEx) {
                    logger.warning("Failed to migrate mod config from v" + fileVersion + ": " + migrateEx.getMessage());
                }
            }

            List<String> scheduledTimes = splitCsv(props.getProperty("scheduled-times", ""));
            if (!scheduledTimes.isEmpty()) {
                config.setScheduledTimes(scheduledTimes);
            }

            List<String> scheduledDays = splitCsv(props.getProperty("scheduled-days", ""));
            if (!scheduledDays.isEmpty()) {
                config.setScheduledDays(scheduledDays);
            }

            List<Integer> warningTimes = splitIntegerCsv(props.getProperty("warning-times", "300,60,30,10,5"));
            if (!warningTimes.isEmpty()) {
                config.setWarningTimes(warningTimes.stream()
                    .filter(value -> value > 0)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .toList());
            } else {
                config.setWarningTimes(new ArrayList<>(List.of(300, 60, 30, 10, 5)));
            }

            config.setTimezone(props.getProperty("timezone", config.getTimezone()).trim());

            applyBoolean(props, "scheduled-restarts-enabled", config::setScheduledRestartsEnabled);
            applyInteger(props, "warning-time", config::setScheduledWarningTime);
            applyBoolean(props, "alerts-enabled", config::setAlertsEnabled);

            applyBoolean(props, "monitoring-enabled", config::setMonitoringEnabled);
            applyDouble(props, "tps-threshold", value -> config.setTpsThreshold(clamp(value, 0.0, 20.0)));
            applyDouble(props, "memory-threshold", value -> config.setMemoryThreshold(clamp(value, 0.0, 100.0)));
            applyInteger(props, "check-interval", value -> config.setCheckInterval(Math.max(value, 1)));
            applyInteger(props, "consecutive-checks", value -> config.setConsecutiveChecks(Math.max(value, 1)));

            applyBoolean(props, "emergency-enabled", config::setEmergencyRestartEnabled);
            applyDouble(props, "emergency-tps-threshold", value -> config.setEmergencyTpsThreshold(clamp(value, 0.0, 20.0)));
            applyDouble(props, "emergency-memory-threshold", value -> config.setEmergencyMemoryThreshold(clamp(value, 0.0, 100.0)));
            applyInteger(props, "emergency-delay", value -> config.setEmergencyDelay(Math.max(value, 0)));

            applyInteger(props, "shutdown-delay-ticks", value -> config.setShutdownDelayTicks(Math.max(value, 0)));
            applyBoolean(props, "use-op-as-admin", config::setUseOpAsAdminEnabled);
            applyInteger(props, "default-permission-level", value -> config.setDefaultPermissionLevel(clamp(value, 0, 4)));
            applyBoolean(props, "public-permissions-enabled", config::setPublicPermissionsEnabled);

            String prefix = props.getProperty("plugin-prefix");
            if (prefix != null) {
                config.setPrefix(prefix);
            }
            applyBoolean(props, "chat-alerts-enabled", config::setChatAlertsEnabled);
            String chatFormat = props.getProperty("chat-alert-format");
            if (chatFormat != null) {
                config.setChatAlertFormat(chatFormat);
            }
            applyBoolean(props, "title-alerts-enabled", config::setTitleAlertsEnabled);
            String titleMain = props.getProperty("title-main-text");
            if (titleMain != null) {
                config.setTitleMainText(titleMain);
            }
            String titleSub = props.getProperty("title-sub-text");
            if (titleSub != null) {
                config.setTitleSubText(titleSub);
            }
            applyBoolean(props, "actionbar-alerts-enabled", config::setActionBarAlertsEnabled);
            String actionFormat = props.getProperty("actionbar-format");
            if (actionFormat != null) {
                config.setActionBarFormat(actionFormat);
            }
            applyBoolean(props, "discord-enabled", config::setDiscordEnabled);
            String discordUrl = props.getProperty("discord-webhook-url");
            if (discordUrl != null) {
                config.setDiscordWebhookUrl(discordUrl);
            }
            String discordUser = props.getProperty("discord-username");
            if (discordUser != null && !discordUser.isBlank()) {
                config.setDiscordUsername(discordUser);
            }
        } catch (IOException exception) {
            logger.warning("Failed to load config: " + exception.getMessage());
        }

        return config;
    }

    @Override
    public void reloadPlatformState() {
        if (runtimeConfigPath == null || mutableConfig == null) {
            return;
        }

        SimplePlatformConfig reloadedConfig = loadSimpleConfig(runtimeConfigPath);
        copyConfig(reloadedConfig, mutableConfig);

        stopPlatformMonitoring();
        startPlatformMonitoring();
        logger.info("Reloaded platform config from " + runtimeConfigPath);
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

    private Thread shutdownHookThread;

    protected final void stopCore() {
        if (started.compareAndSet(true, false)) {
            stopPlatformMonitoring();

            if (shutdownHookThread != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
                } catch (IllegalStateException ignored) {
                }
                shutdownHookThread = null;
            }

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
        shutdownHookThread = new Thread(this::stopCore, threadName);
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
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

    private static String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        }
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    @Override
    public void sendRestartAlert(int seconds, dev.demonz.redstonereboot.common.manager.RestartReason reason) {
        if (mutableConfig == null || !mutableConfig.isAlertsEnabled()) {
            return;
        }

        String timeString = formatDuration(seconds);

        if (mutableConfig.isChatAlertsEnabled()) {
            String chatMessage = mutableConfig.getChatAlertFormat()
                .replace("{time}", timeString)
                .replace("{reason}", reason.getDisplayName());
            broadcastMessage(LegacyTextUtil.translateAlternateColorCodes(chatMessage));
        }

        if (mutableConfig.isTitleAlertsEnabled()) {
            String mainTitle = mutableConfig.getTitleMainText();
            String subTitle = mutableConfig.getTitleSubText().replace("{time}", timeString);
            broadcastTitle(
                LegacyTextUtil.translateAlternateColorCodes(mainTitle),
                LegacyTextUtil.translateAlternateColorCodes(subTitle)
            );
        }

        if (mutableConfig.isActionBarAlertsEnabled()) {
            String actionFormat = mutableConfig.getActionBarFormat()
                .replace("{time}", timeString)
                .replace("{reason}", reason.getDisplayName());
            broadcastActionBar(LegacyTextUtil.translateAlternateColorCodes(actionFormat));
        }
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                String chatRaw = mutableConfig.getChatAlertFormat().replace("{time}", timeString).replace("{reason}", reason.getDisplayName());
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.SCHEDULED_ALERT,
                    seconds, reason, "System", chatRaw, mutableConfig.getTitleMainText(), mutableConfig.getTitleSubText().replace("{time}", timeString),
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void sendFinalRestartAlert(dev.demonz.redstonereboot.common.manager.RestartReason reason) {
        if (mutableConfig == null || !mutableConfig.isAlertsEnabled()) {
            return;
        }
        String prefix = mutableConfig.getPrefix();
        String msg = prefix + " &cServer is restarting NOW! Reason: &e" + reason.getDisplayName();
        broadcastMessage(LegacyTextUtil.translateAlternateColorCodes(msg));
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.FINAL_ALERT,
                    0, reason, "System", msg, null, null,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void sendRestartCancelledAlert() {
        if (mutableConfig == null || !mutableConfig.isAlertsEnabled()) {
            return;
        }
        String prefix = mutableConfig.getPrefix();
        String msg = prefix + " &aScheduled restart has been CANCELLED!";
        broadcastMessage(LegacyTextUtil.translateAlternateColorCodes(msg));
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.CANCELLED,
                    -1, null, "System", msg, null, null,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void sendEmergencyAlert(String reason) {
        if (mutableConfig == null || !mutableConfig.isAlertsEnabled()) {
            return;
        }
        String prefix = mutableConfig.getPrefix();
        String chat = prefix + " &4&lEMERGENCY RESTART&r&c - " + reason;
        broadcastMessage(LegacyTextUtil.translateAlternateColorCodes(chat));
        broadcastTitle(
            LegacyTextUtil.translateAlternateColorCodes("&4&lEmergency Restart"),
            LegacyTextUtil.translateAlternateColorCodes("&c" + reason)
        );
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.EMERGENCY,
                    -1, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "EmergencyMonitor", chat, "&4&lEmergency Restart", "&c" + reason,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
                api.fireEmergency(reason, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void sendPostponedAlert(String adminDetail) {
        if (mutableConfig == null || !mutableConfig.isAlertsEnabled()) {
            return;
        }
        String prefix = mutableConfig.getPrefix();
        String msg = prefix + " &cScheduled restart postponed. &eThe server will remain online.";
        broadcastMessage(LegacyTextUtil.translateAlternateColorCodes(msg));
        logger.warning("RESTART POSTPONED - Admin Detail: " + adminDetail);
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.POSTPONED,
                    -1, null, "System", msg + " (" + adminDetail + ")", null, null,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
                api.fireFailed(adminDetail);
            }
        } catch (Exception ignored) {}
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
        props.setProperty("config-version", String.valueOf(CURRENT_MOD_CONFIG_VERSION));
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
        props.setProperty("public-permissions-enabled", "true");
        props.setProperty("plugin-prefix", "§8[§cRedstone§8] §aReboot");
        props.setProperty("chat-alerts-enabled", "true");
        props.setProperty("chat-alert-format", "§8[§cRedstone§8] §eServer will restart in §c{time}§e!");
        props.setProperty("title-alerts-enabled", "true");
        props.setProperty("title-main-text", "§c⚡ Server Restart");
        props.setProperty("title-sub-text", "§ein §c{time}");
        props.setProperty("actionbar-alerts-enabled", "true");
        props.setProperty("actionbar-format", "§8[§cRedstone§8] §eRestart in: §c{time}");
        props.setProperty("discord-enabled", "false");
        props.setProperty("discord-webhook-url", "");
        props.setProperty("discord-username", "RedstoneReboot");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            out.write(("# config-version: " + CURRENT_MOD_CONFIG_VERSION + " (do not edit manually)\n").getBytes());
            props.store(out, "RedstoneReboot Configuration");
        }
    }

    private static int parseConfigVersion(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private void migrateModConfig(Properties props, int oldVersion, Path configPath) throws IOException {
        logger.info("Migrating " + configPath.getFileName() + " from version " + oldVersion + " to " + CURRENT_MOD_CONFIG_VERSION + "...");
        if (Files.exists(configPath)) {
            Path backup = configPath.resolveSibling(configPath.getFileName().toString() + ".v" + oldVersion + ".backup");
            try {
                Files.copy(configPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("Created mod config backup: " + backup.getFileName());
            } catch (Exception e) {
                logger.warning("Failed to create mod config backup: " + e.getMessage());
            }
        }
        if (oldVersion < 2) {

            if (!props.containsKey("config-version")) {
                props.setProperty("config-version", String.valueOf(CURRENT_MOD_CONFIG_VERSION));
            } else {
                props.setProperty("config-version", String.valueOf(CURRENT_MOD_CONFIG_VERSION));
            }

            props.putIfAbsent("scheduled-restarts-enabled", "false");
            props.putIfAbsent("scheduled-times", "06:00,18:00");
            props.putIfAbsent("scheduled-days", "ALL");
            props.putIfAbsent("timezone", "UTC");
            props.putIfAbsent("warning-time", "300");
            props.putIfAbsent("warning-times", "300,60,30,10,5,4,3,2,1");
            props.putIfAbsent("alerts-enabled", "true");
            props.putIfAbsent("monitoring-enabled", "false");
            props.putIfAbsent("tps-threshold", "18.0");
            props.putIfAbsent("memory-threshold", "85.0");
            props.putIfAbsent("check-interval", "30");
            props.putIfAbsent("consecutive-checks", "3");
            props.putIfAbsent("emergency-enabled", "false");
            props.putIfAbsent("emergency-tps-threshold", "12.0");
            props.putIfAbsent("emergency-memory-threshold", "95.0");
            props.putIfAbsent("emergency-delay", "30");
            props.putIfAbsent("shutdown-delay-ticks", "60");
            props.putIfAbsent("use-op-as-admin", "true");
            props.putIfAbsent("default-permission-level", "2");
            props.putIfAbsent("public-permissions-enabled", "true");
            props.putIfAbsent("plugin-prefix", "§8[§cRedstone§8] §aReboot");
            props.putIfAbsent("chat-alerts-enabled", "true");
            props.putIfAbsent("chat-alert-format", "§8[§cRedstone§8] §eServer will restart in §c{time}§e!");
            props.putIfAbsent("title-alerts-enabled", "true");
            props.putIfAbsent("title-main-text", "§c⚡ Server Restart");
            props.putIfAbsent("title-sub-text", "§ein §c{time}");
            props.putIfAbsent("actionbar-alerts-enabled", "true");
            props.putIfAbsent("actionbar-format", "§8[§cRedstone§8] §eRestart in: §c{time}");
            props.putIfAbsent("discord-enabled", "false");
            props.putIfAbsent("discord-webhook-url", "");
            props.putIfAbsent("discord-username", "RedstoneReboot");
        }

        try (OutputStream out = Files.newOutputStream(configPath)) {
            out.write(("# config-version: " + CURRENT_MOD_CONFIG_VERSION + " (migrated from v" + oldVersion + ")\n").getBytes());
            props.store(out, "RedstoneReboot Configuration (migrated)");
        }
        logger.info("Successfully migrated " + configPath.getFileName() + " to version " + CURRENT_MOD_CONFIG_VERSION + "!");
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

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private void copyConfig(SimplePlatformConfig source, SimplePlatformConfig target) {
        target.setScheduledRestartsEnabled(source.isScheduledRestartsEnabled());
        target.setScheduledTimes(new ArrayList<>(source.getScheduledTimes()));
        target.setScheduledDays(new ArrayList<>(source.getScheduledDays()));
        target.setTimezone(source.getTimezone());
        target.setScheduledWarningTime(source.getScheduledWarningTime());
        target.setWarningTimes(new ArrayList<>(source.getWarningTimes()));
        target.setAlertsEnabled(source.isAlertsEnabled());
        target.setMonitoringEnabled(source.isMonitoringEnabled());
        target.setTpsThreshold(source.getTpsThreshold());
        target.setMemoryThreshold(source.getMemoryThreshold());
        target.setCheckInterval(source.getCheckInterval());
        target.setConsecutiveChecks(source.getConsecutiveChecks());
        target.setEmergencyRestartEnabled(source.isEmergencyRestartEnabled());
        target.setEmergencyTpsThreshold(source.getEmergencyTpsThreshold());
        target.setEmergencyMemoryThreshold(source.getEmergencyMemoryThreshold());
        target.setEmergencyDelay(source.getEmergencyDelay());
        target.setShutdownDelayTicks(source.getShutdownDelayTicks());
        target.setUseOpAsAdminEnabled(source.isUseOpAsAdminEnabled());
        target.setDefaultPermissionLevel(source.getDefaultPermissionLevel());
        target.setPublicPermissionsEnabled(source.isPublicPermissionsEnabled());
        target.setPrefix(source.getPrefix());
        target.setChatAlertsEnabled(source.isChatAlertsEnabled());
        target.setChatAlertFormat(source.getChatAlertFormat());
        target.setTitleAlertsEnabled(source.isTitleAlertsEnabled());
        target.setTitleMainText(source.getTitleMainText());
        target.setTitleSubText(source.getTitleSubText());
        target.setActionBarAlertsEnabled(source.isActionBarAlertsEnabled());
        target.setActionBarFormat(source.getActionBarFormat());
        target.setDiscordEnabled(source.isDiscordEnabled());
        target.setDiscordWebhookUrl(source.getDiscordWebhookUrl());
        target.setDiscordUsername(source.getDiscordUsername());
    }
}
