package dev.demonz.redstonereboot.common;

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
import java.util.logging.Logger;

/**
 * Core engine for RedstoneReboot.
 */
public class RedstoneRebootCore {

    public static final String VERSION = "1.3.1";
    public static final String BRAND = "RedstoneReboot";

    private static final Logger LOGGER = Logger.getLogger(BRAND);

    private final ServerPlatform platform;
    private final PlatformTaskScheduler scheduler;
    private final PlatformConfig config;
    private final UpdateChecker updateChecker;
    private final BackendRegistry backendRegistry;
    private final RestartManager restartManager;

    public RedstoneRebootCore(ServerPlatform platform, PlatformTaskScheduler scheduler, PlatformConfig config, Path dataFolder) {
        this.platform = platform;
        this.scheduler = scheduler;
        this.config = config;
        this.updateChecker = new UpdateChecker("redstonereboot", VERSION, LOGGER);
        
        BackendConfig backendConfig = new BackendConfig(dataFolder, LOGGER);
        this.backendRegistry = new BackendRegistry(LOGGER, backendConfig);
        this.restartManager = new RestartManager(LOGGER, platform, scheduler, config, backendRegistry);
    }

    /**
     * Called when the platform enables the plugin or mod.
     */
    public void onEnable() {
        printStartupBanner();
        LOGGER.info("Platform: " + platform.getPlatformName() + " (MC " + platform.getMinecraftVersion() + ")");
        LOGGER.info("TPS: " + String.format("%.1f", platform.getTPS()));

        backendRegistry.initialize();
        restartManager.initialize();

        // Advisory environment detection
        List<String> detected = EnvironmentDetector.detectPotentialBackends();
        if (!detected.isEmpty()) {
            LOGGER.info("Detected Environment: " + String.join(", ", detected));
            String active = backendRegistry.getActiveBackend().getName().toUpperCase();
            if (!detected.contains(active) && !active.equals("SHUTDOWNONLY") && !active.equals("LOCALSCRIPT")) {
                LOGGER.warning("Mismatch detected: Running on " + String.join("/", detected) + " but backend is " + active);
            }
        }

        LOGGER.info("Engine initialized successfully.");
        updateChecker.checkForUpdates();
    }

    /**
     * Called when the platform disables the plugin or mod.
     */
    public void onDisable() {
        LOGGER.info("RedstoneReboot engine shutting down...");
        restartManager.cleanup();
        LOGGER.info("Shutdown complete.");
    }

    public void reloadRuntimeState() {
        platform.reloadPlatformState();
        backendRegistry.initialize();
        restartManager.initialize();
    }

    /**
     * Trigger an emergency restart with a given reason.
     *
     * @param reason the human-readable reason for the emergency
     */
    public void triggerEmergencyRestart(String reason) {
        LOGGER.severe("==========================================");
        LOGGER.severe("EMERGENCY RESTART TRIGGERED");
        LOGGER.severe("Reason: " + reason);
        LOGGER.severe("==========================================");
        platform.sendEmergencyAlert(reason);
        int delay = config.getEmergencyDelay();
        if (delay > 0) {
            restartManager.scheduleRestart(delay, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "Emergency: " + reason);
        } else {
            restartManager.performImmediateRestart(dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "Emergency: " + reason);
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
}
