package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.backend.impl.*;

import java.util.logging.Logger;

/**
 * Registry for managing and discovering restart backends.
 * <p>
 * Reads the {@code restart-backends.properties} file via {@link BackendConfig}
 * and instantiates the appropriate {@link RestartBackend} implementation.
 * Supports hot-reload through {@link #initialize()} which can be called
 * after configuration changes.
 * </p>
 *
 * @see BackendConfig
 * @see RestartBackend
 * @since 1.0.0
 */
public class BackendRegistry {

    private final Logger logger;
    private final BackendConfig config;
    private RestartBackend activeBackend;

    public BackendRegistry(Logger logger, BackendConfig config) {
        this.logger = logger;
        this.config = config;
    }

    /**
     * Load (or reload) the backend configuration and instantiate the active backend.
     * <p>
     * Safe to call multiple times — each call re-reads {@code restart-backends.properties}
     * and replaces the active backend instance.
     * </p>
     */
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

    /**
     * Get the currently active restart backend.
     *
     * @return the active backend, falling back to {@link dev.demonz.redstonereboot.common.backend.impl.ShutdownOnlyBackend}
     *         if none is initialized
     */
    public RestartBackend getActiveBackend() {
        if (activeBackend == null) {
            return new ShutdownOnlyBackend(logger);
        }
        return activeBackend;
    }

    /** @return the underlying backend configuration */
    public BackendConfig getConfig() {
        return config;
    }
}
