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