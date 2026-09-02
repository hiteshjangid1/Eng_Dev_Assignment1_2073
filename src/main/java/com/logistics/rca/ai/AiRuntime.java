package com.logistics.rca.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AiRuntime {

    private static final Logger log = LoggerFactory.getLogger(AiRuntime.class);

    private final AiProperties properties;
    private volatile boolean applicationReady;

    public AiRuntime(AiProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        applicationReady = true;
        if (properties.isEnabled() && properties.hasKey()) {
            log.info("AI insights enabled via Cursor Cloud Agents (model={}, baseUrl={})", properties.getModel(), properties.getBaseUrl());
        } else if (properties.isEnabled()) {
            log.info("AI insights configured but no API key — using rule-based narratives. Set CURSOR_API_KEY from https://cursor.com/dashboard");
        } else {
            log.info("AI insights disabled (rca.ai.enabled=false)");
        }
    }

    public boolean shouldCallLlm() {
        if (!properties.isEnabled() || !properties.hasKey()) {
            return false;
        }
        return properties.isOnStartup() || applicationReady;
    }

    public boolean configured() {
        return properties.isEnabled() && properties.hasKey();
    }
}
