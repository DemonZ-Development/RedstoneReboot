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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Fabric dedicated-server bootstrap.
 */
public final class RedstoneRebootFabricMod extends AbstractBootstrapServerPlatform implements DedicatedServerModInitializer {

    private JavaPlatformScheduler scheduler;
    private MinecraftServer server;

    public RedstoneRebootFabricMod() {
        super(Logger.getLogger("RedstoneReboot/Fabric"), "Fabric", resolveMinecraftVersion());
        registerShutdownHook("RedstoneReboot-Fabric-Shutdown");
    }

    @Override
    public void onInitializeServer() {
        scheduler = new JavaPlatformScheduler(this::dispatchToServerThread);
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("redstonereboot.properties");
        startCore(scheduler, loadSimpleConfig(configPath), FabricLoader.getInstance().getConfigDir());

        ServerLifecycleEvents.SERVER_STARTED.register(startedServer -> this.server = startedServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(stoppingServer -> {
            this.server = stoppingServer;
            stopCore();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            new BrigadierCommand(core).register(dispatcher, source -> new FabricSender(this, (ServerCommandSource) source));
            getLogger().info("RedstoneReboot command registered.");
        });

        core.onEnable();
        startPlatformMonitoring();
        getLogger().info("Fabric dedicated-server bootstrap initialized.");
    }

    @Override
    public void broadcastMessage(String message) {
        String plainMessage = LegacyTextUtil.stripLegacyFormatting(message);
        if (server != null && server.getPlayerManager() != null) {
            server.getPlayerManager().broadcast(Text.literal(plainMessage), false);
        }
        getLogger().info("[broadcast] " + plainMessage);
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        if (server == null || server.getPlayerManager() == null) {
            getLogger().info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
                + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
            return;
        }
        Text titleText = Text.literal(LegacyTextUtil.stripLegacyFormatting(title));
        Text subtitleText = Text.literal(LegacyTextUtil.stripLegacyFormatting(subtitle));
        for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket(10, 40, 10));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.SubtitleS2CPacket(subtitleText));
            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.TitleS2CPacket(titleText));
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

        task.run();
    }

    private static class FabricSender implements CommandProcessor.CommandSender {
        private final RedstoneRebootFabricMod mod;
        private final ServerCommandSource source;

        private FabricSender(RedstoneRebootFabricMod mod, ServerCommandSource source) {
            this.mod = mod;
            this.source = source;
        }

        @Override
        public void sendMessage(String message) {
            source.sendFeedback(() -> Text.literal(LegacyTextUtil.stripLegacyFormatting(message)), false);
        }

        @Override
        public String getName() {
            return source.getName();
        }

        @Override
        public boolean hasPermission(String permission) {
            if (mod.core.getConfig().isUseOpAsAdminEnabled() && source.hasPermissionLevel(4)) {
                return true;
            }
            return source.hasPermissionLevel(mod.core.getConfig().getDefaultPermissionLevel());
        }
    }

    private static String resolveMinecraftVersion() {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    }
}
