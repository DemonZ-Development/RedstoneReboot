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

public class UpdateChecker {

    private static final Pattern VERSION_PATTERN =
        Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern LOADER_VERSION_PATTERN =
        Pattern.compile("\"loaders\"\\s*:\\s*\\[(.*?)\\].*?\"version_number\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);

    private final String projectId;
    private final String currentVersion;
    private final Logger logger;
    private final String platformLoader;
    private volatile String latestVersion;
    private volatile boolean updateAvailable;
    private volatile dev.demonz.redstonereboot.common.scheduler.ScheduledTaskHandle periodicCheckTask;

    public UpdateChecker(String projectId, String currentVersion, Logger logger) {
        this(projectId, currentVersion, logger, null);
    }

    public UpdateChecker(String projectId, String currentVersion, Logger logger, String platformLoader) {
        this.projectId = projectId;
        this.currentVersion = currentVersion;
        this.logger = logger;
        this.platformLoader = platformLoader != null ? platformLoader.toLowerCase(Locale.ROOT) : null;
    }

    public CompletableFuture<Void> checkForUpdates() {
        return checkForUpdates(false);
    }

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
                    String resolvedLatest = null;
                    if (platformLoader != null && !platformLoader.isBlank()) {
                        resolvedLatest = findLatestForLoader(response, platformLoader);
                    }
                    if (resolvedLatest == null) {
                        Matcher matcher = VERSION_PATTERN.matcher(response);
                        if (!matcher.find()) {
                            logger.warning("Update check: unexpected JSON format — version field not found.");
                            return;
                        }
                        resolvedLatest = matcher.group(1);
                    }
                    latestVersion = resolvedLatest;
                    updateAvailable = isNewerVersion(currentVersion, latestVersion);

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

    public synchronized void startPeriodicChecks(dev.demonz.redstonereboot.common.scheduler.PlatformTaskScheduler scheduler) {
        stopPeriodicChecks();
        periodicCheckTask = scheduler.runRepeating(() -> checkForUpdates(true), 432000L, 432000L);
    }

    public synchronized void stopPeriodicChecks() {
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

    private static String baseVersion(String version) {
        if (version == null) return "";
        int idx = version.indexOf('-');
        return idx >= 0 ? version.substring(0, idx) : version;
    }

    static boolean isNewerVersion(String currentVersion, String candidateVersion) {
        int[] current = parseVersion(baseVersion(currentVersion));
        int[] candidate = parseVersion(baseVersion(candidateVersion));
        if (current == null || candidate == null) {
            return false;
        }

        int length = Math.max(current.length, candidate.length);
        for (int index = 0; index < length; index++) {
            int currentPart = index < current.length ? current[index] : 0;
            int candidatePart = index < candidate.length ? candidate[index] : 0;
            if (candidatePart != currentPart) {
                return candidatePart > currentPart;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }

        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("\\d+(?:\\.\\d+)*")) {
            return null;
        }

        String[] parts = normalized.split("\\.");
        int[] parsed = new int[parts.length];
        try {
            for (int index = 0; index < parts.length; index++) {
                parsed[index] = Integer.parseInt(parts[index]);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String findLatestForLoader(String json, String desiredLoader) {
        if (json == null || desiredLoader == null) return null;
        String desired = desiredLoader.toLowerCase(Locale.ROOT);
        Matcher matcher = LOADER_VERSION_PATTERN.matcher(json);
        String firstVersion = null;
        while (matcher.find()) {
            String loadersRaw = matcher.group(1).toLowerCase(Locale.ROOT);
            String version = matcher.group(2);
            if (firstVersion == null) {
                firstVersion = version;
            }

            if (loadersRaw.contains("\"" + desired + "\"")) {
                return version;
            }
        }

        if (isBukkitFamily(desired)) {
            matcher.reset();
            while (matcher.find()) {
                String loadersRaw = matcher.group(1).toLowerCase(Locale.ROOT);
                String version = matcher.group(2);
                if (loadersRaw.contains("\"bukkit\"") || loadersRaw.contains("\"paper\"")
                    || loadersRaw.contains("\"purpur\"") || loadersRaw.contains("\"spigot\"")) {
                    return version;
                }
            }
        }
        return firstVersion;
    }

    private static boolean isBukkitFamily(String loader) {
        return loader.equals("bukkit") || loader.equals("paper") || loader.equals("purpur")
            || loader.equals("spigot") || loader.equals("craftbukkit");
    }
}
