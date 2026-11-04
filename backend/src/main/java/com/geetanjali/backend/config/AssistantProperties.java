package com.geetanjali.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AssistantProperties: Configuration properties for the Floating AI Assistant.
 * Maps app.assistant.* properties from application.properties / environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "app.assistant")
public class AssistantProperties {

    private GroqProperties groq = new GroqProperties();
    private GeminiProperties gemini = new GeminiProperties();
    private int rollingWindowSize = 10;

    public GroqProperties getGroq() {
        return groq;
    }

    public void setGroq(GroqProperties groq) {
        this.groq = groq;
    }

    public GeminiProperties getGemini() {
        return gemini;
    }

    public void setGemini(GeminiProperties gemini) {
        this.gemini = gemini;
    }

    public int getRollingWindowSize() {
        return rollingWindowSize;
    }

    public void setRollingWindowSize(int rollingWindowSize) {
        this.rollingWindowSize = rollingWindowSize;
    }

    public static class GroqProperties {
        private String apiKey = "";
        private String model = "openai/gpt-oss-120b";
        private int maxTokens = 900;
        private String baseUrl = "https://api.groq.com/openai/v1";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.trim().isEmpty();
        }
    }

    public static class GeminiProperties {
        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.trim().isEmpty();
        }
    }
}
