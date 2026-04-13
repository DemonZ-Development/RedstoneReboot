package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.ControllerBackend;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Restart backend using the Pterodactyl Client API.
 */
public class PterodactylBackend extends ControllerBackend {

    private final String panelUrl;
    private final String apiKey;
    private final String serverId;
    private final HttpClient httpClient;

    public PterodactylBackend(Logger logger, String panelUrl, String apiKey, String serverId) {
        super(logger, "Pterodactyl");
        this.panelUrl = panelUrl;
        this.apiKey = apiKey;
        this.serverId = serverId;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public BackendResult execute() {
        if (isBlank(panelUrl) || isBlank(serverId) || isBlank(resolveApiKey())) {
            logger.warning("Pterodactyl backend misconfigured. Missing URL, Key, or ID.");
            return BackendResult.FAILED;
        }

        try {
            String baseUrl = panelUrl.endsWith("/") ? panelUrl : panelUrl + "/";
            URI uri = URI.create(baseUrl + "api/client/servers/" + serverId + "/power");

            String body = "{\"signal\": \"restart\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + resolveApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "Application/vnd.pterodactyl.v1+json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                logger.info("Pterodactyl accepted the restart signal.");
                return BackendResult.ACCEPTED;
            } else {
                logger.warning("Pterodactyl rejected restart signal. Status: " + status + " Body: " + response.body());
                return BackendResult.FAILED;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            logger.warning("Pterodactyl API timeout. Restart state is UNKNOWN.");
            return BackendResult.UNKNOWN;
        } catch (Exception e) {
            logger.warning("Pterodactyl API error: " + e.getMessage());
            return BackendResult.UNKNOWN;
        }
    }

    @Override
    public BackendState getState() {
        if (isBlank(panelUrl) || isBlank(serverId) || isBlank(resolveApiKey())) {
            return BackendState.MISCONFIGURED;
        }

        try {
            String baseUrl = panelUrl.endsWith("/") ? panelUrl : panelUrl + "/";
            URI uri = URI.create(baseUrl + "api/client/servers/" + serverId + "/resources");
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + resolveApiKey())
                .header("Accept", "Application/vnd.pterodactyl.v1+json")
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300
                ? BackendState.FULL
                : BackendState.ASSISTED;
        } catch (Exception exception) {
            logger.fine("Pterodactyl backend verification skipped: " + exception.getMessage());
            return BackendState.ASSISTED;
        }
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String resolveApiKey() {
        // Allow env-var overrides for sensitive tokens
        String envToken = System.getenv("REBOOT_PTERO_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken;
        }
        return apiKey;
    }
}
