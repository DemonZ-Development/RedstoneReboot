package dev.demonz.redstonereboot.bukkit.commands;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.bukkit.managers.RestartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Main command handler for /reboot.
 */
public class RebootCommand implements CommandExecutor, TabCompleter {
    private final RedstoneRebootPlugin plugin;

    public RebootCommand(RedstoneRebootPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "now": return handleNow(sender, args);
            case "schedule": return handleSchedule(sender, args);
            case "cancel": return handleCancel(sender);
            case "status": return handleStatus(sender);
            case "info": return handleInfo(sender);
            case "reload": return handleReload(sender);
            case "help": sendHelp(sender); return true;
            default: msg(sender, "Unknown subcommand. Use '/reboot help'.", NamedTextColor.RED); return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> comp = new ArrayList<>();
        if (args.length == 1) {
            for (String c : Arrays.asList("now", "schedule", "cancel", "status", "info", "reload", "help"))
                if (c.startsWith(args[0].toLowerCase())) comp.add(c);
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("now") || args[0].equalsIgnoreCase("schedule"))) {
            comp.addAll(Arrays.asList("30", "60", "120", "300", "600"));
        }
        return comp;
    }

    private boolean handleNow(CommandSender sender, String[] args) {
        if (sender instanceof Player && !plugin.getPermissionManager().canRestartNow((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        int delay = 60;
        if (args.length > 1) try { delay = Math.min(Math.max(Integer.parseInt(args[1]), 0), 3600); }
        catch (NumberFormatException e) { msg(sender, "Invalid delay.", NamedTextColor.RED); return true; }
        if (plugin.getRestartManager().isRestartInProgress()) {
            msg(sender, "A restart is already in progress.", NamedTextColor.YELLOW); return true;
        }
        plugin.getRestartManager().scheduleRestart(delay, RestartManager.RestartReason.MANUAL, sender.getName());
        msg(sender, "Server restart in " + delay + "s.", NamedTextColor.GREEN);
        return true;
    }

    private boolean handleSchedule(CommandSender sender, String[] args) {
        if (sender instanceof Player && !plugin.getPermissionManager().canScheduleRestart((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        if (args.length < 2) { msg(sender, "Usage: /reboot schedule <seconds>", NamedTextColor.RED); return true; }
        try {
            int delay = Integer.parseInt(args[1]);
            if (delay <= 0 || delay > 86400) { msg(sender, "Delay must be 1-86400.", NamedTextColor.RED); return true; }
            if (plugin.getRestartManager().isRestartInProgress()) {
                msg(sender, "Restart already in progress.", NamedTextColor.YELLOW); return true;
            }
            plugin.getRestartManager().scheduleRestart(delay, RestartManager.RestartReason.MANUAL, sender.getName());
            msg(sender, "Restart scheduled in " + delay + "s.", NamedTextColor.GREEN);
        } catch (NumberFormatException e) { msg(sender, "Invalid number.", NamedTextColor.RED); }
        return true;
    }

    private boolean handleCancel(CommandSender sender) {
        if (sender instanceof Player && !plugin.getPermissionManager().canCancelRestart((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        msg(sender, plugin.getRestartManager().cancelRestart() ? "Restart cancelled." : "No restart pending.", 
            plugin.getRestartManager().isRestartInProgress() ? NamedTextColor.YELLOW : NamedTextColor.GREEN);
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (sender instanceof Player && !plugin.getPermissionManager().canViewStatus((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        msg(sender, "═══ RedstoneReboot Status ═══", NamedTextColor.GOLD);
        msg(sender, "Version: " + plugin.getDescription().getVersion(), NamedTextColor.GRAY);
        msg(sender, "Timezone: " + plugin.getConfigManager().getTimezone(), NamedTextColor.GRAY);
        if (plugin.getRestartManager().isRestartInProgress()) {
            msg(sender, "Status: §cRestart in progress", NamedTextColor.RED);
            msg(sender, "Reason: " + plugin.getRestartManager().getCurrentRestartReason().getDisplayName(), NamedTextColor.GRAY);
        } else {
            msg(sender, "Status: §aNormal operation", NamedTextColor.GREEN);
        }
        if (plugin.getRestartManager().getNextScheduledRestart() != null) {
            msg(sender, "Next: " + plugin.getRestartManager().getNextScheduledRestart()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + plugin.getConfigManager().getTimezone(), NamedTextColor.AQUA);
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (sender instanceof Player && !plugin.getPermissionManager().canViewStatus((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        msg(sender, "═══ Server Performance ═══", NamedTextColor.GOLD);
        if (plugin.getServerLoadMonitor() != null) {
            msg(sender, String.format("TPS: %.1f", plugin.getServerLoadMonitor().getLastTPS()), NamedTextColor.YELLOW);
            msg(sender, String.format("Memory: %.1f%%", plugin.getServerLoadMonitor().getLastMemoryUsage()), NamedTextColor.YELLOW);
            msg(sender, "Health: " + (plugin.getServerLoadMonitor().isHealthy() ? "§aHEALTHY" : "§cPOOR"), NamedTextColor.WHITE);
        } else {
            msg(sender, "Monitoring: §7DISABLED", NamedTextColor.GRAY);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (sender instanceof Player && !plugin.getPermissionManager().canReloadConfig((Player) sender)) {
            msg(sender, "No permission.", NamedTextColor.RED); return true;
        }
        try {
            plugin.getConfigManager().reloadConfig();
            plugin.getRestartManager().cleanup();
            if (plugin.getConfigManager().isScheduledRestartsEnabled()) plugin.getRestartManager().initialize();
            msg(sender, "Configuration reloaded!", NamedTextColor.GREEN);
        } catch (Exception e) {
            msg(sender, "Reload error: " + e.getMessage(), NamedTextColor.RED);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "═══ RedstoneReboot Commands ═══", NamedTextColor.GOLD);
        msg(sender, "/reboot status — View restart status", NamedTextColor.GRAY);
        msg(sender, "/reboot info — Server performance", NamedTextColor.GRAY);
        msg(sender, "/reboot now [delay] — Restart now", NamedTextColor.GRAY);
        msg(sender, "/reboot schedule <seconds> — Schedule restart", NamedTextColor.GRAY);
        msg(sender, "/reboot cancel — Cancel restart", NamedTextColor.GRAY);
        msg(sender, "/reboot reload — Reload config", NamedTextColor.GRAY);
    }

    private void msg(CommandSender sender, String text, NamedTextColor color) {
        Component c = Component.text()
            .append(LegacyComponentSerializer.legacySection().deserialize(plugin.getConfigManager().getPrefix()))
            .append(Component.space())
            .append(Component.text(text, color))
            .build();
        if (plugin.getAdventure() != null) {
            plugin.getAdventure().sender(sender).sendMessage(c);
        }
    }
}
