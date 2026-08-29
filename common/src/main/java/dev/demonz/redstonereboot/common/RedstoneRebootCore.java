package dev.demonz.redstonereboot.common;

import dev.demonz.redstonereboot.common.api.RedstoneRebootAPI;
import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.backend.EnvironmentDetector;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import dev.demonz.redstonereboot.common.utils.UpdateChecker;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class RedstoneRebootCore {

    public static final String VERSION = "1.6.0";
    public static final String BRAND = "RedstoneReboot";

    private static final Logger LOGGER = Logger.getLogger(BRAND);

    public String getVersion() { return VERSION; }

    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final UpdateChecker updateChecker;
    private final BackendRegistry backendRegistry;
    private final RestartManager restartManager;
    private final Path dataFolder;
    private volatile dev.demonz.redstonereboot.common.api.MessageAdapter discordAdapter;

    public RedstoneRebootCore(ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config, Path dataFolder) {
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.dataFolder = dataFolder;
        this.updateChecker = new UpdateChecker("redstonereboot", VERSION, LOGGER, resolveModrinthLoader(platform.getPlatformName()));

        BackendConfig backendConfig = new BackendConfig(dataFolder, LOGGER);
        this.backendRegistry = new BackendRegistry(LOGGER, backendConfig, dataFolder);
        this.restartManager = new RestartManager(LOGGER, platform, scheduler, config, backendRegistry, dataFolder);
    }

    private static String resolveModrinthLoader(String platformName) {
        if (platformName == null) return null;
        String lower = platformName.toLowerCase(Locale.ROOT);
        if (lower.contains("neoforge")) return "neoforge";
        if (lower.contains("forge")) return "forge";
        if (lower.contains("fabric")) return "fabric";
        if (lower.contains("folia")) return "folia";
        if (lower.contains("paper")) return "paper";
        if (lower.contains("purpur")) return "purpur";
        if (lower.contains("spigot")) return "spigot";
        if (lower.contains("bukkit") || lower.contains("craftbukkit")) return "bukkit";

        return null;
    }

    public void onEnable() {
        printStartupBanner();
        LOGGER.info("Platform: " + platform.getPlatformName() + " (MC " + platform.getMinecraftVersion() + ")");
        LOGGER.info("TPS: " + String.format(Locale.ROOT, "%.1f", platform.getTPS()));

        try {
            RedstoneRebootAPI.setInstance(new RedstoneRebootAPI(this));
            LOGGER.info("Developer API available: RedstoneRebootAPI.getInstance()");
        } catch (Exception e) {
            LOGGER.warning("Failed to initialize Developer API: " + e.getMessage());
        }

        backendRegistry.initialize();
        restartManager.initialize();

        List<String> detected = EnvironmentDetector.detectPotentialBackends();
        if (!detected.isEmpty()) {
            LOGGER.info("Detected Environment: " + String.join(", ", detected));
            String active = backendRegistry.getActiveBackend().getName().toUpperCase();

            String normalizedActive = active.replace("_", "");
            boolean isShutdownHost = normalizedActive.equals("SHUTDOWNONLY") || normalizedActive.equals("DEPENDONHOST");
            boolean isLocalScript = normalizedActive.equals("LOCALSCRIPT");
            boolean isPterodactylOnDocker = normalizedActive.equals("PTERODACTYL") && detected.contains("DOCKER") && !detected.contains("PTERODACTYL");
            if (!detected.contains(active) && !isShutdownHost && !isLocalScript && !isPterodactylOnDocker) {
                LOGGER.warning("Mismatch detected: Running on " + String.join("/", detected) + " but backend is " + active);
            } else if (isPterodactylOnDocker) {
                LOGGER.info("Environment note: Pterodactyl backend active inside Docker container (expected).");
            }
        }

        LOGGER.info("Engine initialized successfully.");
        syncDiscordAdapter();
        updateChecker.checkForUpdates();
        updateChecker.startPeriodicChecks(scheduler);
    }

    public void onDisable() {
        LOGGER.info("RedstoneReboot engine shutting down...");
        restartManager.cleanup();
        updateChecker.stopPeriodicChecks();
        clearDiscordAdapter();
        try { RedstoneRebootAPI.clearInstance(); } catch (Exception ignored) {}
        LOGGER.info("Shutdown complete.");
    }

    public void reloadRuntimeState() {
        platform.reloadPlatformState();
        backendRegistry.initialize();
        restartManager.initialize();
        syncDiscordAdapter();
    }

    public void triggerEmergencyRestart(String reason) {
        triggerEmergencyRestart(reason, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS);
    }

    public void triggerEmergencyRestart(String reason, dev.demonz.redstonereboot.common.manager.RestartReason restartReason) {
        LOGGER.severe("==========================================");
        LOGGER.severe("EMERGENCY RESTART TRIGGERED");
        LOGGER.severe("Reason: " + reason);
        LOGGER.severe("==========================================");
        platform.sendEmergencyAlert(reason);
        int delay = config.getEmergencyDelay();
        if (delay > 0) {
            restartManager.scheduleRestart(delay, restartReason, "Emergency: " + reason);
        } else {
            restartManager.performImmediateRestart(restartReason, "Emergency: " + reason);
        }
    }

    private void printStartupBanner() {
        String[] banner = {
            "",
            "==========================================",
            "  RedstoneReboot v" + VERSION,
            "  by DemonZ Development",
            "------------------------------------------",
            "  Platform  : " + platform.getPlatformName(),
            "  Minecraft : " + platform.getMinecraftVersion(),
            "  Players   : " + platform.getOnlinePlayerCount(),
            "  Engine    : Multi-Platform Restart Engine",
            "==========================================",
            ""
        };

        for (String line : banner) {
            LOGGER.info(LegacyTextUtil.stripLegacyFormatting(line));
        }
    }

    public ServerPlatform getPlatform() {
        return platform;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public BackendRegistry getBackendRegistry() {
        return backendRegistry;
    }

    public PlatformTaskScheduler getScheduler() {
        return scheduler;
    }

    public PlatformConfig getConfig() {
        return config;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    private void syncDiscordAdapter() {
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api == null) return;
            if (discordAdapter != null) {
                api.unregisterMessageAdapter(discordAdapter);
                discordAdapter = null;
            }
            if (config.isDiscordEnabled()) {
                String url = config.getDiscordWebhookUrl();
                if (url != null && !url.isBlank() && url.startsWith("http")) {
                    discordAdapter = new dev.demonz.redstonereboot.common.api.DiscordWebhookAdapter(LOGGER, url, config.getDiscordUsername());
                    api.registerMessageAdapter(discordAdapter);
                    LOGGER.info("Discord webhook integration enabled");
                } else if (config.isDiscordEnabled()) {
                    LOGGER.warning("Discord enabled but webhook-url is empty/invalid - integration skipped");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to sync Discord adapter: " + e.getMessage());
        }
    }

    private void clearDiscordAdapter() {
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null && discordAdapter != null) {
                api.unregisterMessageAdapter(discordAdapter);
            }
        } catch (Exception ignored) {}
        discordAdapter = null;
    }
}
