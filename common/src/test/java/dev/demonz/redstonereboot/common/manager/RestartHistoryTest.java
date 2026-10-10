package dev.demonz.redstonereboot.common.manager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestartHistoryTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultLogPathResolvesToLogsDirectory() {
        RestartHistory history = new RestartHistory(tempDir);
        assertNotNull(history.getLogFile());
        assertTrue(history.getLogFile().toString().replace('\\', '/').endsWith("logs/redstonereboot/restarts.log"));
    }

    @Test
    void customLogPathResolvesCorrectly() {
        Path customTarget = tempDir.resolve("custom-sub").resolve("my-restarts.log");
        RestartHistory history = new RestartHistory(tempDir, customTarget.toString(), ZonedDateTime::now);
        assertEquals(customTarget, history.getLogFile());
    }

    @Test
    void dataFolderSyntaxResolvesWithinDataFolder() {
        RestartHistory history = new RestartHistory(tempDir, "{dataFolder}/restarts.log", ZonedDateTime::now);
        assertEquals(tempDir.resolve("restarts.log"), history.getLogFile());
    }

    @Test
    void recordCreatesParentDirectoryAndWrites() throws IOException {
        Path targetLog = tempDir.resolve("nested").resolve("dir").resolve("restarts.log");
        RestartHistory history = new RestartHistory(tempDir, targetLog.toString(), ZonedDateTime::now);

        assertFalse(Files.exists(targetLog));
        history.record("MANUAL", "Server maintenance", "Console");

        assertTrue(Files.exists(targetLog));
        List<String> lines = Files.readAllLines(targetLog);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("MANUAL"));
        assertTrue(lines.get(0).contains("reason=Server maintenance"));
        assertTrue(lines.get(0).contains("initiator=Console"));

        List<RestartHistory.Entry> recent = history.getRecent(5);
        assertEquals(1, recent.size());
        assertEquals("MANUAL", recent.get(0).format().contains("MANUAL") ? "MANUAL" : "");
    }

    @Test
    void migrateLegacyLogMovesFileWhenTargetDiffers() throws IOException {
        Path legacyLog = tempDir.resolve("restarts.log");
        Files.writeString(legacyLog, "2026-10-09 12:00:00 | SCHEDULED | reason=Daily | initiator=Timer\n");

        Path newTarget = tempDir.resolve("server_logs").resolve("restarts.log");
        RestartHistory history = new RestartHistory(tempDir, newTarget.toString(), ZonedDateTime::now);

        assertFalse(Files.exists(legacyLog));
        assertTrue(Files.exists(newTarget));
        List<String> lines = Files.readAllLines(newTarget);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("SCHEDULED"));

        history.record("EMERGENCY", "Low TPS", "Watchdog");
        List<String> updatedLines = Files.readAllLines(newTarget);
        assertEquals(2, updatedLines.size());
    }

    @Test
    void migrateLegacyLogToDedicatedSubfolder() throws IOException {
        Path legacyLog = tempDir.resolve("restarts.log");
        Files.writeString(legacyLog, "2026-10-09 12:00:00 | SCHEDULED | reason=Daily | initiator=Timer\n");

        Path subfolderTarget = tempDir.resolve("logs").resolve("redstonereboot").resolve("restarts.log");
        RestartHistory history = new RestartHistory(tempDir, subfolderTarget.toString(), ZonedDateTime::now);

        assertFalse(Files.exists(legacyLog));
        assertTrue(Files.exists(subfolderTarget));
        List<String> lines = Files.readAllLines(subfolderTarget);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("SCHEDULED"));
    }
}
