package dev.demonz.redstonereboot.common.manager;

/**
 * Standardized reasons for server restarts.
 * <p>
 * Each restart event carries a reason that is displayed to players in alerts
 * and logged for diagnostic purposes.
 * </p>
 *
 * @since 1.0.0
 */
public enum RestartReason {
    /** A restart triggered by the automated schedule timer. */
    SCHEDULED("Scheduled Restart"),

    /** A restart manually scheduled by an operator via {@code /reboot schedule}. */
    SCHEDULED_API("Manual Scheduled Restart"),

    /** An immediate restart triggered by an operator via {@code /reboot now}. */
    MANUAL("Manual Restart"),

    /** An emergency restart triggered by critically low TPS. */
    EMERGENCY_TPS("Emergency - Low TPS"),

    /** An emergency restart triggered by critically high memory usage. */
    EMERGENCY_MEMORY("Emergency - High Memory"),

    /** A restart triggered programmatically through the Developer API. */
    API("API Restart"),

    /** Fallback reason when the trigger source is undetermined. */
    UNKNOWN("Unknown");

    private final String displayName;

    RestartReason(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Get the human-readable display name for this restart reason.
     *
     * @return display name shown to players in alerts
     */
    public String getDisplayName() {
        return displayName;
    }
}
