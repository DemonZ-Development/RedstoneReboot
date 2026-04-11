package dev.demonz.redstonereboot.common;

import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import dev.demonz.redstonereboot.common.utils.UpdateChecker;

import java.util.logging.Logger;

/**
 * Core engine for RedstoneReboot.
 */
public class RedstoneRebootCore {

    public static final String VERSION = "1.3.0";
    public static final String BRAND = "RedstoneReboot";

    private static final Logger LOGGER = Logger.getLogger(BRAND);

    private final ServerPlatform platform;
    private final UpdateChecker updateChecker;

    public RedstoneRebootCore(ServerPlatform platform) {
        this.platform = platform;
        this.updateChecker = new UpdateChecker("redstonereboot", VERSION, LOGGER);
    }

    /**
     * Called when the platform enables the plugin or mod.
     */
    public void onEnable() {
        printStartupBanner();
        LOGGER.info("Platform: " + platform.getPlatformName() + " (MC " + platform.getMinecraftVersion() + ")");
        LOGGER.info("TPS: " + String.format("%.1f", platform.getTPS()));
        LOGGER.info("Engine initialized successfully.");
        updateChecker.checkForUpdates();
    }

    /**
     * Called when the platform disables the plugin or mod.
     */
    public void onDisable() {
        LOGGER.info("RedstoneReboot engine shutting down...");
        LOGGER.info("Shutdown complete.");
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
        platform.broadcastTitle("§c§lEMERGENCY RESTART", "§e" + reason);
        platform.broadcastMessage("§c§lSERVER GOING DOWN - " + reason);
        platform.shutdownServer();
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
}
