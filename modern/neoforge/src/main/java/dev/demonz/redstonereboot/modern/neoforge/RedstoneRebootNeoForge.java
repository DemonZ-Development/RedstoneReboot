package dev.demonz.redstonereboot.modern.neoforge;

import dev.demonz.redstonereboot.modern.ModernServerPlatform;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod("redstonereboot")
public final class RedstoneRebootNeoForge extends ModernServerPlatform {
    public RedstoneRebootNeoForge() {
        super("NeoForge", net.minecraft.SharedConstants.getCurrentVersion().name(), FMLPaths.CONFIGDIR.get());
        NeoForge.EVENT_BUS.addListener(this::commands);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void commands(RegisterCommandsEvent event) { registerCommands(event.getDispatcher()); }
    private void serverStarted(ServerStartedEvent event) { started(event.getServer()); }
    private void serverStopping(ServerStoppingEvent event) { stopping(); }
}
