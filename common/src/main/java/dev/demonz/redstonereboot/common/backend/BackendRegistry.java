package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.backend.impl.*;

import java.util.logging.Logger;

/**
 * Registry for managing and discovering restart backends.
 */
public class BackendRegistry {

    private final Logger logger;
    private final BackendConfig config;
    private RestartBackend activeBackend;

    public BackendRegistry(Logger logger, BackendConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public void initialize() {
        config.load();
        String type = config.getActiveBackend();

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
                activeBackend = new LocalScriptBackend(logger);
                break;
            default:
                activeBackend = new ShutdownOnlyBackend(logger);
                break;
        }

        logger.info("Active Restart Backend: " + activeBackend.getName() + " [" + activeBackend.getState() + "]");
    }

    public RestartBackend getActiveBackend() {
        if (activeBackend == null) {
            return new ShutdownOnlyBackend(logger);
        }
        return activeBackend;
    }

    public BackendConfig getConfig() {
        return config;
    }
}
