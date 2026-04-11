package dev.demonz.redstonereboot.common.command;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;

import java.time.format.DateTimeFormatter;
import java.util.Map;

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
        sender.sendMessage("§6=== RedstoneReboot Status ===");
        sender.sendMessage("§7Version: §f" + RedstoneRebootCore.VERSION);
        sender.sendMessage("§7Platform: §f" + core.getPlatform().getPlatformName());

        if (rm.isRestartInProgress()) {
            sender.sendMessage("§cStatus: §lRestart in progress §r§7(§e" + rm.getSecondsUntilRestart() + "s remaining§7)");
            sender.sendMessage("§7Reason: §f" + rm.getCurrentRestartReason().getDisplayName());
        } else {
            sender.sendMessage("§aStatus: §fNormal operation");
        }

        if (rm.getNextScheduledRestart() != null) {
            sender.sendMessage("§bNext: §f" + rm.getNextScheduledRestart().format(STATUS_TIME_FORMAT) + " " + core.getConfig().getTimezone());
        }
    }

    public void processReload(CommandSender sender) {
        // Ensure platform has fetched the latest file from disk beforehand
        core.getRestartManager().initialize();
        sender.sendMessage("§aCore engine re-initialized with new settings.");
    }

    public void processNow(CommandSender sender, int delay) {
        boolean scheduled = core.getRestartManager().scheduleRestart(delay, RestartReason.MANUAL, sender.getName());
        if (scheduled) {
            sender.sendMessage("§aRestart triggered by " + sender.getName() + " in " + delay + "s.");
        } else {
            sender.sendMessage("§eA sooner restart is already in progress.");
        }
    }

    public void processSchedule(CommandSender sender, int delay) {
        boolean scheduled = core.getRestartManager().scheduleRestart(delay, RestartReason.SCHEDULED_API, sender.getName());
        if (scheduled) {
            sender.sendMessage("§aManual restart scheduled in " + delay + "s.");
        } else {
            sender.sendMessage("§eA sooner restart is already in progress.");
        }
    }

    public void processCancel(CommandSender sender) {
        boolean cancelled = core.getRestartManager().cancelRestart();
        if (cancelled) {
            sender.sendMessage("§aRestart cancelled.");
        } else {
            sender.sendMessage("§eNo restart pending.");
        }
    }

    public void processInfo(CommandSender sender) {
        sender.sendMessage("§6=== Server Performance ===");
        sender.sendMessage("§7Platform: §f" + core.getPlatform().getPlatformName());
        sender.sendMessage("§7TPS: §f" + String.format("%.1f", core.getPlatform().getTPS()));

        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100.0;
        sender.sendMessage("§7Memory: §f" + String.format("%.1f%%", memoryUsage));
        sender.sendMessage("§7Players: §f" + core.getPlatform().getOnlinePlayerCount());

        RestartManager rm = core.getRestartManager();
        if (rm.isRestartInProgress()) {
            sender.sendMessage("§cStatus: §lRestart in progress §r§7(§e" + rm.getSecondsUntilRestart() + "s§7)");
        } else {
            sender.sendMessage("§aStatus: §fNormal operation");
        }
    }

    public void processHelp(CommandSender sender) {
        sender.sendMessage("§6=== RedstoneReboot Commands ===");
        sender.sendMessage("§7/reboot status §8- §fView restart status");
        sender.sendMessage("§7/reboot info §8- §fServer performance");
        sender.sendMessage("§7/reboot now [delay] §8- §fRestart now");
        sender.sendMessage("§7/reboot schedule <seconds> §8- §fSchedule restart");
        sender.sendMessage("§7/reboot cancel §8- §fCancel restart");
        sender.sendMessage("§7/reboot reload §8- §fReload config");
        sender.sendMessage("§7/reboot help §8- §fShow this menu");
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
