package dev.demonz.redstonereboot.modern.forge;

import dev.demonz.redstonereboot.modern.ModernServerPlatform;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("redstonereboot")
public final class RedstoneRebootForge extends ModernServerPlatform {
    public RedstoneRebootForge() {
        super("Forge", net.minecraft.SharedConstants.getCurrentVersion().name(), FMLPaths.CONFIGDIR.get());
        RegisterCommandsEvent.BUS.addListener(event -> registerCommands(event.getDispatcher()));
        ServerStartedEvent.BUS.addListener(event -> started(event.getServer()));
        ServerStoppingEvent.BUS.addListener(event -> stopping());
    }
}
