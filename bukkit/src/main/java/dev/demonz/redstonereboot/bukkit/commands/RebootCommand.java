package dev.demonz.redstonereboot.bukkit.commands;

import dev.demonz.redstonereboot.bukkit.RedstoneRebootPlugin;
import dev.demonz.redstonereboot.common.command.CommandProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main command handler for /reboot.
 */
public class RebootCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final RedstoneRebootPlugin plugin;
    private final CommandProcessor processor;

    public RebootCommand(RedstoneRebootPlugin plugin) {
        this.plugin = plugin;
        this.processor = new CommandProcessor(plugin.getCore());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        BukkitSender wrapper = new BukkitSender(sender);

        return switch (args[0].toLowerCase()) {
            case "now" -> handleNow(sender, args);
            case "schedule" -> handleSchedule(sender, args);
            case "cancel" -> handleCancel(sender);
            case "status" -> {
                processor.processStatus(wrapper);
                yield true;
            }
            case "info" -> handleInfo(sender);
            case "reload" -> handleReload(sender);
            case "help" -> {
                sendHelp(sender);
                yield true;
            }
            default -> {
                msg(sender, "Unknown subcommand. Use '/reboot help'.", NamedTextColor.RED);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String candidate : Arrays.asList("now", "schedule", "cancel", "status", "info", "reload", "help")) {
                if (candidate.startsWith(args[0].toLowerCase())) {
                    completions.add(candidate);
                }
            }
        } else if (args.length == 2
            && (args[0].equalsIgnoreCase("now") || args[0].equalsIgnoreCase("schedule"))) {
            completions.addAll(Arrays.asList("30", "60", "120", "300", "600"));
        }
        return completions;
    }

    private boolean handleNow(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !plugin.getPermissionManager().canRestartNow(player)) {
            msg(sender, "No permission.", NamedTextColor.RED);
            return true;
        }

        int delay = 60;
        if (args.length > 1) {
            try {
                delay = Integer.parseInt(args[1]);
                if (delay < 0 || delay > 3600) {
                    msg(sender, "Delay must be between 0 and 3600 seconds.", NamedTextColor.RED);
                    return true;
                }
            } catch (NumberFormatException exception) {
                msg(sender, "Invalid delay.", NamedTextColor.RED);
                return true;
            }
        }

        processor.processNow(new BukkitSender(sender), delay);
        return true;
    }

    private boolean handleSchedule(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !plugin.getPermissionManager().canScheduleRestart(player)) {
            msg(sender, "No permission.", NamedTextColor.RED);
            return true;
        }

        if (args.length < 2) {
            msg(sender, "Usage: /reboot schedule <seconds>", NamedTextColor.RED);
            return true;
        }

        try {
            int delay = Integer.parseInt(args[1]);
            if (delay < 1 || delay > 86400) {
                msg(sender, "Delay must be between 1 and 86400 seconds.", NamedTextColor.RED);
                return true;
            }
            processor.processSchedule(new BukkitSender(sender), delay);
        } catch (NumberFormatException exception) {
            msg(sender, "Invalid number.", NamedTextColor.RED);
        }
        return true;
    }

    private boolean handleCancel(CommandSender sender) {
        if (sender instanceof Player player && !plugin.getPermissionManager().canCancelRestart(player)) {
            msg(sender, "No permission.", NamedTextColor.RED);
            return true;
        }

        processor.processCancel(new BukkitSender(sender));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (sender instanceof Player player && !plugin.getPermissionManager().canViewStatus(player)) {
            msg(sender, "No permission.", NamedTextColor.RED);
            return true;
        }

        msg(sender, "=== Server Performance ===", NamedTextColor.GOLD);
        if (plugin.getServerLoadMonitor() != null) {
            msg(sender, String.format("TPS: %.1f", plugin.getServerLoadMonitor().getLastTPS()), NamedTextColor.YELLOW);
            msg(sender,
                String.format("Memory: %.1f%%", plugin.getServerLoadMonitor().getLastMemoryUsage()),
                NamedTextColor.YELLOW);
            msg(sender,
                "Health: " + (plugin.getServerLoadMonitor().isHealthy() ? "HEALTHY" : "POOR"),
                plugin.getServerLoadMonitor().isHealthy() ? NamedTextColor.GREEN : NamedTextColor.RED);
        } else {
            msg(sender, "Monitoring: DISABLED", NamedTextColor.GRAY);
        }
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (sender instanceof Player player && !plugin.getPermissionManager().canReloadConfig(player)) {
            msg(sender, "No permission.", NamedTextColor.RED);
            return true;
        }

        try {
            plugin.reloadPluginState();
            msg(sender, "Configuration reloaded.", NamedTextColor.GREEN);
        } catch (Exception exception) {
            msg(sender, "Reload error: " + exception.getMessage(), NamedTextColor.RED);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, "=== RedstoneReboot Commands ===", NamedTextColor.GOLD);
        msg(sender, "/reboot status - View restart status", NamedTextColor.GRAY);
        msg(sender, "/reboot info - Server performance", NamedTextColor.GRAY);
        msg(sender, "/reboot now [delay] - Restart now", NamedTextColor.GRAY);
        msg(sender, "/reboot schedule <seconds> - Schedule restart", NamedTextColor.GRAY);
        msg(sender, "/reboot cancel - Cancel restart", NamedTextColor.GRAY);
        msg(sender, "/reboot reload - Reload config", NamedTextColor.GRAY);
    }

    private void msg(CommandSender sender, String text, NamedTextColor color) {
        Component prefix = LEGACY_SERIALIZER.deserialize(plugin.getConfigManager().getPrefix());
        Component message = prefix.append(Component.space()).append(Component.text(text, color));

        if (plugin.getAdventure() != null) {
            plugin.getAdventure().sender(sender).sendMessage(message);
            return;
        }

        sender.sendMessage(LEGACY_SERIALIZER.serialize(message));
    }

    private class BukkitSender implements CommandProcessor.CommandSender {
        private final CommandSender sender;

        public BukkitSender(CommandSender sender) {
            this.sender = sender;
        }

        @Override
        public void sendMessage(String message) {
            Component prefix = LEGACY_SERIALIZER.deserialize(plugin.getConfigManager().getPrefix());
            Component content = LEGACY_SERIALIZER.deserialize(message);
            Component fullMessage = prefix.append(Component.space()).append(content);
            if (plugin.getAdventure() != null) {
                plugin.getAdventure().sender(sender).sendMessage(fullMessage);
                return;
            }

            sender.sendMessage(LEGACY_SERIALIZER.serialize(fullMessage));
        }

        @Override
        public String getName() {
            return sender.getName();
        }

        @Override
        public boolean hasPermission(String permission) {
            return sender.hasPermission(permission);
        }
    }
}
