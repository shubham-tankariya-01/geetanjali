package com.geetanjali.backend.service.assistant.provider;

import com.geetanjali.backend.service.assistant.model.AssistantMessage;
import com.geetanjali.backend.service.assistant.model.ToolDefinition;
import java.util.List;

/**
 * LlmClient: Common contract for external LLM inference providers.
 */
public interface LlmClient {

    /**
     * Provider identifier (e.g. "groq", "gemini").
     */
    String getProviderName();

    /**
     * Whether valid API credentials are configured.
     */
    boolean isConfigured();

    /**
     * Generates a conversational turn or tool execution request.
     *
     * @param messages Full or rolling message history.
     * @param tools Predefined operational tools available to the model.
     * @param maxTokens Maximum completion tokens allowed (e.g. 900 for Groq).
     * @return Model response message (either content or toolCalls).
     */
    AssistantMessage chat(List<AssistantMessage> messages, List<ToolDefinition> tools, int maxTokens) throws Exception;
}
