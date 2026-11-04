package com.geetanjali.backend.service.assistant.provider;

import com.geetanjali.backend.service.assistant.model.AssistantMessage;
import com.geetanjali.backend.service.assistant.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FallbackLlmClient: Composite resilience provider.
 * Executes Groq as the native high-speed engine (with 900 max tokens).
 * Automatically catches network errors, 429 rate limits, or missing keys and falls back to Gemini.
 */
@Component
@Primary
public class FallbackLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackLlmClient.class);

    private final GroqLlmClient groqClient;
    private final GeminiLlmClient geminiClient;
    private final ThreadLocal<String> lastProviderUsed = new ThreadLocal<>();

    public FallbackLlmClient(GroqLlmClient groqClient, GeminiLlmClient geminiClient) {
        this.groqClient = groqClient;
        this.geminiClient = geminiClient;
    }

    @Override
    public String getProviderName() {
        String active = lastProviderUsed.get();
        return active != null ? active : (groqClient.isConfigured() ? "groq" : "gemini");
    }

    @Override
    public boolean isConfigured() {
        return groqClient.isConfigured() || geminiClient.isConfigured();
    }

    @Override
    public AssistantMessage chat(List<AssistantMessage> messages, List<ToolDefinition> tools, int maxTokens) throws Exception {
        lastProviderUsed.remove();

        // 1. Try Groq (Primary Native Provider)
        if (groqClient.isConfigured()) {
            try {
                log.debug("Attempting LLM chat with primary provider: Groq (maxTokens: {})", maxTokens);
                AssistantMessage reply = groqClient.chat(messages, tools, maxTokens);
                lastProviderUsed.set("groq");
                return reply;
            } catch (Exception ex) {
                log.warn("Groq inference failed or was rate limited: {}. Seamlessly falling back to Gemini.", ex.getMessage());
            }
        } else {
            log.debug("Groq is not configured (missing GROQ_API_KEY). Checking Gemini fallback...");
        }

        // 2. Fallback to Gemini
        if (geminiClient.isConfigured()) {
            try {
                log.debug("Attempting LLM chat with fallback provider: Gemini");
                AssistantMessage reply = geminiClient.chat(messages, tools, maxTokens);
                lastProviderUsed.set("gemini");
                return reply;
            } catch (Exception ex) {
                log.error("Gemini fallback also failed: {}", ex.getMessage());
                throw new RuntimeException("Both Groq and Gemini providers failed: " + ex.getMessage(), ex);
            }
        }

        throw new IllegalStateException("Neither GROQ_API_KEY nor GEMINI_API_KEY is configured in your environment.");
    }
}
