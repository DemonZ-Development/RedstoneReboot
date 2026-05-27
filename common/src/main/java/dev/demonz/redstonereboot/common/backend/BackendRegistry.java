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
    private final java.nio.file.Path dataFolder;
    private volatile RestartBackend activeBackend;
    private volatile RestartBackend fallbackBackend;

    public BackendRegistry(Logger logger, BackendConfig config, java.nio.file.Path dataFolder) {
        this.logger = logger;
        this.config = config;
        this.dataFolder = dataFolder;
    }

    /**
     * Load (or reload) the backend configuration and instantiate the active backend.
     * <p>
     * Safe to call multiple times — each call re-reads {@code restart-backends.properties}
     * and replaces the active backend instance. This method is synchronized to prevent
     * concurrent initialization from overlapping.
     * </p>
     */
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

        // Check if backends are disabled — use ShutdownOnly if so
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

        // Clean up any previous backend instance before replacing
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

    /**
     * Get the currently active restart backend.
     *
     * @return the active backend, falling back to a cached {@link dev.demonz.redstonereboot.common.backend.impl.ShutdownOnlyBackend}
     *         if none is initialized
     */
    public RestartBackend getActiveBackend() {
        if (activeBackend == null) {
            return getOrCreateFallback();
        }
        return activeBackend;
    }

    /**
     * Lazily create and cache the fallback ShutdownOnlyBackend instance.
     */
    private RestartBackend getOrCreateFallback() {
        if (fallbackBackend == null) {
            fallbackBackend = new ShutdownOnlyBackend(logger);
        }
        return fallbackBackend;
    }

    /** @return the underlying backend configuration */
    public BackendConfig getConfig() {
        return config;
    }
}
