package com.logistics.rca.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM client. {@code rca.ai.provider=cursor} uses the official Cursor Cloud Agents API
 * (no-repo agent) billed to the Cursor account behind {@code CURSOR_API_KEY}.
 * {@code openai} keeps OpenAI-compatible Chat Completions.
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final AiProperties properties;
    private final ObjectMapper mapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(Math.max(10, properties.getTimeoutSeconds()) * 1000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(trimSlash(properties.getBaseUrl()))
                .build();
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (properties.isCursor()) {
            return completeViaCursorAgent(systemPrompt, userPrompt);
        }
        return completeViaChatCompletions(systemPrompt, userPrompt);
    }

    private String completeViaChatCompletions(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "messages", java.util.List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        String raw = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(this::auth)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode root = mapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IllegalStateException("Empty LLM content: " + raw);
            }
            return content.asText();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse LLM response", e);
        }
    }

    private String completeViaCursorAgent(String systemPrompt, String userPrompt) {
        String promptText = systemPrompt + "\n\n" + userPrompt
                + "\n\nDo not edit files or use tools. Reply with the requested JSON only.";

        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("name", "Delivery RCA insight");
        createBody.put("prompt", Map.of("text", promptText));
        if (properties.getModel() != null && !properties.getModel().isBlank()) {
            createBody.put("model", Map.of("id", properties.getModel()));
        }

        String created = restClient.post()
                .uri("/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(this::auth)
                .body(createBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = mapper.readTree(created);
            String agentId = root.path("agent").path("id").asText(null);
            String runId = root.path("run").path("id").asText(null);
            if (runId == null || runId.isBlank()) {
                runId = root.path("agent").path("latestRunId").asText(null);
            }
            if (agentId == null || runId == null) {
                throw new IllegalStateException("Cursor create-agent response missing ids: " + created);
            }
            String result = pollRunResult(agentId, runId);
            archiveQuietly(agentId);
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cursor Cloud Agent call failed", e);
        }
    }

    private String pollRunResult(String agentId, String runId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + properties.getTimeoutSeconds() * 1000L;
        while (System.currentTimeMillis() < deadline) {
            String raw = restClient.get()
                    .uri("/v1/agents/{id}/runs/{runId}", agentId, runId)
                    .headers(this::auth)
                    .retrieve()
                    .body(String.class);
            JsonNode run;
            try {
                run = mapper.readTree(raw);
            } catch (Exception e) {
                throw new IllegalStateException("Invalid Cursor run JSON", e);
            }
            String status = run.path("status").asText("");
            if ("FINISHED".equalsIgnoreCase(status)) {
                String result = run.path("result").asText("");
                if (result.isBlank()) {
                    throw new IllegalStateException("Cursor run finished with empty result");
                }
                return result;
            }
            if ("ERROR".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)
                    || "EXPIRED".equalsIgnoreCase(status)) {
                throw new IllegalStateException("Cursor run ended with status " + status + ": " + raw);
            }
            Thread.sleep(2000);
        }
        throw new IllegalStateException("Timed out waiting for Cursor Cloud Agent run " + runId);
    }

    private void archiveQuietly(String agentId) {
        try {
            restClient.post()
                    .uri("/v1/agents/{id}/archive", agentId)
                    .headers(this::auth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.debug("Could not archive Cursor agent {}: {}", agentId, e.getMessage());
        }
    }

    private void auth(org.springframework.http.HttpHeaders headers) {
        if (!properties.hasKey()) {
            return;
        }
        String key = properties.getApiKey().trim();
        if (properties.isCursor()) {
            headers.setBasicAuth(key, "");
        } else {
            headers.setBearerAuth(key);
        }
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://api.cursor.com";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
