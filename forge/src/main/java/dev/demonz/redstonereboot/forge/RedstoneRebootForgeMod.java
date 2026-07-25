/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

import java.nio.file.Files;
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
            Path baseConfigDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
            Path modConfigDir = baseConfigDir.resolve("redstonereboot");
            Files.createDirectories(modConfigDir);

            Path configPath = modConfigDir.resolve("redstonereboot.properties");
            Path legacyConfigPath = baseConfigDir.resolve("redstonereboot.properties");
            if (Files.exists(legacyConfigPath) && !Files.exists(configPath)) {
                try {
                    Files.copy(legacyConfigPath, configPath);
                } catch (Exception ignored) {}
            }

            startCore(scheduler, loadSimpleConfig(configPath), modConfigDir);

            MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
            MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
            MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

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

    private void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        if (core != null) {
            try {
                core.onEnable();
                startPlatformMonitoring();
            } catch (Exception onEnableException) {
                getLogger().severe("Failed to enable RedstoneReboot core: " + onEnableException.getMessage());
            }
        }
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
    public void broadcastActionBar(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.getPlayerList() != null) {
            Component component = parseLegacyComponent(message);
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(component, true);
            }
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
            
            boolean isAdmin = permission.startsWith("redstonereboot.restart.") || permission.contains(".reload") || permission.contains(".doctor");
            
            if (coreRef != null && coreRef.getConfig().isUseOpAsAdminEnabled() && source.hasPermission(4)) {
                return true;
            }
            
            if (isAdmin) {
                return source.hasPermission(4);
            }
            
            int level = coreRef != null ? coreRef.getConfig().getDefaultPermissionLevel() : 0;
            return level <= 0 || source.hasPermission(level);
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
                    if (format.isColor() || format == net.minecraft.ChatFormatting.RESET) {
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
}