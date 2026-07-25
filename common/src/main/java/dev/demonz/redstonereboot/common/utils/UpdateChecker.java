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


package dev.demonz.redstonereboot.common.utils;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to check for updates via the Modrinth API.
 */
public class UpdateChecker {

    private static final Pattern VERSION_PATTERN =
        Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    private final String projectId;
    private final String currentVersion;
    private final Logger logger;
    private volatile String latestVersion;
    private volatile boolean updateAvailable;
    private volatile dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle periodicCheckTask;

    public UpdateChecker(String projectId, String currentVersion, Logger logger) {
        this.projectId = projectId;
        this.currentVersion = currentVersion;
        this.logger = logger;
    }

    /**
     * Fetches the latest version asynchronously.
     */
    public CompletableFuture<Void> checkForUpdates() {
        return checkForUpdates(false);
    }

    /**
     * Fetches the latest version asynchronously with option to suppress "up-to-date" success logs.
     */
    public CompletableFuture<Void> checkForUpdates(boolean silent) {
        return CompletableFuture.runAsync(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = java.net.URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version").toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "DemonZDevelopment/RedstoneReboot/" + currentVersion);

                if (conn.getResponseCode() != 200) {
                    logger.warning("Update check failed. Modrinth returned HTTP " + conn.getResponseCode());
                    return;
                }

                try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    Matcher matcher = VERSION_PATTERN.matcher(response);
                    if (!matcher.find()) {
                        logger.warning("Update check: unexpected JSON format — version field not found.");
                        return;
                    }
                    latestVersion = matcher.group(1);
                    updateAvailable = !currentVersion.equalsIgnoreCase(latestVersion);

                    if (updateAvailable) {
                        logger.info("==========================================");
                        logger.info("A new version of RedstoneReboot is available!");
                        logger.info("Current version: " + currentVersion);
                        logger.info("Latest version:  " + latestVersion);
                        logger.info("Download it at: https://modrinth.com/project/" + projectId + "/versions");
                        logger.info("==========================================");
                    } else if (!silent) {
                        logger.info("RedstoneReboot is up to date (v" + currentVersion + ").");
                    }
                }
            } catch (Exception exception) {
                logger.warning("Update check failed: " + exception.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    public void startPeriodicChecks(dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler scheduler) {
        stopPeriodicChecks();
        periodicCheckTask = scheduler.runRepeating(() -> checkForUpdates(true), 432000L, 432000L);
    }

    public void stopPeriodicChecks() {
        if (periodicCheckTask != null) {
            periodicCheckTask.cancel();
            periodicCheckTask = null;
        }
    }

    public boolean hasUpdate() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}