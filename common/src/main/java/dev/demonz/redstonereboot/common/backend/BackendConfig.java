package dev.demonz.redstonereboot.common.backend;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles loading and saving of the {@code restart-backends.properties} backend configuration file.
 * <p>
 * This file controls which restart backend is active, its connection parameters, and lockout
 * behavior. It is automatically generated with safe defaults on first run and can be reloaded
 * at runtime via {@code /reboot reload}.
 * </p>
 *
 * @see BackendRegistry
 * @since 1.0.0
 */
public class BackendConfig {

    private final Path configPath;
    private final Logger logger;
    private final Object propsLock = new Object();
    private final Properties properties = new Properties();

    /** Environment variable prefixes that are allowed for ${env.VAR:-fallback} resolution. */
    private static final Set<String> ALLOWED_ENV_PREFIXES = Set.of(
        "REBOOT_", "PTERO_", "MINECRAFT_", "JAVA_"
    );

    public BackendConfig(Path dataFolder, Logger logger) {
        this.configPath = dataFolder.resolve("restart-backends.properties");
        this.logger = logger;
    }

    /**
     * Load (or reload) the backend configuration from the properties file.
     *
     * @return {@code true} if the configuration was loaded successfully, {@code false} on failure
     */
    public boolean load() {
        try {
            synchronized (propsLock) {
                properties.clear();
                if (!Files.exists(configPath)) {
                    saveDefaults();
                }
                try (InputStream in = Files.newInputStream(configPath)) {
                    properties.load(in);
                }
                // Warn if API token is stored in plaintext
                String pteroToken = properties.getProperty("ptero-token", "");
                if (pteroToken != null && !pteroToken.isBlank() && !pteroToken.startsWith("${env.")) {
                    logger.warning("Pterodactyl API token detected in properties file. "
                        + "Consider using environment variable REBOOT_PTERO_TOKEN for better security.");
                }
            }
            return true;
        } catch (Exception e) {
            logger.warning("Failed to load restart-backends.properties: " + e.getMessage());
            return false;
        }
    }

    private void saveDefaults() throws Exception {
        Files.createDirectories(configPath.getParent());
        properties.setProperty("backends-enabled", "false");
        properties.setProperty("active-backend", "SHUTDOWN_ONLY");
        properties.setProperty("lockout-duration-seconds", "300");
        
        properties.setProperty("ptero-url", "");
        properties.setProperty("ptero-token", "");
        properties.setProperty("ptero-id", "");
        
        properties.setProperty("systemd-service", "minecraft");
        properties.setProperty("localscript-file", "");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            out.write("# RedstoneReboot Backend Configuration\n".getBytes());
            out.write("# Set backends-enabled=true to enable automatic server restart backends.\n".getBytes());
            out.write("# When disabled (default), the plugin will only stop the server without auto-restart.\n".getBytes());
            out.write("#\n".getBytes());
            out.write("# WARNING: Storing API tokens in plaintext is insecure. Use environment variable REBOOT_PTERO_TOKEN instead.\n".getBytes());
            // Write the actual properties (skipping the date header that Properties.store adds)
            properties.store(out, null);
        }

        // Attempt to set owner-only read/write permissions (Unix only)
        try {
            Files.setPosixFilePermissions(configPath,
                PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // Not a POSIX filesystem (e.g., Windows) — ignore
        } catch (Exception e) {
            logger.log(Level.FINE, "Could not set file permissions on " + configPath, e);
        }
    }

    /**
     * Check whether backends are enabled in the configuration.
     * <p>
     * When disabled (the default), the plugin uses ShutdownOnlyBackend which simply
     * stops the server without any automatic restart mechanism. Users who need automatic
     * server restart must explicitly enable this option.
     * </p>
     *
     * @return {@code true} if backends are enabled, {@code false} otherwise
     */
    public boolean isBackendsEnabled() {
        synchronized (propsLock) {
            return Boolean.parseBoolean(properties.getProperty("backends-enabled", "false").trim());
        }
    }

    public String getActiveBackend() {
        synchronized (propsLock) {
            return properties.getProperty("active-backend", "SHUTDOWN_ONLY").toUpperCase();
        }
    }

    public int getLockoutDuration() {
        try {
            int val;
            synchronized (propsLock) {
                val = Integer.parseInt(properties.getProperty("lockout-duration-seconds", "300").trim());
            }
            return Math.max(val, 0);
        } catch (NumberFormatException exception) {
            logger.warning("Invalid lockout-duration-seconds in properties. Defaulting to 300 seconds.");
            return 300;
        }
    }

    public String getProperty(String key) {
        String val;
        synchronized (propsLock) {
            val = properties.getProperty(key);
        }
        if (val != null && val.startsWith("${env.") && val.endsWith("}")) {
            String inner = val.substring(6, val.length() - 1);
            String envVar = inner;
            String fallback = null;
            int colonIndex = inner.indexOf(":-");
            if (colonIndex >= 0) {
                envVar = inner.substring(0, colonIndex);
                fallback = inner.substring(colonIndex + 2);
            }
            // Enforce env-var allowlist: only resolve vars with approved prefixes
            if (!isEnvVarAllowed(envVar)) {
                logger.warning("Environment variable '" + envVar + "' is not in the allowlist "
                    + "(allowed prefixes: " + ALLOWED_ENV_PREFIXES + "). Using fallback value.");
                return fallback != null ? fallback : val;
            }
            String envVal = System.getenv(envVar);
            if (envVal != null && !envVal.isEmpty()) {
                return envVal;
            }
            return fallback != null ? fallback : val;
        }
        return val != null ? val : "";
    }

    /**
     * Check whether an environment variable name is in the allowlist.
     * Only variables starting with one of the approved prefixes are allowed.
     */
    private boolean isEnvVarAllowed(String envVar) {
        if (envVar == null || envVar.isEmpty()) return false;
        for (String prefix : ALLOWED_ENV_PREFIXES) {
            if (envVar.startsWith(prefix)) return true;
        }
        return false;
    }
}
