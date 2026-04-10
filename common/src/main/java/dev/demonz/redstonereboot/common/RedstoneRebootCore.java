package dev.demonz.redstonereboot.common;

import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core engine for RedstoneReboot — platform-agnostic restart management.
 * <p>
 * This class contains all shared logic and is instantiated by each
 * platform-specific module (Bukkit, Folia, Fabric, Forge, NeoForge).
 * </p>
 *
 * @author DemonZ Development
 * @since 1.0.0
 */
public class RedstoneRebootCore {

    public static final String VERSION = "1.0.0";
    public static final String BRAND = "RedstoneReboot";

    private static final Logger LOGGER = LoggerFactory.getLogger(BRAND);

    private final ServerPlatform platform;

    public RedstoneRebootCore(ServerPlatform platform) {
        this.platform = platform;
    }

    /**
     * Called when the platform enables the plugin/mod.
     */
    public void onEnable() {
        printStartupBanner();
        LOGGER.info("Platform: {} (MC {})", platform.getPlatformName(), platform.getMinecraftVersion());
        LOGGER.info("TPS: {}", String.format("%.1f", platform.getTPS()));
        LOGGER.info("Engine initialized successfully.");
    }

    /**
     * Called when the platform disables the plugin/mod.
     */
    public void onDisable() {
        LOGGER.info("RedstoneReboot engine shutting down...");
        LOGGER.info("Goodbye! 👋");
    }

    /**
     * Trigger an emergency restart with a given reason.
     *
     * @param reason the human-readable reason for the emergency
     */
    public void triggerEmergencyRestart(String reason) {
        LOGGER.error("╔══════════════════════════════════════╗");
        LOGGER.error("║    EMERGENCY RESTART TRIGGERED       ║");
        LOGGER.error("║    Reason: {}", reason);
        LOGGER.error("╚══════════════════════════════════════╝");
        platform.broadcastTitle("§c§l⚠ EMERGENCY RESTART", "§e" + reason);
        platform.broadcastMessage("§c§lSERVER GOING DOWN — " + reason);
        platform.shutdownServer();
    }

    /**
     * Print the premium ASCII startup banner to console.
     */
    private void printStartupBanner() {
        String[] banner = {
            "",
            "§c  ██████╗ ███████╗██████╗ ███████╗████████╗ ██████╗ ███╗   ██╗███████╗",
            "§c  ██╔══██╗██╔════╝██╔══██╗██╔════╝╚══██╔══╝██╔═══██╗████╗  ██║██╔════╝",
            "§c  ██████╔╝█████╗  ██║  ██║███████╗   ██║   ██║   ██║██╔██╗ ██║█████╗  ",
            "§c  ██╔══██╗██╔══╝  ██║  ██║╚════██║   ██║   ██║   ██║██║╚██╗██║██╔══╝  ",
            "§c  ██║  ██║███████╗██████╔╝███████║   ██║   ╚██████╔╝██║ ╚████║███████╗",
            "§c  ╚═╝  ╚═╝╚══════╝╚═════╝ ╚══════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═══╝╚══════╝",
            "§6  ██████╗ ███████╗██████╗  ██████╗  ██████╗ ████████╗",
            "§6  ██╔══██╗██╔════╝██╔══██╗██╔═══██╗██╔═══██╗╚══██╔══╝",
            "§6  ██████╔╝█████╗  ██████╔╝██║   ██║██║   ██║   ██║   ",
            "§6  ██╔══██╗██╔══╝  ██╔══██╗██║   ██║██║   ██║   ██║   ",
            "§6  ██║  ██║███████╗██████╔╝╚██████╔╝╚██████╔╝   ██║   ",
            "§6  ╚═╝  ╚═╝╚══════╝╚═════╝  ╚═════╝  ╚═════╝    ╚═╝   ",
            "",
            "§8  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§7  ⚡ §fRedstoneReboot §7v" + VERSION + " §8│ §7by §cDemonZ Development",
            "§8  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            "§7  │ §fPlatform   §8» §a" + platform.getPlatformName(),
            "§7  │ §fMinecraft  §8» §e" + platform.getMinecraftVersion(),
            "§7  │ §fPlayers    §8» §b" + platform.getOnlinePlayerCount(),
            "§7  │ §fEngine     §8» §dMulti-Platform Restart Engine",
            "§8  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            ""
        };

        for (String line : banner) {
            // Strip § color codes for SLF4J output
            String clean = line.replaceAll("§[0-9a-fk-or]", "");
            LOGGER.info(clean);
        }
    }

    public ServerPlatform getPlatform() {
        return platform;
    }
}
