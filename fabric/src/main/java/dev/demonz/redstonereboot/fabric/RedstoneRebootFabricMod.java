package dev.demonz.redstonereboot.fabric;

import dev.demonz.redstonereboot.common.platform.AbstractBootstrapServerPlatform;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.logging.Logger;

/**
 * Fabric dedicated-server bootstrap.
 */
public final class RedstoneRebootFabricMod extends AbstractBootstrapServerPlatform implements DedicatedServerModInitializer {

    public RedstoneRebootFabricMod() {
        super(Logger.getLogger("RedstoneReboot/Fabric"), "Fabric Dedicated Server", resolveMinecraftVersion());
        registerShutdownHook("RedstoneReboot-Fabric-Shutdown");
    }

    @Override
    public void onInitializeServer() {
        startCore();
        getLogger().info("Fabric dedicated-server bootstrap initialized.");
    }

    private static String resolveMinecraftVersion() {
        return FabricLoader.getInstance()
            .getModContainer("minecraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    }
}
