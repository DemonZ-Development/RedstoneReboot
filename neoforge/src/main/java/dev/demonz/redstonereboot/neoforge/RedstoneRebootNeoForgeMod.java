package dev.demonz.redstonereboot.neoforge;

import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import net.neoforged.fml.common.Mod;

import java.util.logging.Logger;

/**
 * NeoForge dedicated-server bootstrap.
 */
@Mod("redstonereboot")
public final class RedstoneRebootNeoForgeMod extends AbstractBootstrapServerPlatform {

    public RedstoneRebootNeoForgeMod() {
        super(Logger.getLogger("RedstoneReboot/NeoForge"), "NeoForge Dedicated Server", "1.20.4");
        registerShutdownHook("RedstoneReboot-NeoForge-Shutdown");
        startCore();
        getLogger().info("NeoForge dedicated-server bootstrap initialized.");
    }
}
