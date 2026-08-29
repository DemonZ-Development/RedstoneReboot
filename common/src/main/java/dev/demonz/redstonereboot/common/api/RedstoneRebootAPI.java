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

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Hook into RedstoneReboot from any plugin/mod.
 * Grab it after startup: RedstoneRebootAPI.getInstance() — null if not loaded.
 * Works on Paper, Folia, Fabric, Forge, NeoForge. Use registerListener / registerMessageAdapter to react.
 */
public final class RedstoneRebootAPI {

    private static volatile RedstoneRebootAPI instance;

    private final RedstoneRebootCore core;
    private final Logger logger;
    private static final CopyOnWriteArrayList<RestartListener> listeners = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<MessageAdapter> adapters = new CopyOnWriteArrayList<>();

    public RedstoneRebootAPI(RedstoneRebootCore core) {
        this.core = core;
        this.logger = Logger.getLogger("RedstoneRebootAPI");
    }

    /**
     * @return the singleton API instance, or {@code null} if RedstoneReboot is not enabled
     */
    public static RedstoneRebootAPI getInstance() {
        return instance;
    }

    /**
     * @return true if the API is available
     */
    public static boolean isAvailable() {
        return instance != null;
    }

    public static void setInstance(RedstoneRebootAPI api) {
        instance = api;
    }

    public static void clearInstance() {
        instance = null;
    }

    public RedstoneRebootCore getCore() { return core; }

    public RestartManager getRestartManager() { return core != null ? core.getRestartManager() : null; }

    public String getVersion() { return core != null ? core.getVersion() : RedstoneRebootCore.VERSION; }

    // ---- Restart control ----

    public boolean scheduleRestart(int delaySeconds, RestartReason reason, String initiator) {
        RestartManager rm = getRestartManager();
        if (rm == null) return false;
        return rm.scheduleRestart(delaySeconds, reason, initiator);
    }

    public boolean cancelRestart() {
        RestartManager rm = getRestartManager();
        if (rm == null) return false;
        return rm.cancelRestart();
    }

    public int getSecondsUntilRestart() {
        RestartManager rm = getRestartManager();
        return rm != null ? rm.getSecondsUntilRestart() : -1;
    }

    public boolean isRestartInProgress() {
        RestartManager rm = getRestartManager();
        return rm != null && rm.isRestartInProgress();
    }

    // ---- Listeners ----

    public void registerListener(RestartListener listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    public void unregisterListener(RestartListener listener) {
        listeners.remove(listener);
    }

    public List<RestartListener> getListeners() {
        return List.copyOf(listeners);
    }

    // ---- Message adapters (Discord etc.) ----

    public void registerMessageAdapter(MessageAdapter adapter) {
        if (adapter != null) adapters.addIfAbsent(adapter);
    }

    public void unregisterMessageAdapter(MessageAdapter adapter) {
        adapters.remove(adapter);
    }

    public void registerDiscordWebhook(String webhookUrl) {
        registerDiscordWebhook(webhookUrl, "RedstoneReboot");
    }

    public void registerDiscordWebhook(String webhookUrl, String username) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            logger.warning("Discord webhook URL is blank - not registered");
            return;
        }
        registerMessageAdapter(new DiscordWebhookAdapter(logger, webhookUrl, username));
        logger.info("Registered Discord webhook adapter");
    }

    public List<MessageAdapter> getMessageAdapters() {
        return List.copyOf(adapters);
    }

    // ---- Internal dispatch ----

    public void fireScheduled(int seconds, RestartReason reason, String initiator) {
        for (RestartListener l : listeners) {
            try { l.onRestartScheduled(seconds, reason, initiator); } catch (Exception e) { logger.warning("RestartListener onScheduled error: " + e.getMessage()); }
        }
    }

    public void fireCancelled(String prevReason, String prevInitiator) {
        for (RestartListener l : listeners) {
            try { l.onRestartCancelled(prevReason, prevInitiator); } catch (Exception e) { logger.warning("RestartListener onCancelled error: " + e.getMessage()); }
        }
    }

    public void fireExecuting(RestartReason reason, String initiator) {
        for (RestartListener l : listeners) {
            try { l.onRestartExecuting(reason, initiator); } catch (Exception e) { logger.warning("RestartListener onExecuting error: " + e.getMessage()); }
        }
    }

    public void fireFailed(String detail) {
        for (RestartListener l : listeners) {
            try { l.onRestartFailed(detail); } catch (Exception e) { logger.warning("RestartListener onFailed error: " + e.getMessage()); }
        }
    }

    public void fireLockout(int duration) {
        for (RestartListener l : listeners) {
            try { l.onLockoutStarted(duration); } catch (Exception e) { logger.warning("RestartListener onLockout error: " + e.getMessage()); }
        }
    }

    public void fireEmergency(String emergencyReason, RestartReason restartReason) {
        for (RestartListener l : listeners) {
            try { l.onEmergencyTriggered(emergencyReason, restartReason); } catch (Exception e) { logger.warning("RestartListener onEmergency error: " + e.getMessage()); }
        }
    }

    public void dispatchMessage(MessageContext context) {
        for (MessageAdapter a : adapters) {
            try {
                // allow adapters to filter by type if needed
                a.onMessage(context);
            } catch (Exception e) {
                logger.warning("MessageAdapter " + a.getName() + " error: " + e.getMessage());
            }
        }
    }
}
