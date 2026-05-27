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

            ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
            ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
                this.server = stoppingServer;
                stopCore();
            });

            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                if (core == null) return;
                new BrigadierCommand(core).register(dispatcher, source -> new FabricSender(this, (ServerCommandSource) source));
                getLogger().info("RedstoneReboot command registered.");
            });

            if (core != null) core.onEnable();
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

    private void sendTitlePackets(net.minecraft.server.network.ServerPlayerEntity player, Text title, Text subtitle) {
        try {
            // Try 1.20.2+ consolidated TitleS2CPacket with Action enum
            Class<?> titlePacketClass = Class.forName("net.minecraft.network.packet.s2c.play.TitleS2CPacket");
            Class<?> actionEnum = Class.forName("net.minecraft.network.packet.s2c.play.TitleS2CPacket$Action");
            Object[] actions = actionEnum.getEnumConstants();
            if (actions != null && actions.length >= 3) {
                // Resolve enum constants by name instead of ordinal position
                Object titleAction = null, subtitleAction = null, timesAction = null;
                for (Object action : actions) {
                    String name = ((Enum<?>) action).name();
                    switch (name) {
                        case "TITLE": titleAction = action; break;
                        case "SUBTITLE": subtitleAction = action; break;
                        case "TIMES": timesAction = action; break;
                    }
                }

                if (timesAction != null) {
                    // Try to find TIMES constructor that accepts fade-in/stay/fade-out integers
                    try {
                        Constructor<?> timesCtor = titlePacketClass.getConstructor(actionEnum, int.class, int.class, int.class);
                        player.networkHandler.sendPacket((Packet<?>) timesCtor.newInstance(timesAction, TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
                    } catch (NoSuchMethodException e) {
                        // No int-param constructor for TIMES; send with Text constructor (default timing)
                        getLogger().warning("[title] TIMES constructor with int params not found. Title will use default timing.");
                        player.networkHandler.sendPacket((Packet<?>) titlePacketClass.getConstructor(actionEnum, Text.class).newInstance(timesAction, Text.literal("")));
                    }
                } else {
                    getLogger().warning("[title] TIMES action not found in enum. Title will use default timing.");
                }

                if (subtitleAction != null) {
                    Constructor<?> packetCtor = titlePacketClass.getConstructor(actionEnum, Text.class);
                    player.networkHandler.sendPacket((Packet<?>) packetCtor.newInstance(subtitleAction, subtitle));
                }

                if (titleAction != null) {
                    Constructor<?> packetCtor = titlePacketClass.getConstructor(actionEnum, Text.class);
                    player.networkHandler.sendPacket((Packet<?>) packetCtor.newInstance(titleAction, title));
                }

                return;
            }
        } catch (Exception ignored) {
            // Fall through to 1.20.1 approach
        }

        // 1.20.1 and earlier: separate packet classes
        try {
            Class<?> fadeClass = Class.forName("net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket");
            Constructor<?> fadeCtor = fadeClass.getConstructor(int.class, int.class, int.class);
            player.networkHandler.sendPacket((Packet<?>) fadeCtor.newInstance(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));

            Class<?> subtitleClass = Class.forName("net.minecraft.network.packet.s2c.play.SubtitleS2CPacket");
            Constructor<?> subtitleCtor = subtitleClass.getConstructor(Text.class);
            player.networkHandler.sendPacket((Packet<?>) subtitleCtor.newInstance(subtitle));

            Class<?> titleClass = Class.forName("net.minecraft.network.packet.s2c.play.TitleS2CPacket");
            Constructor<?> titleCtor = titleClass.getConstructor(Text.class);
            player.networkHandler.sendPacket((Packet<?>) titleCtor.newInstance(title));
        } catch (Exception ignored) {
            getLogger().warning("[title] Failed to send title packet: " + ignored.getMessage());
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
            if (coreRef != null && coreRef.getConfig().isUseOpAsAdminEnabled() && source.hasPermissionLevel(4)) {
                return true;
            }
            int level = coreRef != null ? coreRef.getConfig().getDefaultPermissionLevel() : 0;
            return level > 0 && source.hasPermissionLevel(level);
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
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                if (currentText.length() > 0) {
                    result.append(Text.literal(currentText.toString()).formatted(formats.toArray(new net.minecraft.util.Formatting[0])));
                    currentText.setLength(0);
                }
                char code = text.charAt(++i);
                net.minecraft.util.Formatting format = net.minecraft.util.Formatting.byCode(code);
                if (format != null) {
                    if (format.isColor()) {
                        formats.clear();
                    }
                    formats.add(format);
                }
            } else {
                currentText.append(c);
            }
        }
        if (currentText.length() > 0) {
            result.append(Text.literal(currentText.toString()).formatted(formats.toArray(new net.minecraft.util.Formatting[0])));
        }
        return result;
    }
}
