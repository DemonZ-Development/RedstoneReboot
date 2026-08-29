package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.backend.impl.*;

import java.util.logging.Logger;

public class BackendRegistry {

    private final Logger logger;
    private final BackendConfig config;
    private final java.nio.file.Path dataFolder;
    private volatile RestartBackend activeBackend;
    private volatile RestartBackend fallbackBackend;

    public BackendRegistry(Logger logger, BackendConfig config, java.nio.file.Path dataFolder) {
        this.logger = logger;
        this.config = config;
        this.dataFolder = dataFolder;
    }

    public synchronized void initialize() {
        boolean loaded = config.load();
        if (!loaded) {
            logger.warning("Failed to load backend configuration. Falling back to ShutdownOnly.");
            if (activeBackend != null) {
                activeBackend.cleanup();
            }
            activeBackend = getOrCreateFallback();
            return;
        }

        if (!config.isBackendsEnabled()) {
            logger.info("Backends are disabled in configuration. Using shutdown-only mode. "
                + "Enable backends in restart-backends.properties if you need automatic server restart.");
            if (activeBackend != null) {
                activeBackend.cleanup();
            }
            activeBackend = getOrCreateFallback();
            logger.info("Active Restart Backend: " + activeBackend.getName());
            return;
        }

        String type = config.getActiveBackend();

        if (activeBackend != null) {
            activeBackend.cleanup();
        }

        try {
            switch (type) {
                case "PTERODACTYL":
                    activeBackend = new PterodactylBackend(
                        logger,
                        config.getProperty("ptero-url"),
                        config.getProperty("ptero-token"),
                        config.getProperty("ptero-id")
                    );
                    break;
                case "SYSTEMD":
                    activeBackend = new SystemdBackend(logger, config.getProperty("systemd-service"));
                    break;
                case "DOCKER":
                    activeBackend = new DockerBackend(logger);
                    break;
                case "LOCALSCRIPT":
                    activeBackend = new LocalScriptBackend(logger, config.getProperty("localscript-file"), dataFolder);
                    break;
                case "DEPEND_ON_HOST":
                case "SHUTDOWN_ONLY":
                default:
                    activeBackend = getOrCreateFallback();
                    break;
            }
        } catch (Exception exception) {
            logger.warning("Failed to initialize backend '" + type + "': " + exception.getMessage() + ". Falling back to ShutdownOnly.");
            activeBackend = getOrCreateFallback();
        }

        logger.info("Active Restart Backend: " + activeBackend.getName());
    }

    public RestartBackend getActiveBackend() {
        if (activeBackend == null) {
            return getOrCreateFallback();
        }
        return activeBackend;
    }

    private RestartBackend getOrCreateFallback() {
        if (fallbackBackend == null) {
            fallbackBackend = new ShutdownOnlyBackend(logger);
        }
        return fallbackBackend;
    }

    public BackendConfig getConfig() {
        return config;
    }
}
