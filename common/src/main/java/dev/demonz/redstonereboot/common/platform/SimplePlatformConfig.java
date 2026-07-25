/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package dev.demonz.redstonereboot.common.platform;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A mutable implementation of {@link PlatformConfig} that allows mod platforms
 * to inject values parsed from their own configuration formats (JSON/TOML).
 */
public class SimplePlatformConfig implements PlatformConfig {

    private static final Logger LOGGER = Logger.getLogger(SimplePlatformConfig.class.getName());

    private volatile boolean scheduledRestartsEnabled = false;
    private volatile List<String> scheduledTimes = new ArrayList<>();
    private volatile List<String> scheduledDays = new ArrayList<>(Collections.singletonList("ALL"));
    private volatile String timezone = "UTC";
    private volatile int scheduledWarningTime = 300;
    private volatile List<Integer> warningTimes = new ArrayList<>(List.of(300, 60, 30, 10, 5));
    private volatile boolean alertsEnabled = true;

    private volatile boolean monitoringEnabled = false;
    private volatile double tpsThreshold = 18.0;
    private volatile double memoryThreshold = 85.0;
    private volatile int checkInterval = 30;
    private volatile int consecutiveChecks = 3;

    private volatile boolean emergencyRestartEnabled = false;
    private volatile double emergencyTpsThreshold = 12.0;
    private volatile double emergencyMemoryThreshold = 95.0;
    private volatile int emergencyDelay = 30;
    private volatile int shutdownDelayTicks = 60;
    private volatile boolean useOpAsAdminEnabled = true;
    private volatile int defaultPermissionLevel = 2;
    private volatile boolean publicPermissionsEnabled = true;

    @Override public boolean isScheduledRestartsEnabled() { return scheduledRestartsEnabled; }
    @Override public List<String> getScheduledTimes() { return Collections.unmodifiableList(scheduledTimes); }
    @Override public List<String> getScheduledDays() { return Collections.unmodifiableList(scheduledDays); }
    @Override public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            LOGGER.warning("Invalid timezone '" + timezone + "' configured, falling back to UTC");
            return ZoneId.of("UTC");
        }
    }
    @Override public String getTimezone() { return timezone; }
    @Override public int getScheduledWarningTime() { return scheduledWarningTime; }
    @Override public List<Integer> getWarningTimes() { return Collections.unmodifiableList(warningTimes); }
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
    @Override public boolean isPublicPermissionsEnabled() { return publicPermissionsEnabled; }

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
    public void setPublicPermissionsEnabled(boolean enabled) { this.publicPermissionsEnabled = enabled; }

    private volatile String prefix = "§8[§cRedstone§8] §aReboot";
    private volatile boolean chatAlertsEnabled = true;
    private volatile String chatAlertFormat = "§8[§cRedstone§8] §eServer will restart in §c{time}§e!";
    private volatile boolean titleAlertsEnabled = true;
    private volatile String titleMainText = "§c⚡ Server Restart";
    private volatile String titleSubText = "§ein §c{time}";
    private volatile boolean actionBarAlertsEnabled = true;
    private volatile String actionBarFormat = "§8[§cRedstone§8] §eRestart in: §c{time}";

    @Override public String getPrefix() { return prefix; }
    @Override public boolean isChatAlertsEnabled() { return chatAlertsEnabled; }
    @Override public String getChatAlertFormat() { return chatAlertFormat; }
    @Override public boolean isTitleAlertsEnabled() { return titleAlertsEnabled; }
    @Override public String getTitleMainText() { return titleMainText; }
    @Override public String getTitleSubText() { return titleSubText; }
    @Override public boolean isActionBarAlertsEnabled() { return actionBarAlertsEnabled; }
    @Override public String getActionBarFormat() { return actionBarFormat; }

    public void setPrefix(String prefix) { this.prefix = prefix; }
    public void setChatAlertsEnabled(boolean enabled) { this.chatAlertsEnabled = enabled; }
    public void setChatAlertFormat(String format) { this.chatAlertFormat = format; }
    public void setTitleAlertsEnabled(boolean enabled) { this.titleAlertsEnabled = enabled; }
    public void setTitleMainText(String text) { this.titleMainText = text; }
    public void setTitleSubText(String text) { this.titleSubText = text; }
    public void setActionBarAlertsEnabled(boolean enabled) { this.actionBarAlertsEnabled = enabled; }
    public void setActionBarFormat(String format) { this.actionBarFormat = format; }
}