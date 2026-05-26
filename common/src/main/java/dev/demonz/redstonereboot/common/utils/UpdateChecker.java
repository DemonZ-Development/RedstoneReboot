package dev.demonz.redstonereboot.common.utils;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Utility class to check for updates via the Modrinth API.
 */
public class UpdateChecker {

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
                    String versionPrefix = "\"version_number\":\"";
                    int index = response.indexOf(versionPrefix);
                    if (index == -1) {
                        return;
                    }

                    int startIndex = index + versionPrefix.length();
                    int endIndex = response.indexOf("\"", startIndex);
                    latestVersion = response.substring(startIndex, endIndex);
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
        // Use 6-hour initial delay to avoid racing with the initial checkForUpdates() call in onEnable()
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
