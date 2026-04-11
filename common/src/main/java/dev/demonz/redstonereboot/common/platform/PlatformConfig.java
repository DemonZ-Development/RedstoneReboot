package dev.demonz.redstonereboot.common.platform;

import java.time.ZoneId;
import java.util.List;

/**
 * Interface for accessing platform-specific configuration in common code.
 */
public interface PlatformConfig {

    boolean isScheduledRestartsEnabled();

    List<String> getScheduledTimes();

    List<String> getScheduledDays();

    ZoneId getZoneId();

    String getTimezone();

    int getScheduledWarningTime();

    List<Integer> getWarningTimes();

    boolean isAlertsEnabled();

    // Monitoring
    boolean isMonitoringEnabled();

    double getTpsThreshold();

    double getMemoryThreshold();

    int getCheckInterval();

    int getConsecutiveChecks();

    // Emergency
    boolean isEmergencyRestartEnabled();

    double getEmergencyTpsThreshold();

    double getEmergencyMemoryThreshold();

    int getEmergencyDelay();

    int getShutdownDelayTicks();

    boolean isUseOpAsAdminEnabled();

    default int getDefaultPermissionLevel() {
        return 2;
    }
}
