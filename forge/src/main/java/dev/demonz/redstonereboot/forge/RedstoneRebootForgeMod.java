package dev.demonz.redstonereboot.forge;

import dev.demonz.redstonereboot.common.command.BrigadierCommand;
import dev.demonz.redstonereboot.common.command.CommandProcessor;
import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import dev.demonz.redstonereboot.common.scheduler.JavaPlatformScheduler;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Forge dedicated-server bootstrap.
 */
@Mod("redstonereboot")
public final class RedstoneRebootForgeMod extends AbstractBootstrapServerPlatform {

    private final JavaPlatformScheduler scheduler;

    public RedstoneRebootForgeMod() {
        super(Logger.getLogger("RedstoneReboot/Forge"), "Forge", "1.20.4");
        registerShutdownHook("RedstoneReboot-Forge-Shutdown");

        scheduler = new JavaPlatformScheduler(this::dispatchToServerThread);
        Path configPath = Path.of("config", "redstonereboot.properties");
        startCore(scheduler, loadSimpleConfig(configPath));

        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        core.onEnable();
        startPlatformMonitoring();
        getLogger().info("Forge dedicated-server bootstrap initialized.");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new BrigadierCommand(core).register(event.getDispatcher(), source -> new ForgeSender(this, (CommandSourceStack) source));
        getLogger().info("RedstoneReboot command registered.");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        stopCore();
    }

    @Override
    public void broadcastMessage(String message) {
        String plainMessage = LegacyTextUtil.stripLegacyFormatting(message);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getPlayerList() != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(plainMessage), false);
        }
        getLogger().info("[broadcast] " + plainMessage);
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList() == null) {
            getLogger().info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
                + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
            return;
        }
        Component titleComponent = Component.literal(LegacyTextUtil.stripLegacyFormatting(title));
        Component subtitleComponent = Component.literal(LegacyTextUtil.stripLegacyFormatting(subtitle));
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(subtitleComponent));
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(titleComponent));
        }
    }

    @Override
    public void executeConsole(String command) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
        } else {
            getLogger().warning("Cannot execute console command: MinecraftServer is null. Command: " + command);
        }
    }

    @Override
    public double getTPS() {
        return dev.demonz.redstonereboot.common.utils.MinecraftTPSUtil.calculateTPS(
            ServerLifecycleHooks.getCurrentServer(),
            getLogger()
        );
    }

    @Override
    public int getOnlinePlayerCount() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.getPlayerList() != null ? server.getPlayerList().getPlayerCount() : 0;
    }

    @Override
    public void shutdownServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            getLogger().info("Shutting down Forge server...");
            server.execute(() -> server.halt(false));
        } else {
            getLogger().warning("Cannot shutdown: MinecraftServer is null.");
        }
    }

    private void dispatchToServerThread(Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(task);
            return;
        }

        task.run();
    }

    private static class ForgeSender implements CommandProcessor.CommandSender {
        private final RedstoneRebootForgeMod mod;
        private final CommandSourceStack source;

        private ForgeSender(RedstoneRebootForgeMod mod, CommandSourceStack source) {
            this.mod = mod;
            this.source = source;
        }

        @Override
        public void sendMessage(String message) {
            source.sendSystemMessage(Component.literal(LegacyTextUtil.stripLegacyFormatting(message)));
        }

        @Override
        public String getName() {
            return source.getTextName();
        }

        @Override
        public boolean hasPermission(String permission) {
            if (mod.core.getConfig().isUseOpAsAdminEnabled() && source.hasPermission(4)) {
                return true;
            }
            return source.hasPermission(mod.core.getConfig().getDefaultPermissionLevel());
        }
    }
}
