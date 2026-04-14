package dev.demonz.redstonereboot.common.platform;

import java.time.ZoneId;
import java.util.List;

/**
 * Read-only interface for accessing platform-specific configuration values.
 * <p>
 * Implemented by {@link SimplePlatformConfig} for mod platforms and by the
 * Bukkit {@code ConfigManager} for plugin platforms. The common engine reads
 * all scheduling, monitoring, and emergency configuration through this interface.
 * </p>
 *
 * @since 1.0.0
 */
public interface PlatformConfig {

    /** @return {@code true} if scheduled automatic restarts are enabled */
    boolean isScheduledRestartsEnabled();

    /** @return list of configured restart times in {@code H:mm} format (e.g. {@code "06:00"}) */
    List<String> getScheduledTimes();

    /** @return list of day names (e.g. {@code "MONDAY"}) or {@code "ALL"} */
    List<String> getScheduledDays();

    /** @return the configured timezone as a {@link ZoneId} */
    ZoneId getZoneId();

    /** @return the raw timezone string from configuration (e.g. {@code "Asia/Kolkata"}) */
    String getTimezone();

    /** @return seconds of warning time before a scheduled restart begins */
    int getScheduledWarningTime();

    /** @return ordered list of countdown warning thresholds in seconds */
    List<Integer> getWarningTimes();

    /** @return {@code true} if player-facing alerts are enabled */
    boolean isAlertsEnabled();

    /** @return {@code true} if TPS/memory health monitoring is enabled */
    boolean isMonitoringEnabled();

    /** @return TPS threshold below which monitoring triggers a restart */
    double getTpsThreshold();

    /** @return memory usage percentage above which monitoring triggers a restart */
    double getMemoryThreshold();

    /** @return interval in seconds between health monitoring checks */
    int getCheckInterval();

    /** @return number of consecutive failed checks required before triggering */
    int getConsecutiveChecks();

    /** @return {@code true} if emergency immediate-restart logic is enabled */
    boolean isEmergencyRestartEnabled();

    /** @return TPS threshold for emergency restarts (more aggressive than monitoring) */
    double getEmergencyTpsThreshold();

    /** @return memory percentage threshold for emergency restarts */
    double getEmergencyMemoryThreshold();

    /** @return delay in seconds before an emergency restart executes */
    int getEmergencyDelay();

    /** @return ticks to wait after saving worlds before executing the shutdown sequence */
    int getShutdownDelayTicks();

    /** @return {@code true} if server operators are treated as admins when no permission plugin is present */
    boolean isUseOpAsAdminEnabled();

    /** @return the default operator permission level for mod platform commands (0–4) */
    default int getDefaultPermissionLevel() {
        return 2;
    }
}
