package dev.demonz.redstonereboot.common.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical tests for {@link BackendConfig} — configuration loading, default generation,
 * env-var resolution, security hardening, and edge cases.
 */
class BackendConfigTest {

    @TempDir
    Path tempDir;

    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("BackendConfigTest");
    }

    // --- Default generation: backends should be disabled by default ---

    @Test
    void defaultsDisableBackends() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load(), "Load should succeed");

        assertFalse(config.isBackendsEnabled(),
            "Backends should be DISABLED by default");
        assertEquals("SHUTDOWN_ONLY", config.getActiveBackend(),
            "Default backend should be SHUTDOWN_ONLY");
    }

    // --- Default lockout duration ---

    @Test
    void defaultLockoutDurationIs300() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals(300, config.getLockoutDuration());
    }

    // --- Properties file is generated on first load ---

    @Test
    void propertiesFileGeneratedOnFirstLoad() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        assertFalse(Files.exists(configPath), "Config file should not exist yet");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        assertTrue(Files.exists(configPath), "Config file should be generated");
        assertTrue(Files.size(configPath) > 0, "Config file should have content");

        // Verify it contains the backends-enabled=false line
        String content = Files.readString(configPath);
        assertTrue(content.contains("backends-enabled=false"),
            "Generated config should have backends-enabled=false");
    }

    // --- File permissions are owner-only on POSIX systems ---

    @Test
    void filePermissionsAreOwnerOnlyOnPosix() throws IOException {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        Path configPath = tempDir.resolve("restart-backends.properties");
        try {
            var perms = Files.getPosixFilePermissions(configPath);
            assertEquals("rw-------",
                java.nio.file.attribute.PosixFilePermissions.toString(perms),
                "Config file should have owner-only permissions (rw-------)");
        } catch (UnsupportedOperationException e) {
            // Windows — skip this test
            System.out.println("Skipping POSIX permission test on non-POSIX filesystem");
        }
    }

    // --- Custom lockout duration is read correctly ---

    @Test
    void customLockoutDurationIsRead() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=SHUTDOWN_ONLY\nlockout-duration-seconds=600\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals(600, config.getLockoutDuration());
    }

    // --- Invalid lockout duration defaults to 300 ---

    @Test
    void invalidLockoutDurationDefaultsTo300() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=SHUTDOWN_ONLY\nlockout-duration-seconds=not_a_number\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals(300, config.getLockoutDuration(),
            "Invalid lockout duration should default to 300");
    }

    // --- Negative lockout duration is clamped to 0 ---

    @Test
    void negativeLockoutDurationClampedToZero() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=SHUTDOWN_ONLY\nlockout-duration-seconds=-50\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals(0, config.getLockoutDuration(),
            "Negative lockout duration should be clamped to 0");
    }

    // --- Pterodactyl properties are read correctly ---

    @Test
    void pterodactylPropertiesAreRead() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=PTERODACTYL\n"
            + "ptero-url=https://panel.example.com\n"
            + "ptero-token=secret123\n"
            + "ptero-id=abc-def\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("PTERODACTYL", config.getActiveBackend());
        assertEquals("https://panel.example.com", config.getProperty("ptero-url"));
        assertEquals("secret123", config.getProperty("ptero-token"));
        assertEquals("abc-def", config.getProperty("ptero-id"));
    }

    // --- Env var resolution with allowed prefix ---

    @Test
    void envVarResolutionWithAllowedPrefix() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=${env.REBOOT_PTERO_TOKEN:-default_fallback}\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        String resolved = config.getProperty("ptero-token");
        // REBOOT_ is in the allowlist; if the env var isn't set, it uses the fallback
        assertEquals("default_fallback", resolved);
    }

    // --- Env var resolution with disallowed prefix uses fallback ---

    @Test
    void envVarResolutionWithDisallowedPrefixUsesFallback() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=${env.AWS_SECRET_KEY:-fallback_value}\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        String resolved = config.getProperty("ptero-token");
        // AWS_ is NOT in the allowlist, so fallback should be used
        assertEquals("fallback_value", resolved);
    }

    // --- getProperty returns empty string for missing keys ---

    @Test
    void missingPropertyReturnsEmptyString() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("", config.getProperty("nonexistent-key"));
    }

    // --- Reload picks up new values ---

    @Test
    void reloadPicksUpNewValues() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertFalse(config.isBackendsEnabled());

        // Modify the file
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=PTERODACTYL\nlockout-duration-seconds=300\n");

        // Reload
        assertTrue(config.load());
        assertTrue(config.isBackendsEnabled(),
            "Reload should pick up backends-enabled=true");
        assertEquals("PTERODACTYL", config.getActiveBackend());
    }

    // --- Plaintext token warning is triggered ---

    @Test
    void plaintextTokenWarningDoesNotCrash() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=plaintext_token_here\n");

        // This should not throw — just log a warning
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("plaintext_token_here", config.getProperty("ptero-token"));
    }

    // --- Case-insensitive active-backend ---

    @Test
    void activeBackendIsCaseInsensitive() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=pterodactyl\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("PTERODACTYL", config.getActiveBackend(),
            "Active backend should be uppercased");
    }
}
