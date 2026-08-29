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

public class BackendConfig {

    public static final int CURRENT_BACKEND_CONFIG_VERSION = 2;

    private final Path configPath;
    private final Logger logger;
    private final Object propsLock = new Object();
    private final Properties properties = new Properties();

    private static final Set<String> ALLOWED_ENV_PREFIXES = Set.of(
        "REBOOT_", "PTERO_", "MINECRAFT_", "JAVA_"
    );

    public BackendConfig(Path dataFolder, Logger logger) {
        this.configPath = dataFolder.resolve("restart-backends.properties");
        this.logger = logger;
    }

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
                int version = parseVersion(properties.getProperty("config-version", "1"));
                if (version < CURRENT_BACKEND_CONFIG_VERSION) {
                    migrate(version);
                }
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

    private int parseVersion(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private void migrate(int oldVersion) throws Exception {
        logger.info("Migrating restart-backends.properties from version " + oldVersion + " to " + CURRENT_BACKEND_CONFIG_VERSION + "...");

        if (Files.exists(configPath)) {
            Path backup = configPath.resolveSibling("restart-backends.properties.v" + oldVersion + ".backup");
            try {
                Files.copy(configPath, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("Created backend config backup: " + backup.getFileName());
            } catch (Exception e) {
                logger.warning("Failed to create backend config backup: " + e.getMessage());
            }
        }

        if (oldVersion < 2) {

            String active = properties.getProperty("active-backend", "DEPEND_ON_HOST");
            if (active != null && active.trim().equalsIgnoreCase("SHUTDOWN_ONLY")) {
                properties.setProperty("active-backend", "DEPEND_ON_HOST");
                logger.info("Migrated active-backend SHUTDOWN_ONLY -> DEPEND_ON_HOST");
            }

            if (!properties.containsKey("backends-enabled")) {
                properties.setProperty("backends-enabled", "false");
            }
            if (!properties.containsKey("lockout-duration-seconds")) {
                properties.setProperty("lockout-duration-seconds", "300");
            }
            if (!properties.containsKey("ptero-url")) {
                properties.setProperty("ptero-url", "");
            }
            if (!properties.containsKey("ptero-token")) {
                properties.setProperty("ptero-token", "");
            }
            if (!properties.containsKey("ptero-id")) {
                properties.setProperty("ptero-id", "");
            }
            if (!properties.containsKey("systemd-service")) {
                properties.setProperty("systemd-service", "minecraft");
            }
            if (!properties.containsKey("localscript-file")) {
                properties.setProperty("localscript-file", "");
            }
            properties.setProperty("config-version", String.valueOf(CURRENT_BACKEND_CONFIG_VERSION));
            persistWithHeader();
            logger.info("Successfully migrated restart-backends.properties to version " + CURRENT_BACKEND_CONFIG_VERSION + "!");
        }
    }

    private void persistWithHeader() throws Exception {
        try (OutputStream out = Files.newOutputStream(configPath)) {
            out.write("# RedstoneReboot Backend Configuration\n".getBytes());
            out.write(("# config-version: " + CURRENT_BACKEND_CONFIG_VERSION + " (do not edit manually)\n").getBytes());
            out.write("# Set backends-enabled=true to enable automatic server restart backends.\n".getBytes());
            out.write("# When disabled (default), the plugin will only stop the server without auto-restart.\n".getBytes());
            out.write("#\n".getBytes());
            out.write("# WARNING: Storing API tokens in plaintext is insecure. Use environment variable REBOOT_PTERO_TOKEN instead.\n".getBytes());
            properties.store(out, null);
        }
    }

    private void saveDefaults() throws Exception {
        Files.createDirectories(configPath.getParent());
        properties.setProperty("config-version", String.valueOf(CURRENT_BACKEND_CONFIG_VERSION));
        properties.setProperty("backends-enabled", "false");
        properties.setProperty("active-backend", "DEPEND_ON_HOST");
        properties.setProperty("lockout-duration-seconds", "300");

        properties.setProperty("ptero-url", "");
        properties.setProperty("ptero-token", "");
        properties.setProperty("ptero-id", "");

        properties.setProperty("systemd-service", "minecraft");
        properties.setProperty("localscript-file", "");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            out.write("# RedstoneReboot Backend Configuration\n".getBytes());
            out.write(("# config-version: " + CURRENT_BACKEND_CONFIG_VERSION + " (do not edit manually)\n").getBytes());
            out.write("# Set backends-enabled=true to enable automatic server restart backends.\n".getBytes());
            out.write("# When disabled (default), the plugin will only stop the server without auto-restart.\n".getBytes());
            out.write("#\n".getBytes());
            out.write("# WARNING: Storing API tokens in plaintext is insecure. Use environment variable REBOOT_PTERO_TOKEN instead.\n".getBytes());
            properties.store(out, null);
        }

        try {
            Files.setPosixFilePermissions(configPath,
                PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
        } catch (Exception e) {
            logger.log(Level.FINE, "Could not set file permissions on " + configPath, e);
        }
    }

    public boolean isBackendsEnabled() {
        synchronized (propsLock) {
            return Boolean.parseBoolean(properties.getProperty("backends-enabled", "false").trim());
        }
    }

    public String getActiveBackend() {
        synchronized (propsLock) {
            return properties.getProperty("active-backend", "DEPEND_ON_HOST").toUpperCase();
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
            if (!isEnvVarAllowed(envVar)) {
                logger.warning("Environment variable '" + envVar + "' is not in the allowlist "
                    + "(allowed prefixes: " + ALLOWED_ENV_PREFIXES + "). Using fallback value.");
                return fallback != null ? fallback : val;
            }
            String envVal = System.getenv(envVar);
            if (envVal != null && !envVal.isEmpty()) {
                return envVal;
            }
            return fallback != null ? fallback : "";
        }
        return val != null ? val : "";
    }

    private boolean isEnvVarAllowed(String envVar) {
        if (envVar == null || envVar.isEmpty()) return false;
        for (String prefix : ALLOWED_ENV_PREFIXES) {
            if (envVar.startsWith(prefix)) return true;
        }
        return false;
    }
}
