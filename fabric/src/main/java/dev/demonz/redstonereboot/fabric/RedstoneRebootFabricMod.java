package dev.demonz.redstonereboot.fabric;

import dev.demonz.redstonereboot.common.command.BrigadierCommand;
import dev.demonz.redstonereboot.common.command.CommandProcessor;
import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.JavaPlatformScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Fabric dedicated-server bootstrap.
 */
public final class RedstoneRebootFabricMod extends AbstractBootstrapServerPlatform implements DedicatedServerModInitializer {

    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_STAY = 60;
    private static final int TITLE_FADE_OUT = 10;

    private JavaPlatformScheduler scheduler;
    private volatile MinecraftServer server;

    public RedstoneRebootFabricMod() {
        super(Logger.getLogger("RedstoneReboot/Fabric"), "Fabric", resolveMinecraftVersion());
        registerShutdownHook("RedstoneReboot-Fabric-Shutdown");
    }

    @Override
    public void onInitializeServer() {
        try {
            scheduler = new JavaPlatformScheduler(this::dispatchToServerThread);
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("redstonereboot.properties");
            startCore(scheduler, loadSimpleConfig(configPath), FabricLoader.getInstance().getConfigDir());

            ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> {
                this.server = startedServer;
                if (core != null) core.onEnable();
            });
            ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
                this.server = stoppingServer;
                stopCore();
            });
            ServerLifecycleEvents.SERVER_STOPPED.register(stoppedServer -> {
                this.server = null;
            });

            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                if (core == null) return;
                new BrigadierCommand(core).register(dispatcher, source -> new FabricSender(this, (ServerCommandSource) source));
                getLogger().info("RedstoneReboot command registered.");
            });

            startPlatformMonitoring();
            getLogger().info("Fabric dedicated-server bootstrap initialized.");
        } catch (Exception exception) {
            getLogger().severe("Failed to initialize RedstoneReboot: " + exception.getMessage());
        }
    }

    @Override
    public void broadcastMessage(String message) {
        if (server != null && server.getPlayerManager() != null) {
            server.getPlayerManager().broadcast(parseLegacyText(message), false);
        }
        getLogger().info("[broadcast] " + LegacyTextUtil.stripLegacyFormatting(message));
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        if (server == null || server.getPlayerManager() == null) {
            getLogger().info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
                + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
            return;
        }
        Text titleText = parseLegacyText(title);
        Text subtitleText = parseLegacyText(subtitle);
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendTitlePackets(player, titleText, subtitleText);
        }
    }

    @Override
    public void broadcastActionBar(String message) {
        if (server != null && server.getPlayerManager() != null) {
            Text text = parseLegacyText(message);
            for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                player.sendMessage(text, true);
            }
        }
    }

    private void sendTitlePackets(net.minecraft.server.network.ServerPlayerEntity player, Text title, Text subtitle) {
        try {
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(subtitle));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(title));
        } catch (Exception exception) {
            getLogger().warning("[title] Failed to send title packet: " + exception.getMessage());
        }
    }

    @Override
    public void executeConsole(String command) {
        if (server != null) {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), command);
        } else {
            getLogger().warning("Cannot execute console command: MinecraftServer is null. Command: " + command);
        }
    }

    @Override
    public double getTPS() {
        return dev.demonz.redstonereboot.common.utils.MinecraftTPSUtil.calculateTPS(server, getLogger());
    }

    @Override
    public int getOnlinePlayerCount() {
        return server != null ? server.getCurrentPlayerCount() : 0;
    }

    @Override
    public void shutdownServer() {
        if (server != null) {
            getLogger().info("Shutting down Fabric server...");
            server.execute(() -> server.stop(false));
        } else {
            getLogger().warning("Cannot shutdown: MinecraftServer is null.");
        }
    }

    private void dispatchToServerThread(Runnable task) {
        MinecraftServer currentServer = server;
        if (currentServer != null) {
            currentServer.execute(task);
            return;
        }

        getLogger().warning("Server reference is null. Running task on caller thread - this may cause thread safety issues.");
        task.run();
    }

    private static class FabricSender implements CommandProcessor.CommandSender {
        private final RedstoneRebootFabricMod mod;
        private final ServerCommandSource source;
        private final dev.demonz.redstonereboot.common.RedstoneRebootCore coreRef;

        private FabricSender(RedstoneRebootFabricMod mod, ServerCommandSource source) {
            this.mod = mod;
            this.source = source;
            this.coreRef = mod.core;
        }

        @Override
        public void sendMessage(String message) {
            source.sendFeedback(() -> mod.parseLegacyText(message), false);
        }

        @Override
        public String getName() {
            return source.getName();
        }

        @Override
        public boolean hasPermission(String permission) {
            if (permission == null) {
                return false;
            }
            if (CommandProcessor.isPublicPermission(permission) && coreRef != null && coreRef.getConfig().isPublicPermissionsEnabled()) {
                return true;
            }
            
            boolean isAdmin = permission.startsWith("redstonereboot.restart.") || permission.contains(".reload") || permission.contains(".doctor");
            
            if (coreRef != null && coreRef.getConfig().isUseOpAsAdminEnabled() && source.hasPermissionLevel(4)) {
                return true;
            }
            
            if (isAdmin) {
                return source.hasPermissionLevel(4);
            }
            
            int level = coreRef != null ? coreRef.getConfig().getDefaultPermissionLevel() : 0;
            return level <= 0 || source.hasPermissionLevel(level);
        }
    }

    private static String resolveMinecraftVersion() {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    }

    private Text parseLegacyText(String text) {
        if (text == null || text.isEmpty()) return Text.empty();
        net.minecraft.text.MutableText result = Text.empty();
        StringBuilder currentText = new StringBuilder();
        java.util.List<net.minecraft.util.Formatting> formats = new java.util.ArrayList<>();
        net.minecraft.text.TextColor activeColor = null;
        
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
                        net.minecraft.text.MutableText partText = Text.literal(currentText.toString()).formatted(formats.toArray(new net.minecraft.util.Formatting[0]));
                        if (activeColor != null) {
                            partText.setStyle(partText.getStyle().withColor(activeColor));
                        }
                        result.append(partText);
                        currentText.setLength(0);
                    }
                    try {
                        activeColor = net.minecraft.text.TextColor.fromRgb(Integer.parseInt(hex.substring(1), 16));
                    } catch (NumberFormatException ignored) {
                        activeColor = null;
                    }
                    formats.clear();
                    i += 13;
                    continue;
                }
            }

            if (c == '\u00A7' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                net.minecraft.util.Formatting format = net.minecraft.util.Formatting.byCode(code);
                if (format != null) {
                    if (currentText.length() > 0) {
                        net.minecraft.text.MutableText partText = Text.literal(currentText.toString()).formatted(formats.toArray(new net.minecraft.util.Formatting[0]));
                        if (activeColor != null) {
                            partText.setStyle(partText.getStyle().withColor(activeColor));
                        }
                        result.append(partText);
                        currentText.setLength(0);
                    }
                    if (format.isColor() || format == net.minecraft.util.Formatting.RESET) {
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
            net.minecraft.text.MutableText partText = Text.literal(currentText.toString()).formatted(formats.toArray(new net.minecraft.util.Formatting[0]));
            if (activeColor != null) {
                partText.setStyle(partText.getStyle().withColor(activeColor));
            }
            result.append(partText);
        }
        return result;
    }
}
