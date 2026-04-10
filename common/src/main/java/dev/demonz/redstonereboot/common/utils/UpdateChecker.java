package dev.demonz.redstonereboot.common.utils;

import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class to check for updates via the Modrinth API.
 */
public class UpdateChecker {

    private final String projectId;
    private final String currentVersion;
    private final Logger logger;
    private String latestVersion = null;
    private boolean updateAvailable = false;

    public UpdateChecker(String projectId, String currentVersion, Logger logger) {
        this.projectId = projectId;
        this.currentVersion = currentVersion;
        this.logger = logger;
    }

    /**
     * Fetches the latest version asynchronously.
     */
    public CompletableFuture<Void> checkForUpdates() {
        return CompletableFuture.runAsync(() -> {
            try {
                URL url = java.net.URI.create("https://api.modrinth.com/v2/project/" + projectId + "/version").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "DemonZDevelopment/RedstoneReboot/" + currentVersion);

                if (conn.getResponseCode() == 200) {
                    try (Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String response = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        // Modrinth API returns an array of version objects. The first one is the latest.
                        // We do a simple string search to avoid bundling a JSON library in the common module.
                        String versionPrefix = "\"version_number\":\"";
                        int index = response.indexOf(versionPrefix);
                        if (index != -1) {
                            int startIndex = index + versionPrefix.length();
                            int endIndex = response.indexOf("\"", startIndex);
                            latestVersion = response.substring(startIndex, endIndex);

                            if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                                updateAvailable = true;
                                logger.info("==========================================");
                                logger.info("A new version of RedstoneReboot is available!");
                                logger.info("Current version: " + currentVersion);
                                logger.info("Latest version:  " + latestVersion);
                                logger.info("Download it at: https://modrinth.com/project/" + projectId + "/versions");
                                logger.info("==========================================");
                            } else {
                                logger.info("RedstoneReboot is up to date (v" + currentVersion + ").");
                            }
                        }
                    }
                } else {
                    logger.debug("Failed to check for updates. Response code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                logger.debug("Exception while checking for updates: " + e.getMessage());
            }
        });
    }

    public boolean hasUpdate() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
