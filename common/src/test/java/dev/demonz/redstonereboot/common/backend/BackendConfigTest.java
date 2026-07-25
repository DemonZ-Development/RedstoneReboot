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


    @Test
    void defaultsDisableBackends() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load(), "Load should succeed");

        assertFalse(config.isBackendsEnabled(),
            "Backends should be DISABLED by default");
        assertEquals("DEPEND_ON_HOST", config.getActiveBackend(),
            "Default backend should be DEPEND_ON_HOST");
    }


    @Test
    void defaultLockoutDurationIs300() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals(300, config.getLockoutDuration());
    }


    @Test
    void propertiesFileGeneratedOnFirstLoad() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        assertFalse(Files.exists(configPath), "Config file should not exist yet");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        assertTrue(Files.exists(configPath), "Config file should be generated");
        assertTrue(Files.size(configPath) > 0, "Config file should have content");

        String content = Files.readString(configPath);
        assertTrue(content.contains("backends-enabled=false"),
            "Generated config should have backends-enabled=false");
    }


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
            System.out.println("Skipping POSIX permission test on non-POSIX filesystem");
        }
    }


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


    @Test
    void envVarResolutionWithAllowedPrefix() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=${env.REBOOT_PTERO_TOKEN:-default_fallback}\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        String resolved = config.getProperty("ptero-token");
        assertEquals("default_fallback", resolved);
    }


    @Test
    void envVarResolutionWithDisallowedPrefixUsesFallback() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=${env.AWS_SECRET_KEY:-fallback_value}\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());

        String resolved = config.getProperty("ptero-token");
        assertEquals("fallback_value", resolved);
    }


    @Test
    void missingPropertyReturnsEmptyString() {
        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("", config.getProperty("nonexistent-key"));
    }


    @Test
    void reloadPicksUpNewValues() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertFalse(config.isBackendsEnabled());

        Files.writeString(configPath,
            "backends-enabled=true\nactive-backend=PTERODACTYL\nlockout-duration-seconds=300\n");

        assertTrue(config.load());
        assertTrue(config.isBackendsEnabled(),
            "Reload should pick up backends-enabled=true");
        assertEquals("PTERODACTYL", config.getActiveBackend());
    }


    @Test
    void plaintextTokenWarningDoesNotCrash() throws IOException {
        Path configPath = tempDir.resolve("restart-backends.properties");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath,
            "backends-enabled=true\nptero-token=plaintext_token_here\n");

        BackendConfig config = new BackendConfig(tempDir, logger);
        assertTrue(config.load());
        assertEquals("plaintext_token_here", config.getProperty("ptero-token"));
    }


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