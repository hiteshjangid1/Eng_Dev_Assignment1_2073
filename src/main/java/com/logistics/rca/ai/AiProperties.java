package com.logistics.rca.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rca.ai")
public class AiProperties {

    /**
     * Master switch. Even when true, the LLM is skipped if no API key is set.
     */
    private boolean enabled = true;

    /**
     * {@code cursor} uses Cloud Agents on api.cursor.com. {@code openai} uses Chat Completions.
     */
    private String provider = "cursor";

    /**
     * Cursor: set env CURSOR_API_KEY (dashboard → API Keys). Never commit the key.
     */
    private String apiKey = "";

    private String baseUrl = "https://api.cursor.com";

    private String model = "composer-2.5";

    /**
     * If false (default), startup DemoRunner stays on rule-based text so boot is fast/offline.
     * HTTP calls still use the LLM once the app is ready.
     */
    private boolean onStartup = false;

    private int timeoutSeconds = 180;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isCursor() {
        return provider == null || provider.isBlank() || "cursor".equalsIgnoreCase(provider);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isOnStartup() {
        return onStartup;
    }

    public void setOnStartup(boolean onStartup) {
        this.onStartup = onStartup;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
