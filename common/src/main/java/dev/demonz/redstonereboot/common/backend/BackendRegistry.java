/*
 * Copyright (c) 2026 DemonZ Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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