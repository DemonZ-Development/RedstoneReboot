package dev.demonz.redstonereboot.common.platform;

import dev.demonz.redstonereboot.common.manager.RestartReason;

public interface ServerPlatform {

    void broadcastMessage(String message);

    void broadcastTitle(String title, String subtitle);

    default void broadcastActionBar(String message) {
    }

    default void sendAlert(String message, String title, String subtitle) {
        broadcastMessage(message);
        broadcastTitle(title, subtitle);
    }

    default void sendRestartAlert(int seconds, RestartReason reason) {
        String time = formatDuration(seconds);
        String chat = "\u00A7c\u00A7lSERVER RESTART \u00A7e- Reason: \u00A7f" + reason.getDisplayName() + " \u00A7bin " + time;
        String title = "\u00A7c\u00A7lRestarting";
        String subtitle = "\u00A7ein \u00A7f" + time;
        sendAlert(chat, title, subtitle);
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.SCHEDULED_ALERT,
                    seconds, reason, "System", chat, title, subtitle,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
            }
        } catch (Exception ignored) {}
    }

    default void sendFinalRestartAlert(RestartReason reason) {
        String msg = "\u00A7c\u00A7lSERVER RESTARTING NOW! \u00A7eReason: \u00A7f" + reason.getDisplayName();
        broadcastMessage(msg);
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

    default void sendRestartCancelledAlert() {
        String msg = "\u00A7a\u00A7lRESTART CANCELLED \u00A7e- The server will remain online.";
        broadcastMessage(msg);
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

    default void sendEmergencyAlert(String reason) {
        String chat = "\u00A74\u00A7lEMERGENCY RESTART \u00A7c- " + reason;
        String title = "\u00A74\u00A7lEmergency Restart";
        String subtitle = "\u00A7c" + reason;
        sendAlert(chat, title, subtitle);
        try {
            dev.demonz.redstonereboot.common.api.RedstoneRebootAPI api = dev.demonz.redstonereboot.common.api.RedstoneRebootAPI.getInstance();
            if (api != null) {
                api.dispatchMessage(new dev.demonz.redstonereboot.common.api.MessageContext(
                    dev.demonz.redstonereboot.common.api.MessageContext.Type.EMERGENCY,
                    -1, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS, "EmergencyMonitor", chat, title, subtitle,
                    System.currentTimeMillis(), getPlatformName(), getMinecraftVersion()));
                api.fireEmergency(reason, dev.demonz.redstonereboot.common.manager.RestartReason.EMERGENCY_TPS);
            }
        } catch (Exception ignored) {}
    }

    default void sendPostponedAlert(String adminDetail) {
        String msg = "\u00A7c\u00A7lScheduled restart postponed. \u00A7eThe server will remain online.";
        broadcastMessage(msg);
        java.util.logging.Logger.getLogger("RedstoneReboot")
            .warning("RESTART POSTPONED - Admin Detail: " + adminDetail);
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

    default void reloadPlatformState() {
    }

    void executeConsole(String command);

    double getTPS();

    default String getPlatformName() {
        return "Unknown";
    }

    default String getMinecraftVersion() {
        return "Unknown";
    }

    default int getOnlinePlayerCount() {
        return 0;
    }

    default int getDefaultPermissionLevel() {
        return 2;
    }

    default void shutdownServer() {
        executeConsole("stop");
    }

    default void shutdownServer(String reason) {
        shutdownServer();
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
