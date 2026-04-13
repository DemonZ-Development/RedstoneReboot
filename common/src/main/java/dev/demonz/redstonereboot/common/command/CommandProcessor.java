package dev.demonz.redstonereboot.common.command;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.backend.EnvironmentDetector;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shared command processing logic for all platforms.
 */
public class CommandProcessor {

    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RedstoneRebootCore core;

    public CommandProcessor(RedstoneRebootCore core) {
        this.core = core;
    }

    public void processStatus(CommandSender sender) {
        RestartManager rm = core.getRestartManager();
        sender.sendMessage("\u00A76=== RedstoneReboot Status ===");
        sender.sendMessage("\u00A77Version: \u00A7f" + RedstoneRebootCore.VERSION);
        sender.sendMessage("\u00A77Platform: \u00A7f" + core.getPlatform().getPlatformName());

        if (rm.isRestartInProgress()) {
            sender.sendMessage("\u00A7cStatus: \u00A7lRestart in progress \u00A7r\u00A77(\u00A7e"
                + rm.getSecondsUntilRestart() + "s remaining\u00A77)");
            sender.sendMessage("\u00A77Reason: \u00A7f" + rm.getCurrentRestartReason().getDisplayName());
        } else {
            sender.sendMessage("\u00A7aStatus: \u00A7fNormal operation");
        }

        if (rm.getNextScheduledRestart() != null) {
            sender.sendMessage("\u00A7bNext: \u00A7f"
                + rm.getNextScheduledRestart().format(STATUS_TIME_FORMAT)
                + " "
                + core.getConfig().getTimezone());
        }
    }

    public void processReload(CommandSender sender) {
        core.reloadRuntimeState();
        sender.sendMessage("\u00A7aCore engine re-initialized with refreshed backend settings.");
    }

    public void processNow(CommandSender sender, int delay) {
        boolean scheduled = core.getRestartManager().scheduleRestart(delay, RestartReason.MANUAL, sender.getName());
        if (scheduled) {
            sender.sendMessage("\u00A7aRestart triggered by " + sender.getName() + " in " + delay + "s.");
        } else {
            sender.sendMessage("\u00A7eA sooner restart is already in progress.");
        }
    }

    public void processSchedule(CommandSender sender, int delay) {
        boolean scheduled = core.getRestartManager().scheduleRestart(delay, RestartReason.SCHEDULED_API, sender.getName());
        if (scheduled) {
            sender.sendMessage("\u00A7aManual restart scheduled in " + delay + "s.");
        } else {
            sender.sendMessage("\u00A7eA sooner restart is already in progress.");
        }
    }

    public void processCancel(CommandSender sender) {
        boolean cancelled = core.getRestartManager().cancelRestart();
        if (cancelled) {
            sender.sendMessage("\u00A7aRestart cancelled.");
        } else {
            sender.sendMessage("\u00A7eNo restart pending.");
        }
    }

    public void processInfo(CommandSender sender) {
        sender.sendMessage("\u00A76=== Server Performance ===");
        sender.sendMessage("\u00A77Platform: \u00A7f" + core.getPlatform().getPlatformName());
        sender.sendMessage("\u00A77TPS: \u00A7f" + String.format("%.1f", core.getPlatform().getTPS()));

        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0;
        sender.sendMessage("\u00A77Memory: \u00A7f" + String.format("%.1f%%", memoryUsage));
        sender.sendMessage("\u00A77Players: \u00A7f" + core.getPlatform().getOnlinePlayerCount());

        RestartManager rm = core.getRestartManager();
        if (rm.isRestartInProgress()) {
            sender.sendMessage("\u00A7cStatus: \u00A7lRestart in progress \u00A7r\u00A77(\u00A7e"
                + rm.getSecondsUntilRestart() + "s\u00A77)");
        } else {
            sender.sendMessage("\u00A7aStatus: \u00A7fNormal operation");
        }
    }

    public void processHelp(CommandSender sender) {
        sender.sendMessage("\u00A76=== RedstoneReboot Commands ===");
        sender.sendMessage("\u00A77/reboot status \u00A78- \u00A7fView restart status");
        sender.sendMessage("\u00A77/reboot info \u00A78- \u00A7fServer performance");
        sender.sendMessage("\u00A77/reboot doctor \u00A78- \u00A7fSystem diagnostics");
        sender.sendMessage("\u00A77/reboot now [delay] \u00A78- \u00A7fRestart now");
        sender.sendMessage("\u00A77/reboot schedule <seconds> \u00A78- \u00A7fSchedule restart");
        sender.sendMessage("\u00A77/reboot cancel \u00A78- \u00A7fCancel restart");
        sender.sendMessage("\u00A77/reboot reload \u00A78- \u00A7fReload config");
        sender.sendMessage("\u00A77/reboot help \u00A78- \u00A7fShow this menu");
    }

    public void processDoctor(CommandSender sender) {
        sender.sendMessage("\u00A76=== RedstoneReboot Diagnostics ===");

        RestartBackend backend = core.getBackendRegistry().getActiveBackend();
        RestartBackend.BackendState state = backend.getState();

        sender.sendMessage("\u00A77Active Backend: \u00A7b" + backend.getName());

        String stateColor = "\u00A7a";
        if (state == RestartBackend.BackendState.MISCONFIGURED) {
            stateColor = "\u00A7c";
        } else if (state == RestartBackend.BackendState.GENERATED || state == RestartBackend.BackendState.ASSISTED) {
            stateColor = "\u00A7e";
        }

        sender.sendMessage("\u00A77Backend State: " + stateColor + "\u00A7l" + state.name());

        if (state == RestartBackend.BackendState.GENERATED) {
            sender.sendMessage("\u00A7e[!] Script generated, but no 'Wired' proof found.");
            sender.sendMessage("\u00A7e    Add \u00A7f-Dredstonereboot.active=true \u00A7eto startup.");
        } else if (state == RestartBackend.BackendState.SHUTDOWN_ONLY) {
            sender.sendMessage("\u00A77[i] No automated restart active. Graceful stop only.");
        }

        List<String> detected = EnvironmentDetector.detectPotentialBackends();
        if (!detected.isEmpty()) {
            sender.sendMessage("\u00A77Detected Env: \u00A7f" + String.join(", ", detected));
            if (!detected.contains(backend.getName().toUpperCase())
                && !backend.getName().equals("ShutdownOnly")
                && !backend.getName().equals("LocalScript")) {
                sender.sendMessage("\u00A7c[!] Potential Mismatch: Backend vs Environment.");
            }
        } else {
            sender.sendMessage("\u00A77Detected Env: \u00A7fGeneric VPS/Local");
        }

        RestartManager rm = core.getRestartManager();
        if (rm.isLockoutActive()) {
            sender.sendMessage("\u00A7c[!] Lockout Active: New restarts suppressed.");
        }
    }

    /**
     * Platform-neutral command sender abstraction.
     */
    public interface CommandSender {
        void sendMessage(String message);
        String getName();
        boolean hasPermission(String permission);
    }
}
