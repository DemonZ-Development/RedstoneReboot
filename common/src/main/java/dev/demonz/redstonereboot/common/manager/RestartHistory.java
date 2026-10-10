package dev.demonz.redstonereboot.common.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class RestartHistory {

    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT);
    private static final int MAX_MEMORY = 50;

    private final Deque<Entry> memory = new ArrayDeque<>();
    private final Path logFile;
    private final Supplier<ZonedDateTime> nowSupplier;

    public RestartHistory(Path dataFolder) {
        this(dataFolder, "logs/redstonereboot/restarts.log", ZonedDateTime::now);
    }

    public RestartHistory(Path dataFolder, Supplier<ZonedDateTime> nowSupplier) {
        this(dataFolder, "logs/redstonereboot/restarts.log", nowSupplier);
    }

    public RestartHistory(Path dataFolder, String customLogPath, Supplier<ZonedDateTime> nowSupplier) {
        this.logFile = resolveLogFile(dataFolder, customLogPath);
        this.nowSupplier = nowSupplier;
        migrateLegacyLog(dataFolder, this.logFile);
    }

    public Path getLogFile() {
        return logFile;
    }

    private static Path resolveLogFile(Path dataFolder, String customPath) {
        if (customPath == null || customPath.isBlank()) {
            return Path.of("logs", "redstonereboot", "restarts.log");
        }
        String trimmed = customPath.trim();
        if (dataFolder != null && (trimmed.startsWith("{dataFolder}") || trimmed.startsWith("dataFolder"))) {
            String remainder = trimmed.replaceFirst("^\\{?dataFolder\\}?[/\\\\]?", "");
            return remainder.isBlank() ? dataFolder.resolve("restarts.log") : dataFolder.resolve(remainder);
        }
        return Path.of(trimmed);
    }

    private static void migrateLegacyLog(Path dataFolder, Path targetLog) {
        if (targetLog == null) {
            return;
        }
        try {
            Path normalizedTarget = targetLog.toAbsolutePath().normalize();
            if (Files.exists(normalizedTarget)) {
                return;
            }
            if (dataFolder != null) {
                Path legacyLog = dataFolder.resolve("restarts.log");
                if (Files.exists(legacyLog)) {
                    Path normalizedLegacy = legacyLog.toAbsolutePath().normalize();
                    if (!normalizedLegacy.equals(normalizedTarget)) {
                        relocateLog(legacyLog, targetLog);
                        return;
                    }
                }
            }
            Path legacyRootLog = Path.of("logs", "restarts.log");
            if (Files.exists(legacyRootLog)) {
                Path normalizedRoot = legacyRootLog.toAbsolutePath().normalize();
                if (!normalizedRoot.equals(normalizedTarget)) {
                    relocateLog(legacyRootLog, targetLog);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void relocateLog(Path source, Path target) throws IOException {
        Path targetParent = target.toAbsolutePath().getParent();
        if (targetParent != null && !Files.exists(targetParent)) {
            Files.createDirectories(targetParent);
        }
        if (!Files.exists(target)) {
            Files.move(source, target);
        }
    }

    public synchronized void record(String type, String reason, String initiator) {
        ZonedDateTime now = nowSupplier.get();
        Entry entry = new Entry(now, type, reason, initiator);
        memory.addLast(entry);
        while (memory.size() > MAX_MEMORY) {
            memory.removeFirst();
        }
        if (logFile != null) {
            try {
                Path parent = logFile.toAbsolutePath().getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(logFile, entry.toLogLine() + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized List<Entry> getRecent(int limit) {
        List<Entry> all = new ArrayList<>(memory);
        if (limit <= 0 || limit >= all.size()) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
    }

    public static final class Entry {
        private final ZonedDateTime time;
        private final String type;
        private final String reason;
        private final String initiator;

        Entry(ZonedDateTime time, String type, String reason, String initiator) {
            this.time = time;
            this.type = type;
            this.reason = reason;
            this.initiator = initiator;
        }

        public String toLogLine() {
            return FORMAT.format(time) + " | " + type + " | reason=" + reason + " | initiator=" + initiator;
        }

        public String format() {
            return FORMAT.format(time) + "  " + type + "  (" + reason + ", by " + initiator + ")";
        }
    }
}
