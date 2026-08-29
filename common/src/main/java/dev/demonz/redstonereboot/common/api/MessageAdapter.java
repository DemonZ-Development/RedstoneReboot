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


package dev.demonz.redstonereboot.common.api;

/** Send RedstoneReboot alerts to your own service (Discord, Slack, etc). Register with RedstoneRebootAPI. Thread-safe — may be called async. */
public interface MessageAdapter {

    /**
     * Called for every alert/restart message dispatched.
     *
     * @param context immutable message context
     */
    void onMessage(MessageContext context);

    /**
     * @return human-readable adapter name for diagnostics
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Whether this adapter should receive postponed (failure) messages.
     * Override to filter.
     */
    default boolean handlesPostponed() { return true; }

    /**
     * Whether this adapter should receive regular countdown alerts.
     */
    default boolean handlesAlerts() { return true; }
}
