package dev.demonz.redstonereboot.common.platform;

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
     * Shutdown the server gracefully.
     */
    default void shutdownServer() {
        executeConsole("stop");
    }
}
