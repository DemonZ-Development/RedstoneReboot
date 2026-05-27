package dev.demonz.redstonereboot.common.command;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.BackendRegistry;
import dev.demonz.redstonereboot.common.manager.RestartReason;
import dev.demonz.redstonereboot.common.platform.ServerPlatform;
import dev.demonz.redstonereboot.common.platform.SimplePlatformConfig;
import dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler;
import dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Practical tests for {@link CommandProcessor} — command handling, permission logic,
 * and interaction with RestartManager.
 */
class CommandProcessorTest {

    @TempDir
    Path tempDir;

    private Logger logger;
    private SimplePlatformConfig config;
    private FakeScheduler scheduler;
    private FakePlatform platform;
    private RedstoneRebootCore core;
    private CommandProcessor processor;

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger("CommandProcessorTest");
        config = new SimplePlatformConfig();
        config.setScheduledRestartsEnabled(false);
        scheduler = new FakeScheduler();
        platform = new FakePlatform();
        core = new RedstoneRebootCore(platform, scheduler, config, tempDir);
        processor = new CommandProcessor(core);
    }

    // --- Public permission checks ---

    @Test
    void isPublicPermissionReturnsTrueForStatus() {
        assertTrue(CommandProcessor.isPublicPermission("redstonereboot.status"));
    }

    @Test
    void isPublicPermissionReturnsTrueForUse() {
        assertTrue(CommandProcessor.isPublicPermission("redstonereboot.use"));
    }

    @Test
    void isPublicPermissionReturnsTrueForNotify() {
        assertTrue(CommandProcessor.isPublicPermission("redstonereboot.notify"));
    }

    @Test
    void isPublicPermissionReturnsFalseForAdmin() {
        assertFalse(CommandProcessor.isPublicPermission("redstonereboot.admin"));
    }

    @Test
    void isPublicPermissionReturnsFalseForNow() {
        assertFalse(CommandProcessor.isPublicPermission("redstonereboot.now"));
    }

    @Test
    void isPublicPermissionReturnsFalseForRandom() {
        assertFalse(CommandProcessor.isPublicPermission("some.other.permission"));
    }

    // --- Status command ---

    @Test
    void processStatusSendsVersionInfo() {
        CapturingSender sender = new CapturingSender(true);
        processor.processStatus(sender);
        // Version is in one of the messages (not necessarily the last one)
        String allMessages = String.join(" ", sender.messages);
        assertTrue(allMessages.contains("1.4.2"),
            "Status should include version 1.4.2");
    }

    @Test
    void processStatusShowsNormalWhenNoRestart() {
        CapturingSender sender = new CapturingSender(true);
        processor.processStatus(sender);
        String allMessages = String.join(" ", sender.messages);
        assertTrue(allMessages.contains("Normal operation"),
            "Should show normal operation when no restart in progress");
    }

    // --- Cancel command ---

    @Test
    void processCancelReportsNoRestartPending() {
        CapturingSender sender = new CapturingSender(true);
        processor.processCancel(sender);
        assertTrue(sender.lastMessage.contains("No restart pending"),
            "Should report no restart pending");
    }

    @Test
    void processCancelCancelsActiveRestart() {
        core.getRestartManager().scheduleRestart(60, RestartReason.MANUAL, "Test");

        CapturingSender sender = new CapturingSender(true);
        processor.processCancel(sender);
        assertTrue(sender.lastMessage.contains("cancelled"),
            "Should confirm cancellation");
        assertFalse(core.getRestartManager().isRestartInProgress());
    }

    // --- Now command ---

    @Test
    void processNowTriggersRestart() {
        CapturingSender sender = new CapturingSender(true);
        processor.processNow(sender, 10);
        assertTrue(sender.lastMessage.contains("10s"),
            "Should confirm restart in 10s");
        assertTrue(core.getRestartManager().isRestartInProgress());
    }

    // --- Help command ---

    @Test
    void processHelpListsAllCommands() {
        CapturingSender sender = new CapturingSender(true);
        processor.processHelp(sender);
        String allMessages = String.join("\n", sender.messages);
        assertTrue(allMessages.contains("status"));
        assertTrue(allMessages.contains("now"));
        assertTrue(allMessages.contains("cancel"));
        assertTrue(allMessages.contains("schedule"));
        assertTrue(allMessages.contains("reload"));
        assertTrue(allMessages.contains("doctor"));
        assertTrue(allMessages.contains("help"));
    }

    // --- Info command ---

    @Test
    void processInfoShowsTPSAndMemory() {
        CapturingSender sender = new CapturingSender(true);
        processor.processInfo(sender);
        String allMessages = String.join("\n", sender.messages);
        assertTrue(allMessages.contains("TPS"), "Info should show TPS");
        assertTrue(allMessages.contains("Memory"), "Info should show Memory");
        assertTrue(allMessages.contains("Players"), "Info should show Players");
    }

    // --- Reload command ---

    @Test
    void processReloadRefreshesConfig() {
        CapturingSender sender = new CapturingSender(true);
        processor.processReload(sender);
        assertTrue(sender.lastMessage.contains("re-initialized"),
            "Reload should confirm re-initialization");
    }

    // --- Helper classes ---

    private static class FakeScheduler implements PlatformTaskScheduler {
        private final List<Runnable> repeatingTasks = new ArrayList<>();

        @Override
        public ScheduledTaskHandle runRepeating(Runnable task, long initialDelayTicks, long periodTicks) {
            repeatingTasks.add(task);
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runRepeatingAsync(Runnable task, long initialDelayTicks, long periodTicks) {
            return runRepeating(task, initialDelayTicks, periodTicks);
        }

        @Override
        public ScheduledTaskHandle runLater(Runnable task, long delayTicks) {
            task.run();
            return () -> {};
        }

        @Override
        public ScheduledTaskHandle runLaterAsync(Runnable task, long delayTicks) {
            task.run();
            return () -> {};
        }

        @Override
        public boolean isFolia() { return false; }
    }

    private static class FakePlatform implements ServerPlatform {
        @Override public void broadcastMessage(String message) {}
        @Override public void broadcastTitle(String title, String subtitle) {}
        @Override public void executeConsole(String command) {}
        @Override public double getTPS() { return 20.0; }
    }

    private static class CapturingSender implements CommandProcessor.CommandSender {
        private final boolean admin;
        private final List<String> messages = new ArrayList<>();
        private String lastMessage = "";

        CapturingSender(boolean admin) { this.admin = admin; }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
            lastMessage = message;
        }

        @Override
        public String getName() { return "TestSender"; }

        @Override
        public boolean hasPermission(String permission) { return admin; }
    }
}
