package dev.demonz.redstonereboot.common.platform;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mutable implementation of {@link PlatformConfig} that allows mod platforms
 * to inject values parsed from their own configuration formats (JSON/TOML).
 */
public class SimplePlatformConfig implements PlatformConfig {

    private boolean scheduledRestartsEnabled = false;
    private List<String> scheduledTimes = new ArrayList<>();
    private List<String> scheduledDays = new ArrayList<>(Collections.singletonList("ALL"));
    private String timezone = "UTC";
    private int scheduledWarningTime = 300;
    private List<Integer> warningTimes = new ArrayList<>(List.of(300, 60, 30, 10, 5));
    private boolean alertsEnabled = true;

    private boolean monitoringEnabled = false;
    private double tpsThreshold = 18.0;
    private double memoryThreshold = 85.0;
    private int checkInterval = 30;
    private int consecutiveChecks = 3;

    private boolean emergencyRestartEnabled = false;
    private double emergencyTpsThreshold = 12.0;
    private double emergencyMemoryThreshold = 95.0;
    private int emergencyDelay = 30;
    private int shutdownDelayTicks = 60;
    private boolean useOpAsAdminEnabled = true;
    private int defaultPermissionLevel = 2;

    @Override public boolean isScheduledRestartsEnabled() { return scheduledRestartsEnabled; }
    @Override public List<String> getScheduledTimes() { return scheduledTimes; }
    @Override public List<String> getScheduledDays() { return scheduledDays; }
    @Override public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
    @Override public String getTimezone() { return timezone; }
    @Override public int getScheduledWarningTime() { return scheduledWarningTime; }
    @Override public List<Integer> getWarningTimes() { return warningTimes; }
    @Override public boolean isAlertsEnabled() { return alertsEnabled; }
    @Override public boolean isMonitoringEnabled() { return monitoringEnabled; }
    @Override public double getTpsThreshold() { return tpsThreshold; }
    @Override public double getMemoryThreshold() { return memoryThreshold; }
    @Override public int getCheckInterval() { return checkInterval; }
    @Override public int getConsecutiveChecks() { return consecutiveChecks; }
    @Override public boolean isEmergencyRestartEnabled() { return emergencyRestartEnabled; }
    @Override public double getEmergencyTpsThreshold() { return emergencyTpsThreshold; }
    @Override public double getEmergencyMemoryThreshold() { return emergencyMemoryThreshold; }
    @Override public int getEmergencyDelay() { return emergencyDelay; }
    @Override public int getShutdownDelayTicks() { return shutdownDelayTicks; }
    @Override public boolean isUseOpAsAdminEnabled() { return useOpAsAdminEnabled; }
    @Override public int getDefaultPermissionLevel() { return defaultPermissionLevel; }

    // Setters for external injection
    public void setScheduledRestartsEnabled(boolean enabled) { this.scheduledRestartsEnabled = enabled; }
    public void setScheduledTimes(List<String> times) { this.scheduledTimes = times; }
    public void setScheduledDays(List<String> days) { this.scheduledDays = days; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public void setScheduledWarningTime(int warningTime) { this.scheduledWarningTime = warningTime; }
    public void setWarningTimes(List<Integer> warningTimes) { this.warningTimes = warningTimes; }
    public void setAlertsEnabled(boolean alertsEnabled) { this.alertsEnabled = alertsEnabled; }
    public void setMonitoringEnabled(boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }
    public void setTpsThreshold(double tpsThreshold) { this.tpsThreshold = tpsThreshold; }
    public void setMemoryThreshold(double memoryThreshold) { this.memoryThreshold = memoryThreshold; }
    public void setCheckInterval(int checkInterval) { this.checkInterval = checkInterval; }
    public void setConsecutiveChecks(int consecutiveChecks) { this.consecutiveChecks = consecutiveChecks; }
    public void setEmergencyRestartEnabled(boolean enabled) { this.emergencyRestartEnabled = enabled; }
    public void setEmergencyTpsThreshold(double threshold) { this.emergencyTpsThreshold = threshold; }
    public void setEmergencyMemoryThreshold(double threshold) { this.emergencyMemoryThreshold = threshold; }
    public void setEmergencyDelay(int seconds) { this.emergencyDelay = seconds; }
    public void setShutdownDelayTicks(int ticks) { this.shutdownDelayTicks = ticks; }
    public void setUseOpAsAdminEnabled(boolean enabled) { this.useOpAsAdminEnabled = enabled; }
    public void setDefaultPermissionLevel(int level) { this.defaultPermissionLevel = level; }
}
