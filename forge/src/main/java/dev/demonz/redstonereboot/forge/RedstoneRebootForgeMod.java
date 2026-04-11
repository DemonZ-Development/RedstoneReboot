package dev.demonz.redstonereboot.forge;

import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import net.minecraftforge.fml.common.Mod;

import java.util.logging.Logger;

/**
 * Forge dedicated-server bootstrap.
 */
@Mod("redstonereboot")
public final class RedstoneRebootForgeMod extends AbstractBootstrapServerPlatform {

    public RedstoneRebootForgeMod() {
        super(Logger.getLogger("RedstoneReboot/Forge"), "Forge Dedicated Server", "1.20.4");
        registerShutdownHook("RedstoneReboot-Forge-Shutdown");
        startCore();
        getLogger().info("Forge dedicated-server bootstrap initialized.");
    }
}
