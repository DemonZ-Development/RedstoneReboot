package dev.demonz.redstonereboot.common.platform;

import java.time.ZoneId;
import java.util.List;

public interface PlatformConfig {

    boolean isScheduledRestartsEnabled();

    List<String> getScheduledTimes();

    List<String> getScheduledDays();

    ZoneId getZoneId();

    String getTimezone();

    int getScheduledWarningTime();

    List<Integer> getWarningTimes();

    boolean isAlertsEnabled();

    boolean isMonitoringEnabled();

    double getTpsThreshold();

    double getMemoryThreshold();

    int getCheckInterval();

    int getConsecutiveChecks();

    boolean isEmergencyRestartEnabled();

    double getEmergencyTpsThreshold();

    double getEmergencyMemoryThreshold();

    int getEmergencyDelay();

    int getShutdownDelayTicks();

    boolean isUseOpAsAdminEnabled();

    default int getDefaultPermissionLevel() {
        return 2;
    }

    boolean isPublicPermissionsEnabled();

    default String getPrefix() { return "§8[§cRedstone§8] §aReboot"; }

    default boolean isChatAlertsEnabled() { return true; }

    default String getChatAlertFormat() { return "§8[§cRedstone§8] §eServer will restart in §c{time}§e!"; }

    default boolean isTitleAlertsEnabled() { return true; }

    default String getTitleMainText() { return "§c⚡ Server Restart"; }

    default String getTitleSubText() { return "§ein §c{time}"; }

    default boolean isActionBarAlertsEnabled() { return true; }

    default String getActionBarFormat() { return "§8[§cRedstone§8] §eRestart in: §c{time}"; }

    default boolean isDiscordEnabled() { return false; }

    default String getDiscordWebhookUrl() { return ""; }

    default String getDiscordUsername() { return "RedstoneReboot"; }
}
