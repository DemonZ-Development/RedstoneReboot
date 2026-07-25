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


package dev.demonz.redstonereboot.common.backend.impl;

import dev.demonz.redstonereboot.common.backend.BackendResult;
import dev.demonz.redstonereboot.common.backend.ControllerBackend;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Restart backend using the Pterodactyl Client API.
 */
public class PterodactylBackend extends ControllerBackend {

    private final String panelUrl;
    private final String apiKey;
    private final String encodedServerId;
    private final HttpClient httpClient;

    public PterodactylBackend(Logger logger, String panelUrl, String apiKey, String serverId) {
        super(logger, "Pterodactyl");
        if (panelUrl != null && !panelUrl.isBlank()
                && !panelUrl.startsWith("http://") && !panelUrl.startsWith("https://")) {
            throw new IllegalArgumentException(
                "Pterodactyl panelUrl must start with http:// or https:// — got: " + panelUrl);
        }
        this.panelUrl = panelUrl;
        this.apiKey = apiKey;
        this.encodedServerId = URLEncoder.encode(serverId != null ? serverId : "", StandardCharsets.UTF_8);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public BackendResult execute() {
        if (isBlank(panelUrl) || isBlank(encodedServerId) || isBlank(resolveApiKey())) {
            logger.warning("Pterodactyl backend misconfigured. Missing URL, Key, or ID.");
            return BackendResult.FAILED;
        }

        try {
            String baseUrl = panelUrl.endsWith("/") ? panelUrl : panelUrl + "/";
            URI uri = URI.create(baseUrl + "api/client/servers/" + encodedServerId + "/power");

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
                logger.warning("Pterodactyl rejected restart signal. Status: " + status + " (body omitted for security)");
                return BackendResult.FAILED;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            logger.warning("Pterodactyl API timeout. Treating as UNKNOWN.");
            return BackendResult.UNKNOWN;
        } catch (Exception e) {
            logger.warning("Pterodactyl API error: " + e.getMessage());
            return BackendResult.FAILED;
        }
    }

    @Override
    public BackendState getState() {
        if (isBlank(panelUrl) || isBlank(encodedServerId) || isBlank(resolveApiKey())) {
            return BackendState.MISCONFIGURED;
        }

        try {
            String baseUrl = panelUrl.endsWith("/") ? panelUrl : panelUrl + "/";
            URI uri = URI.create(baseUrl + "api/client/servers/" + encodedServerId + "/resources");
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
            logger.warning("Pterodactyl backend verification failed");
            return BackendState.ASSISTED;
        }
    }

    @Override
    public void cleanup() {
        logger.fine("PterodactylBackend cleanup called.");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String resolveApiKey() {
        String envToken = System.getenv("REBOOT_PTERO_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            return envToken;
        }
        return apiKey;
    }

    @Override
    public String toString() {
        return "PterodactylBackend{serverId=" + encodedServerId
            + ", panelUrl=" + panelUrl
            + ", apiKey=***MASKED***}";
    }
}