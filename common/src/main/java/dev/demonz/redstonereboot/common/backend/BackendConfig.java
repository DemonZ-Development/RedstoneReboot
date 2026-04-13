package dev.demonz.redstonereboot.common.backend;

import dev.demonz.redstonereboot.common.platform.ServerPlatform;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Handles loading and saving of the restart-backends.yml (properties format for simplicity in common).
 */
public class BackendConfig {

    private final Path configPath;
    private final Logger logger;
    private final Properties properties = new Properties();

    public BackendConfig(Path dataFolder, Logger logger) {
        this.configPath = dataFolder.resolve("restart-backends.properties");
        this.logger = logger;
    }

    public void load() {
        try {
            properties.clear();
            if (!Files.exists(configPath)) {
                saveDefaults();
            }
            try (InputStream in = Files.newInputStream(configPath)) {
                properties.load(in);
            }
        } catch (Exception e) {
            logger.warning("Failed to load restart-backends.properties: " + e.getMessage());
        }
    }

    private void saveDefaults() throws Exception {
        Files.createDirectories(configPath.getParent());
        properties.setProperty("active-backend", "SHUTDOWN_ONLY");
        properties.setProperty("lockout-duration-seconds", "300");
        
        properties.setProperty("ptero-url", "");
        properties.setProperty("ptero-token", "");
        properties.setProperty("ptero-id", "");
        
        properties.setProperty("systemd-service", "minecraft");
        properties.setProperty("localscript-file", "start.sh");

        try (OutputStream out = Files.newOutputStream(configPath)) {
            properties.store(out, "RedstoneReboot Backend Configuration");
        }
    }

    public String getActiveBackend() {
        return properties.getProperty("active-backend", "SHUTDOWN_ONLY").toUpperCase();
    }

    public int getLockoutDuration() {
        return Integer.parseInt(properties.getProperty("lockout-duration-seconds", "300"));
    }

    public String getProperty(String key) {
        String val = properties.getProperty(key);
        if (val != null && val.startsWith("${env.") && val.endsWith("}")) {
            String envVar = val.substring(6, val.length() - 1);
            String envVal = System.getenv(envVar);
            return envVal != null ? envVal : "";
        }
        return val != null ? val : "";
    }
}
