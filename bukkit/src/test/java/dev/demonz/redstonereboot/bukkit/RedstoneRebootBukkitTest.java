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


package dev.demonz.redstonereboot.bukkit;

import dev.demonz.redstonereboot.bukkit.managers.ConfigManager;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class RedstoneRebootBukkitTest {

    private ServerMock server;
    private RedstoneRebootPlugin plugin;

    @BeforeEach
    public void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(RedstoneRebootPlugin.class);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testPluginEnablesSuccessfully() {
        assertNotNull(plugin);
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getRestartManager());
        assertNotNull(plugin.getConfigManager());
        assertNotNull(plugin.getPermissionManager());
    }

    @Test
    public void testStatusSubcommandOutput() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "redstonereboot.use", true);
        player.addAttachment(plugin, "redstonereboot.status", true);

        player.performCommand("reboot status");

        String statusMessage = player.nextMessage();
        assertNotNull(statusMessage);
        assertTrue(statusMessage.contains("RedstoneReboot Status"));
        
        String versionMessage = player.nextMessage();
        assertNotNull(versionMessage);
        assertTrue(versionMessage.contains("Version:"));
        
        String platformMessage = player.nextMessage();
        assertNotNull(platformMessage);
        assertTrue(platformMessage.contains("Platform:"));
    }

    @Test
    public void testPermissionsGating() {
        PlayerMock regularPlayer = server.addPlayer();
        PlayerMock adminPlayer = server.addPlayer();

        regularPlayer.addAttachment(plugin, "redstonereboot.use", true);
        adminPlayer.addAttachment(plugin, "redstonereboot.use", true);

        regularPlayer.performCommand("reboot now 10");
        String regularFeedback = regularPlayer.nextMessage();
        assertNotNull(regularFeedback);
        assertTrue(regularFeedback.contains("No permission") || regularFeedback.contains("I'm sorry"));

        adminPlayer.addAttachment(plugin, "redstonereboot.restart.now", true);
        adminPlayer.performCommand("reboot now 10");

        RestartManager rm = plugin.getRestartManager();
        assertNotNull(rm);
        assertTrue(rm.isRestartInProgress());
        assertEquals(10, rm.getSecondsUntilRestart());
        assertEquals(RestartReason.MANUAL, rm.getCurrentRestartReason());
    }

    @Test
    public void testCountdownTicksAndAlerts() {
        PlayerMock player = server.addPlayer();
        player.addAttachment(plugin, "redstonereboot.use", true);
        player.addAttachment(plugin, "redstonereboot.restart.now", true);
        player.addAttachment(plugin, "redstonereboot.notify", true);

        player.performCommand("reboot now 5");

        RestartManager rm = plugin.getRestartManager();
        assertTrue(rm.isRestartInProgress());
        assertEquals(5, rm.getSecondsUntilRestart());

        server.getScheduler().performTicks(40L);

        assertEquals(3, rm.getSecondsUntilRestart());
    }

    @Test
    public void testCancellationCommand() {
        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, "redstonereboot.use", true);
        admin.addAttachment(plugin, "redstonereboot.restart.now", true);
        admin.addAttachment(plugin, "redstonereboot.restart.cancel", true);

        admin.performCommand("reboot now 30");
        RestartManager rm = plugin.getRestartManager();
        assertTrue(rm.isRestartInProgress());

        admin.performCommand("reboot cancel");
        
        assertFalse(rm.isRestartInProgress());
        assertEquals(-1, rm.getSecondsUntilRestart());
    }

    @Test
    public void testPlaceholderAPISupport() {
        assertTrue(plugin.getTPS() >= 0.0D);
        assertTrue(plugin.getCachedMemoryUsage() >= 0.0D);

        dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook hook = new dev.demonz.redstonereboot.bukkit.integrations.PlaceholderAPIHook(plugin);
        
        String tpsPlaceholder = hook.onRequest(null, "tps");
        assertNotNull(tpsPlaceholder);
        
        String memoryPlaceholder = hook.onRequest(null, "memory");
        assertNotNull(memoryPlaceholder);
    }

    @Test
    public void testConfigReloadSyntaxErrorProtection() throws Exception {
        ConfigManager cfg = plugin.getConfigManager();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        assertTrue(configFile.exists());

        int activeVersion = cfg.getConfigVersion();
        assertEquals(ConfigManager.CURRENT_CONFIG_VERSION, activeVersion);

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("general:\n  plugin-prefix: \"&8[&cRedstone&8]\"\n  [broken: invalid syntax yaml\n");
        }

        assertThrows(RuntimeException.class, () -> plugin.reloadPluginState());

        assertEquals(ConfigManager.CURRENT_CONFIG_VERSION, cfg.getConfigVersion());
    }

    @Test
    public void testConfigMigration() throws Exception {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        assertTrue(configFile.exists());

        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("general:\n" +
                         "  plugin-prefix: \"§8[§cRedstone§8] §aReboot\"\n" +
                         "  debug-mode: false\n" +
                         "  strict-validation: true\n" +
                         "permissions:\n" +
                         "  luckperms:\n" +
                         "    integration-enabled: true\n" +
                         "    default-permission: \"some.old.permission\"\n" +
                         "    admin-permission: \"some.admin.permission\"\n" +
                         "advanced:\n" +
                         "  async-operations: true\n" +
                         "  thread-pool-size: 4\n" +
                         "config-version: 1\n");
        }

        plugin.reloadPluginState();

        ConfigManager cfg = plugin.getConfigManager();
        assertEquals(ConfigManager.CURRENT_CONFIG_VERSION, cfg.getConfigVersion());

        FileConfiguration fileConfig = plugin.getConfig();
        assertTrue(fileConfig.getBoolean("permissions.fallback.use-op-as-admin"));
        assertEquals(2, fileConfig.getInt("permissions.fallback.default-level"));
        assertTrue(fileConfig.getBoolean("permissions.fallback.public-permissions-enabled"));
        assertTrue(fileConfig.getBoolean("placeholders.enabled"));
        assertTrue(fileConfig.getBoolean("advanced.metrics-enabled"));
        assertEquals(60, fileConfig.getInt("advanced.shutdown-delay-ticks"));

        assertFalse(fileConfig.contains("permissions.luckperms.default-permission"));
        assertFalse(fileConfig.contains("permissions.luckperms.admin-permission"));
        assertFalse(fileConfig.contains("advanced.async-operations"));
        assertFalse(fileConfig.contains("advanced.thread-pool-size"));

        File backupFile = new File(plugin.getDataFolder(), "config.yml.v1.backup");
        assertTrue(backupFile.exists());

        backupFile.delete();
    }
}