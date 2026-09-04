package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

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

    public static RedstoneRebootAPI getInstance() {
        return instance;
    }

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


    public void registerListener(RestartListener listener) {
        if (listener != null) listeners.addIfAbsent(listener);
    }

    public void unregisterListener(RestartListener listener) {
        listeners.remove(listener);
    }

    public List<RestartListener> getListeners() {
        return List.copyOf(listeners);
    }


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
        Objects.requireNonNull(context, "context must not be null");
        for (MessageAdapter a : adapters) {
            try {
                if (context.getType() == MessageContext.Type.POSTPONED && !a.handlesPostponed()) {
                    continue;
                }
                if (context.getType() == MessageContext.Type.SCHEDULED_ALERT && !a.handlesAlerts()) {
                    continue;
                }
                a.onMessage(context);
            } catch (Exception e) {
                logger.log(java.util.logging.Level.WARNING, "MessageAdapter " + a.getName() + " failed", e);
            }
        }
    }
}
