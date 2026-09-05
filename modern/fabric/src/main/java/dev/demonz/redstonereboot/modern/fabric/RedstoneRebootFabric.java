package dev.demonz.redstonereboot.modern.fabric;

import dev.demonz.redstonereboot.modern.ModernServerPlatform;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class RedstoneRebootFabric extends ModernServerPlatform implements DedicatedServerModInitializer {
    public RedstoneRebootFabric() {
        super("Fabric", FabricLoader.getInstance().getModContainer("minecraft").orElseThrow()
            .getMetadata().getVersion().getFriendlyString(), FabricLoader.getInstance().getConfigDir());
    }

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(this::started);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopping());
    }
}
