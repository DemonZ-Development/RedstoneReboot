package dev.demonz.redstonereboot.common.platform;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Shared bootstrap for loader-based platforms that do not yet have a native
 * scheduler and command bridge like the Bukkit implementation.
 */
public abstract class AbstractBootstrapServerPlatform implements ServerPlatform {

    private final Logger logger;
    private final String platformName;
    private final String minecraftVersion;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile RedstoneRebootCore core;

    protected AbstractBootstrapServerPlatform(Logger logger, String platformName, String minecraftVersion) {
        this.logger = logger;
        this.platformName = platformName;
        this.minecraftVersion = minecraftVersion;
    }

    protected final Logger getLogger() {
        return logger;
    }

    protected final void startCore() {
        if (started.compareAndSet(false, true)) {
            core = new RedstoneRebootCore(this);
            core.onEnable();
        }
    }

    protected final void stopCore() {
        if (started.compareAndSet(true, false) && core != null) {
            core.onDisable();
            core = null;
        }
    }

    protected final void registerShutdownHook(String threadName) {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopCore, threadName));
    }

    @Override
    public void broadcastMessage(String message) {
        logger.info("[broadcast] " + LegacyTextUtil.stripLegacyFormatting(message));
    }

    @Override
    public void broadcastTitle(String title, String subtitle) {
        logger.info("[title] " + LegacyTextUtil.stripLegacyFormatting(title)
            + " | " + LegacyTextUtil.stripLegacyFormatting(subtitle));
    }

    @Override
    public void executeConsole(String command) {
        logger.warning("Console execution requested on " + platformName
            + " but no command bridge is available yet: " + command);
    }

    @Override
    public double getTPS() {
        return 20.0;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
}
