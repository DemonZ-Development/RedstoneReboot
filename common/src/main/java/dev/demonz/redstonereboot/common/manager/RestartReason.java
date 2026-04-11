package dev.demonz.redstonereboot.common.manager;

/**
 * Standardized reasons for server restarts.
 */
public enum RestartReason {
    SCHEDULED("Scheduled Restart"),
    SCHEDULED_API("Manual Scheduled Restart"),
    MANUAL("Manual Restart"),
    EMERGENCY_TPS("Emergency - Low TPS"),
    EMERGENCY_MEMORY("Emergency - High Memory"),
    API("API Restart"),
    UNKNOWN("Unknown");

    private final String displayName;

    RestartReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
