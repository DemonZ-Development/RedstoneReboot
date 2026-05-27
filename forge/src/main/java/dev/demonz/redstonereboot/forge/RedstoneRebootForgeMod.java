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

    private JavaPlatformScheduler scheduler;

    public RedstoneRebootForgeMod() {
        super(Logger.getLogger("RedstoneReboot/Forge"), "Forge", resolveMinecraftVersion());
        registerShutdownHook("RedstoneReboot-Forge-Shutdown");

        try {
            scheduler = new JavaPlatformScheduler(this::dispatchToServerThread);
            Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
            Path configPath = configDir.resolve("redstonereboot.properties");
            startCore(scheduler, loadSimpleConfig(configPath), configDir);

            MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
            MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

            if (core != null) {
                try {
                    core.onEnable();
                } catch (Exception onEnableException) {
                    getLogger().severe("Failed to enable RedstoneReboot core: " + onEnableException.getMessage());
                }
            }
            startPlatformMonitoring();
            getLogger().info("Forge dedicated-server bootstrap initialized.");
        } catch (Exception exception) {
            getLogger().severe("Failed to initialize RedstoneReboot: " + exception.getMessage());
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        if (core == null) return;
        new BrigadierCommand(core).register(event.getDispatcher(), source -> new ForgeSender(this, (CommandSourceStack) source));
        getLogger().info("RedstoneReboot command registered.");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        stopCore();
    }

    @Override
    public void broadcastMessage(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getPlayerList() != null) {
            server.getPlayerList().broadcastSystemMessage(parseLegacyComponent(message), false);
        }
        getLogger().info("[broadcast] " + LegacyTextUtil.stripLegacyFormatting(message));
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getPlayerList() == null) {
            getLogger().info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
                + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
            return;
        }
        Component titleComponent = parseLegacyComponent(title);
        Component subtitleComponent = parseLegacyComponent(subtitle);
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 60, 10));
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

        getLogger().warning("Server reference is null. Running task on caller thread - this may cause thread safety issues.");
        task.run();
    }

    private static class ForgeSender implements CommandProcessor.CommandSender {
        private final RedstoneRebootForgeMod mod;
        private final CommandSourceStack source;
        private final dev.demonz.redstonereboot.common.RedstoneRebootCore coreRef;

        private ForgeSender(RedstoneRebootForgeMod mod, CommandSourceStack source) {
            this.mod = mod;
            this.source = source;
            this.coreRef = mod.core;
        }

        @Override
        public void sendMessage(String message) {
            source.sendSystemMessage(mod.parseLegacyComponent(message));
        }

        @Override
        public String getName() {
            return source.getTextName();
        }

        @Override
        public boolean hasPermission(String permission) {
            if (permission == null) {
                return false;
            }
            if (CommandProcessor.isPublicPermission(permission) && coreRef != null && coreRef.getConfig().isPublicPermissionsEnabled()) {
                return true;
            }
            if (coreRef != null && coreRef.getConfig().isUseOpAsAdminEnabled() && source.hasPermission(4)) {
                return true;
            }
            int level = coreRef != null ? coreRef.getConfig().getDefaultPermissionLevel() : 0;
            return level > 0 && source.hasPermission(level);
        }
    }

    private static String resolveMinecraftVersion() {
        return net.minecraftforge.fml.ModList.get()
            .getModContainerById("minecraft")
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("Unknown");
    }

    private Component parseLegacyComponent(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        net.minecraft.network.chat.MutableComponent result = Component.empty();
        StringBuilder currentText = new StringBuilder();
        java.util.List<net.minecraft.ChatFormatting> formats = new java.util.ArrayList<>();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                if (currentText.length() > 0) {
                    result.append(Component.literal(currentText.toString()).withStyle(formats.toArray(new net.minecraft.ChatFormatting[0])));
                    currentText.setLength(0);
                }
                char code = text.charAt(++i);
                net.minecraft.ChatFormatting format = net.minecraft.ChatFormatting.getByCode(code);
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
            result.append(Component.literal(currentText.toString()).withStyle(formats.toArray(new net.minecraft.ChatFormatting[0])));
        }
        return result;
    }
}
