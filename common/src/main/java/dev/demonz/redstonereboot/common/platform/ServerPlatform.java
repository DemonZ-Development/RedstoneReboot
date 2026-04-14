package dev.demonz.redstonereboot.common.platform;

import dev.demonz.redstonereboot.common.manager.RestartReason;

/**
 * Platform abstraction interface for RedstoneReboot.
 * <p>
 * Each server platform (Bukkit, Folia, Fabric, Forge, NeoForge)
 * implements this interface to provide platform-specific functionality
 * to the shared core engine.
 * </p>
 *
 * @author DemonZ Development
 * @since 1.0.0
 */
public interface ServerPlatform {

    /**
     * Broadcast a message to all online players.
     *
     * @param message the message to broadcast (supports MiniMessage format)
     */
    void broadcastMessage(String message);

    /**
     * Display a title and subtitle to all online players.
     *
     * @param title    the main title text
     * @param subtitle the subtitle text
     */
    void broadcastTitle(String title, String subtitle);

    /**
     * Send a unified alert (chat, title, action bar, etc.) based on platform configuration.
     *
     * @param message  the chat message
     * @param title    the main title
     * @param subtitle the subtitle
     */
    default void sendAlert(String message, String title, String subtitle) {
        broadcastMessage(message);
        broadcastTitle(title, subtitle);
    }

    default void sendRestartAlert(int seconds, RestartReason reason) {
        String time = formatDuration(seconds);
        sendAlert(
            "\u00A7c\u00A7lSERVER RESTART \u00A7e- Reason: \u00A7f" + reason.getDisplayName() + " \u00A7bin " + time,
            "\u00A7c\u00A7lRestarting",
            "\u00A7ein \u00A7f" + time
        );
    }

    default void sendFinalRestartAlert(RestartReason reason) {
        broadcastMessage("\u00A7c\u00A7lSERVER RESTARTING NOW! \u00A7eReason: \u00A7f" + reason.getDisplayName());
    }

    default void sendRestartCancelledAlert() {
        broadcastMessage("\u00A7a\u00A7lRESTART CANCELLED \u00A7e- The server will remain online.");
    }

    default void sendEmergencyAlert(String reason) {
        sendAlert(
            "\u00A74\u00A7lEMERGENCY RESTART \u00A7c- " + reason,
            "\u00A74\u00A7lEmergency Restart",
            "\u00A7c" + reason
        );
    }

    /**
     * Notify all players and admins that a restart has been postponed due to a backend error.
     *
     * @param adminDetail the technical details for administrators (log/console only)
     */
    default void sendPostponedAlert(String adminDetail) {
        broadcastMessage("\u00A7c\u00A7lScheduled restart postponed. \u00A7eThe server will remain online.");
        java.util.logging.Logger.getLogger("RedstoneReboot")
            .warning("RESTART POSTPONED - Admin Detail: " + adminDetail);
    }

    /**
     * Reload platform-managed configuration and state if supported.
     */
    default void reloadPlatformState() {
        // Default: platforms without mutable runtime config can ignore reload hooks.
    }

    /**
     * Execute a command from the server console.
     *
     * @param command the command string (without leading /)
     */
    void executeConsole(String command);

    /**
     * Get the current server TPS (ticks per second).
     *
     * @return the current TPS value (ideally 20.0)
     */
    double getTPS();

    /**
     * Get the name of the platform (e.g., "Bukkit", "Fabric").
     *
     * @return platform identifier string
     */
    default String getPlatformName() {
        return "Unknown";
    }

    /**
     * Get the Minecraft version the server is running.
     *
     * @return version string (e.g., "1.21.1")
     */
    default String getMinecraftVersion() {
        return "Unknown";
    }

    /**
     * Get the number of online players.
     *
     * @return online player count
     */
    default int getOnlinePlayerCount() {
        return 0;
    }

    /**
     * Get the default OP level for commands if no permission system is present.
     *
     * @return permission level (0-4)
     */
    default int getDefaultPermissionLevel() {
        return 2;
    }

    /**
     * Shutdown the server gracefully.
     */
    default void shutdownServer() {
        executeConsole("stop");
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
}
