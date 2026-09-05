package dev.demonz.redstonereboot.modern;

import com.mojang.brigadier.CommandDispatcher;
import dev.demonz.redstonereboot.common.command.BrigadierCommand;
import dev.demonz.redstonereboot.common.command.CommandProcessor;
import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.JavaPlatformScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.nio.file.Path;
import java.util.logging.Logger;

public abstract class ModernServerPlatform extends AbstractBootstrapServerPlatform {
    private volatile MinecraftServer server;

    protected ModernServerPlatform(String platform, String minecraftVersion, Path configDir) {
        super(Logger.getLogger("RedstoneReboot/" + platform), platform, minecraftVersion);
        Path directory = configDir.resolve("redstonereboot");
        startCore(new JavaPlatformScheduler(this::dispatch), loadSimpleConfig(directory.resolve("redstonereboot.properties")), directory);
        registerShutdownHook("RedstoneReboot-" + platform + "-Shutdown");
    }

    protected final void started(MinecraftServer server) {
        this.server = server;
        core.onEnable();
        startPlatformMonitoring();
    }

    protected final void stopping() {
        stopCore();
        server = null;
    }

    protected final void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        new BrigadierCommand(core).register(dispatcher, source -> new Sender((CommandSourceStack) source));
        getLogger().info("RedstoneReboot command registered.");
    }

    private void dispatch(Runnable task) {
        MinecraftServer current = server;
        if (current != null) current.execute(task);
    }

    @Override
    public void broadcastMessage(String message) {
        MinecraftServer current = server;
        if (current != null) current.getPlayerList().broadcastSystemMessage(parseLegacyComponent(message), false);
        getLogger().info("[broadcast] " + LegacyTextUtil.stripLegacyFormatting(message));
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        MinecraftServer current = server;
        if (current == null) return;
        Component heading = parseLegacyComponent(title);
        Component subheading = parseLegacyComponent(subtitle);
        for (var player : current.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 10));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subheading));
            player.connection.send(new ClientboundSetTitleTextPacket(heading));
        }
    }

    @Override
    public void broadcastActionBar(String message) {
        MinecraftServer current = server;
        if (current == null) return;
        Component text = parseLegacyComponent(message);
        for (var player : current.getPlayerList().getPlayers()) player.sendSystemMessage(text, true);
    }

    @Override
    public void executeConsole(String command) {
        MinecraftServer current = server;
        if (current != null) current.getCommands().performPrefixedCommand(current.createCommandSourceStack(), command);
    }

    @Override
    public double getTPS() {
        MinecraftServer current = server;
        if (current == null) return 20.0;
        long nanos = current.getAverageTickTimeNanos();
        return nanos <= 0 ? 20.0 : Math.min(20.0, 1_000_000_000.0 / nanos);
    }

    @Override
    public int getOnlinePlayerCount() {
        MinecraftServer current = server;
        return current == null ? 0 : current.getPlayerCount();
    }

    @Override
    public void shutdownServer() {
        MinecraftServer current = server;
        if (current != null) current.execute(() -> current.halt(false));
    }

    private Component parseLegacyComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        net.minecraft.network.chat.MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        java.util.List<net.minecraft.ChatFormatting> formats = new java.util.ArrayList<>();
        net.minecraft.network.chat.TextColor activeColor = null;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 13 < text.length() && text.charAt(i + 1) == 'x') {
                boolean isHex = true;
                StringBuilder hex = new StringBuilder("#");
                for (int j = 0; j < 6; j++) {
                    if (text.charAt(i + 2 + j * 2) != '\u00A7') {
                        isHex = false;
                        break;
                    }
                    hex.append(text.charAt(i + 3 + j * 2));
                }
                if (isHex) {
                    if (currentText.length() > 0) {
                        net.minecraft.network.chat.MutableComponent part = Component.literal(currentText.toString()).withStyle(formats.toArray(new net.minecraft.ChatFormatting[0]));
                        if (activeColor != null) {
                            part.setStyle(part.getStyle().withColor(activeColor));
                        }
                        result.append(part);
                        currentText.setLength(0);
                    }
                    activeColor = net.minecraft.network.chat.TextColor.parseColor(hex.toString()).result().orElse(null);
                    formats.clear();
                    i += 13;
                    continue;
                }
            }

            if (c == '\u00A7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                net.minecraft.ChatFormatting format = net.minecraft.ChatFormatting.getByCode(code);
                if (format != null) {
                    if (currentText.length() > 0) {
                        net.minecraft.network.chat.MutableComponent part = Component.literal(currentText.toString()).withStyle(formats.toArray(new net.minecraft.ChatFormatting[0]));
                        if (activeColor != null) {
                            part.setStyle(part.getStyle().withColor(activeColor));
                        }
                        result.append(part);
                        currentText.setLength(0);
                    }
                    if ("0123456789abcdef".indexOf(Character.toLowerCase(code)) >= 0 || format == net.minecraft.ChatFormatting.RESET) {
                        formats.clear();
                        activeColor = null;
                    }
                    formats.add(format);
                    i++;
                } else {
                    currentText.append(c);
                }
            } else {
                currentText.append(c);
            }
        }
        if (currentText.length() > 0) {
            net.minecraft.network.chat.MutableComponent part = Component.literal(currentText.toString()).withStyle(formats.toArray(new net.minecraft.ChatFormatting[0]));
            if (activeColor != null) {
                part.setStyle(part.getStyle().withColor(activeColor));
            }
            result.append(part);
        }
        return result;
    }

    private final class Sender implements CommandProcessor.CommandSender {
        private final CommandSourceStack source;

        private Sender(CommandSourceStack source) {
            this.source = source;
        }

        public void sendMessage(String message) {
            source.sendSystemMessage(parseLegacyComponent(message));
        }

        public String getName() {
            return source.getTextName();
        }

        public boolean hasPermission(String permission) {
            if (permission == null || core == null) return false;
            var config = core.getConfig();
            if (CommandProcessor.isPublicPermission(permission) && config.isPublicPermissionsEnabled()) return true;
            boolean owner = hasLevel(4);
            if (config.isUseOpAsAdminEnabled() && owner) return true;
            boolean admin = permission.startsWith("redstonereboot.restart.") || permission.contains(".reload")
                || permission.contains(".doctor") || permission.contains(".dump");
            return admin ? owner : hasLevel(config.getDefaultPermissionLevel());
        }

        private boolean hasLevel(int level) {
            return level <= 0 || source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
        }
    }
}
