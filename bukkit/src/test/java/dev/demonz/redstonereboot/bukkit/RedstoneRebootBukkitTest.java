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

        // Verify status feedback was printed
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

        // 1. Attempt manual restart as regular player (should be denied)
        regularPlayer.performCommand("reboot now 10");
        String regularFeedback = regularPlayer.nextMessage();
        assertNotNull(regularFeedback);
        assertTrue(regularFeedback.contains("No permission") || regularFeedback.contains("I'm sorry"));

        // 2. Attempt manual restart as admin player (should succeed)
        adminPlayer.addAttachment(plugin, "redstonereboot.restart.now", true);
        adminPlayer.performCommand("reboot now 10");

        // Verify countdown initiated successfully
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

        // Trigger manual restart countdown with 5 seconds
        player.performCommand("reboot now 5");

        RestartManager rm = plugin.getRestartManager();
        assertTrue(rm.isRestartInProgress());
        assertEquals(5, rm.getSecondsUntilRestart());

        // Fast-forward the Bukkit scheduler in memory by 2 seconds (40 ticks)
        server.getScheduler().performTicks(40L);

        // Verify time decremented correctly
        assertEquals(3, rm.getSecondsUntilRestart());
    }

    @Test
    public void testCancellationCommand() {
        PlayerMock admin = server.addPlayer();
        admin.addAttachment(plugin, "redstonereboot.use", true);
        admin.addAttachment(plugin, "redstonereboot.restart.now", true);
        admin.addAttachment(plugin, "redstonereboot.restart.cancel", true);

        // Start a restart
        admin.performCommand("reboot now 30");
        RestartManager rm = plugin.getRestartManager();
        assertTrue(rm.isRestartInProgress());

        // Cancel it
        admin.performCommand("reboot cancel");
        
        // Assert restart countdown was completely cancelled
        assertFalse(rm.isRestartInProgress());
        assertEquals(-1, rm.getSecondsUntilRestart());
    }

    @Test
    public void testPlaceholderAPISupport() {
        // Assert that getTPS() and getCachedMemoryUsage() return correct values
        assertTrue(plugin.getTPS() >= 0.0D);
        assertTrue(plugin.getCachedMemoryUsage() >= 0.0D);

        // Instantiate PlaceholderAPI expansion and assert requests
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

        // Read active config version
        int activeVersion = cfg.getConfigVersion();
        assertEquals(ConfigManager.CURRENT_CONFIG_VERSION, activeVersion);

        // Intentionally write a broken syntax configuration into config.yml
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write("general:\n  plugin-prefix: \"&8[&cRedstone&8]\"\n  [broken: invalid syntax yaml\n");
        }

        // Trigger config reloading and assert it throws an exception (preventing config override)
        assertThrows(RuntimeException.class, () -> plugin.reloadPluginState());

        // Verify that config in memory did not change and remains safe
        assertEquals(ConfigManager.CURRENT_CONFIG_VERSION, cfg.getConfigVersion());
    }
}
