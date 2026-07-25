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


package dev.demonz.redstonereboot.common.manager;

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

/**
 * Records restart lifecycle events (scheduled, executed, cancelled, postponed)
 * so administrators can review why and when restarts happened. Events are kept
 * in memory (capped) and, when a data folder is available, appended to a
 * {@code restarts.log} file for inspection across server restarts.
 */
public class RestartHistory {

    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ROOT);
    private static final int MAX_MEMORY = 50;

    private final Deque<Entry> memory = new ArrayDeque<>();
    private final Path logFile;
    private final Supplier<ZonedDateTime> nowSupplier;

    public RestartHistory(Path dataFolder) {
        this(dataFolder, ZonedDateTime::now);
    }

    public RestartHistory(Path dataFolder, Supplier<ZonedDateTime> nowSupplier) {
        this.logFile = (dataFolder != null) ? dataFolder.resolve("restarts.log") : null;
        this.nowSupplier = nowSupplier;
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