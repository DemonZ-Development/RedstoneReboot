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


package dev.demonz.redstonereboot.common.api;

import dev.demonz.redstonereboot.common.RedstoneRebootCore;
import dev.demonz.redstonereboot.common.backend.BackendConfig;
import dev.demonz.redstonereboot.common.backend.EnvironmentDetector;
import dev.demonz.redstonereboot.common.backend.RestartBackend;
import dev.demonz.redstonereboot.common.manager.RestartHistory;
import dev.demonz.redstonereboot.common.manager.RestartManager;
import dev.demonz.redstonereboot.common.platform.PlatformConfig;
import dev.demonz.redstonereboot.common.text.LegacyTextUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Makes a debug dump file for support — /reboot dump */
public final class DumpGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private DumpGenerator() {}

    public static Path generate(RedstoneRebootCore core, Path dataFolder) throws IOException {
        if (core == null) throw new IllegalArgumentException("core is null");
        if (dataFolder == null) throw new IllegalArgumentException("dataFolder is null");
        Files.createDirectories(dataFolder);
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
        Path dumpFile = dataFolder.resolve("dump-" + ts + ".txt");

        StringBuilder sb = new StringBuilder(8192);
        sb.append("=== RedstoneReboot Dump ===\n");
        sb.append("Generated: ").append(FMT.format(Instant.now())).append(" (").append(ZoneId.systemDefault()).append(")\n");
        sb.append("Version: ").append(core.getVersion()).append("\n");
        sb.append("Brand: ").append(RedstoneRebootCore.BRAND).append("\n");
        sb.append("Platform: ").append(core.getPlatform().getPlatformName()).append(" (MC ").append(core.getPlatform().getMinecraftVersion()).append(")\n");
        sb.append("Players: ").append(core.getPlatform().getOnlinePlayerCount()).append("\n");
        sb.append("TPS: ").append(String.format(Locale.ROOT, "%.1f", core.getPlatform().getTPS())).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")\n");
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" ").append(System.getProperty("os.arch")).append("\n");

        Runtime rt = Runtime.getRuntime();
        double memPct = (double)(rt.totalMemory() - rt.freeMemory()) / rt.maxMemory() * 100.0;
        sb.append("Memory: ").append(String.format(Locale.ROOT, "%.1f%%", memPct))
          .append(" (free ").append(rt.freeMemory()>>20).append("M / total ").append(rt.totalMemory()>>20).append("M / max ").append(rt.maxMemory()>>20).append("M)\n");

        // Config
        sb.append("\n--- Platform Config ---\n");
        PlatformConfig cfg = core.getConfig();
        try {
            sb.append("scheduledRestartsEnabled: ").append(cfg.isScheduledRestartsEnabled()).append("\n");
            sb.append("scheduledTimes: ").append(cfg.getScheduledTimes()).append("\n");
            sb.append("scheduledDays: ").append(cfg.getScheduledDays()).append("\n");
            sb.append("timezone: ").append(cfg.getTimezone()).append(" (zoneId: ").append(cfg.getZoneId()).append(")\n");
            sb.append("warningTime: ").append(cfg.getScheduledWarningTime()).append("\n");
            sb.append("warningTimes: ").append(cfg.getWarningTimes()).append("\n");
            sb.append("alertsEnabled: ").append(cfg.isAlertsEnabled()).append("\n");
            sb.append("monitoringEnabled: ").append(cfg.isMonitoringEnabled()).append(" tpsThreshold=").append(cfg.getTpsThreshold()).append(" memThreshold=").append(cfg.getMemoryThreshold()).append("\n");
            sb.append("checkInterval: ").append(cfg.getCheckInterval()).append(" consecutiveChecks: ").append(cfg.getConsecutiveChecks()).append("\n");
            sb.append("emergencyEnabled: ").append(cfg.isEmergencyRestartEnabled()).append(" tps=").append(cfg.getEmergencyTpsThreshold()).append(" mem=").append(cfg.getEmergencyMemoryThreshold()).append(" delay=").append(cfg.getEmergencyDelay()).append("\n");
            sb.append("shutdownDelayTicks: ").append(cfg.getShutdownDelayTicks()).append("\n");
            sb.append("prefix: ").append(LegacyTextUtil.stripLegacyFormatting(cfg.getPrefix())).append("\n");
        } catch (Exception e) {
            sb.append("config error: ").append(e.getMessage()).append("\n");
        }

        // Backend
        sb.append("\n--- Backend ---\n");
        try {
            RestartBackend backend = core.getBackendRegistry().getActiveBackend();
            sb.append("activeBackend: ").append(backend.getName()).append("\n");
            sb.append("controllerOwned: ").append(backend.isControllerOwned()).append("\n");
            try {
                sb.append("state: ").append(backend.getState()).append("\n");
            } catch (Exception e) {
                sb.append("state error: ").append(e.getMessage()).append("\n");
            }
            List<String> detected = EnvironmentDetector.detectPotentialBackends();
            sb.append("detectedEnv: ").append(detected.isEmpty() ? "Generic" : String.join(", ", detected)).append("\n");
            BackendConfig bc = core.getBackendRegistry().getConfig();
            sb.append("backendsEnabled: ").append(bc.isBackendsEnabled()).append("\n");
            sb.append("active-backend: ").append(bc.getActiveBackend()).append("\n");
            sb.append("lockoutDuration: ").append(bc.getLockoutDuration()).append("\n");
            sb.append("ptero-url: ").append(sanitize(bc.getProperty("ptero-url"))).append("\n");
            sb.append("ptero-id: ").append(sanitize(bc.getProperty("ptero-id"))).append("\n");
            sb.append("ptero-token: ").append(maskToken(bc.getProperty("ptero-token"))).append("\n");
            sb.append("systemd-service: ").append(bc.getProperty("systemd-service")).append("\n");
            sb.append("localscript-file: ").append(bc.getProperty("localscript-file")).append("\n");
        } catch (Exception e) {
            sb.append("backend error: ").append(e.getMessage()).append("\n");
        }

        // RestartManager
        sb.append("\n--- RestartManager ---\n");
        try {
            RestartManager rm = core.getRestartManager();
            sb.append("restartInProgress: ").append(rm.isRestartInProgress()).append("\n");
            sb.append("secondsUntilRestart: ").append(rm.getSecondsUntilRestart()).append("\n");
            sb.append("currentReason: ").append(rm.getCurrentRestartReason().getDisplayName()).append("\n");
            sb.append("initiator: ").append(rm.getRestartInitiator()).append("\n");
            sb.append("controllerPending: ").append(rm.isControllerRestartPending()).append("\n");
            sb.append("lockoutActive: ").append(rm.isLockoutActive()).append("\n");
            sb.append("nextScheduled: ").append(rm.getNextScheduledRestart() != null ? rm.getNextScheduledRestart().toString() : "None").append("\n");
            sb.append("restartInfo: ").append(rm.getRestartInfo()).append("\n");
        } catch (Exception e) {
            sb.append("restartManager error: ").append(e.getMessage()).append("\n");
        }

        // History
        sb.append("\n--- Recent History (last 20) ---\n");
        try {
            RestartManager rm = core.getRestartManager();
            List<RestartHistory.Entry> entries = rm.getHistory().getRecent(20);
            if (entries.isEmpty()) sb.append("(no events)\n");
            for (RestartHistory.Entry e : entries) sb.append(e.format()).append("\n");
        } catch (Exception e) {
            sb.append("history error: ").append(e.getMessage()).append("\n");
        }

        // Files listing
        sb.append("\n--- Data Folder Files ---\n");
        try {
            if (Files.isDirectory(dataFolder)) {
                try (var stream = Files.list(dataFolder)) {
                    stream.forEach(p -> {
                        try { sb.append(p.getFileName()).append(" (").append(Files.size(p)).append(" bytes)\n"); } catch (Exception ignored) { sb.append(p.getFileName()).append("\n"); }
                    });
                }
            }
        } catch (Exception e) { sb.append("list error: ").append(e.getMessage()).append("\n"); }

        // Update checker
        sb.append("\n--- UpdateChecker ---\n");
        try {
            var uc = core.getUpdateChecker();
            sb.append("hasUpdate: ").append(uc != null && uc.hasUpdate()).append("\n");
            sb.append("latestVersion: ").append(uc != null && uc.getLatestVersion() != null ? uc.getLatestVersion() : "unknown").append("\n");
            sb.append("currentVersion: ").append(core.getVersion()).append("\n");
        } catch (Exception e) { sb.append("updateChecker error: ").append(e.getMessage()).append("\n"); }

        // Message adapters
        sb.append("\n--- Message Adapters ---\n");
        try {
            if (RedstoneRebootAPI.isAvailable()) {
                var api = RedstoneRebootAPI.getInstance();
                sb.append("listeners: ").append(api.getListeners().size()).append("\n");
                for (var l : api.getListeners()) sb.append(" - ").append(l.getClass().getName()).append("\n");
                sb.append("adapters: ").append(api.getMessageAdapters().size()).append("\n");
                for (var a : api.getMessageAdapters()) sb.append(" - ").append(a.getName()).append(" (").append(a.getClass().getName()).append(")\n");
            } else {
                sb.append("API not initialized\n");
            }
        } catch (Exception e) { sb.append("api error: ").append(e.getMessage()).append("\n"); }

        sb.append("\n=== End Dump ===\n");

        Files.writeString(dumpFile, sb.toString());
        return dumpFile;
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private static String maskToken(String token) {
        if (token == null || token.isBlank()) return "(empty)";
        if (token.startsWith("${env.")) return token + " (env var)";
        if (token.length() <= 8) return "***";
        return token.substring(0, 4) + "***" + token.substring(token.length() - 2);
    }
}
