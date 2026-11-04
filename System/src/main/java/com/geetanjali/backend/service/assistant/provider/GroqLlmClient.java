package com.geetanjali.backend.service.assistant.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geetanjali.backend.config.AssistantProperties;
import com.geetanjali.backend.service.assistant.model.AssistantMessage;
import com.geetanjali.backend.service.assistant.model.ToolCall;
import com.geetanjali.backend.service.assistant.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * GroqLlmClient: Native high-speed LLM client calling Groq's OpenAI-compatible completions API.
 * Strictly respects the 900 max output token limit requested for Groq.
 */
@Component
public class GroqLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GroqLlmClient.class);

    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GroqLlmClient(AssistantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    @Override
    public boolean isConfigured() {
        return properties.getGroq() != null && properties.getGroq().isConfigured();
    }

    @Override
    public AssistantMessage chat(List<AssistantMessage> messages, List<ToolDefinition> tools, int maxTokens) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Groq API key is not configured.");
        }

        String endpoint = properties.getGroq().getBaseUrl() + "/chat/completions";
        String apiKey = properties.getGroq().getApiKey().trim();
        String model = properties.getGroq().getModel();

        // Enforce max 900 tokens if not specified or larger
        int tokens = maxTokens > 0 ? Math.min(maxTokens, properties.getGroq().getMaxTokens()) : properties.getGroq().getMaxTokens();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", tokens);
        requestBody.put("temperature", 0.2);

        // Serialize conversation messages to OpenAI format
        List<Map<String, Object>> formattedMessages = new ArrayList<>();
        for (AssistantMessage msg : messages) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("role", msg.getRole());

            if ("tool".equals(msg.getRole())) {
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
                m.put("tool_call_id", msg.getToolCallId());
                if (msg.getName() != null) {
                    m.put("name", msg.getName());
                }
            } else if (msg.hasToolCalls()) {
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
                List<Map<String, Object>> calls = new ArrayList<>();
                for (ToolCall tc : msg.getToolCalls()) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("id", tc.getId());
                    c.put("type", "function");
                    Map<String, Object> fn = new LinkedHashMap<>();
                    fn.put("name", tc.getName());
                    fn.put("arguments", objectMapper.writeValueAsString(tc.getArguments()));
                    c.put("function", fn);
                    calls.add(c);
                }
                m.put("tool_calls", calls);
            } else {
                m.put("content", msg.getContent() != null ? msg.getContent() : "");
            }
            formattedMessages.add(m);
        }
        requestBody.put("messages", formattedMessages);

        // Format tools into OpenAI standard schema
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolList = new ArrayList<>();
            for (ToolDefinition td : tools) {
                Map<String, Object> toolObj = new LinkedHashMap<>();
                toolObj.put("type", "function");
                Map<String, Object> fnObj = new LinkedHashMap<>();
                fnObj.put("name", td.getName());
                fnObj.put("description", td.getDescription());
                fnObj.put("parameters", td.getParameters());
                toolObj.put("function", fnObj);
                toolList.add(toolObj);
            }
            requestBody.put("tools", toolList);
            requestBody.put("tool_choice", "auto");
        }

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.warn("Groq API returned HTTP {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Groq API error HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("Invalid response structure from Groq: " + response.body());
        }

        JsonNode messageNode = choices.get(0).path("message");
        String content = messageNode.path("content").asText(null);

        AssistantMessage result = new AssistantMessage("assistant", content != null ? content : "");

        JsonNode toolCallsNode = messageNode.path("tool_calls");
        if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
            List<ToolCall> toolCalls = new ArrayList<>();
            for (JsonNode tcNode : toolCallsNode) {
                String id = tcNode.path("id").asText();
                String name = tcNode.path("function").path("name").asText();
                String argsJson = tcNode.path("function").path("arguments").asText("{}");
                Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {});
                toolCalls.add(new ToolCall(id, name, args));
            }
            result.setToolCalls(toolCalls);
        }

        return result;
    }
}
