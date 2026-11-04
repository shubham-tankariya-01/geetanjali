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
 * GeminiLlmClient: High-accuracy fallback LLM client calling Google Gemini's generateContent API.
 */
@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GeminiLlmClient(AssistantProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return properties.getGemini() != null && properties.getGemini().isConfigured();
    }

    @Override
    public AssistantMessage chat(List<AssistantMessage> messages, List<ToolDefinition> tools, int maxTokens) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String apiKey = properties.getGemini().getApiKey().trim();
        String model = properties.getGemini().getModel();
        String endpoint = properties.getGemini().getBaseUrl() + "/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = new LinkedHashMap<>();

        // Separate system messages from contents
        StringBuilder systemText = new StringBuilder();
        List<Map<String, Object>> contents = new ArrayList<>();

        for (AssistantMessage msg : messages) {
            if ("system".equalsIgnoreCase(msg.getRole())) {
                if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                    if (systemText.length() > 0) systemText.append("\n\n");
                    systemText.append(msg.getContent());
                }
            } else if ("user".equalsIgnoreCase(msg.getRole())) {
                Map<String, Object> contentObj = new LinkedHashMap<>();
                contentObj.put("role", "user");
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> textPart = new LinkedHashMap<>();
                textPart.put("text", msg.getContent() != null ? msg.getContent() : "");
                parts.add(textPart);
                contentObj.put("parts", parts);
                contents.add(contentObj);
            } else if ("assistant".equalsIgnoreCase(msg.getRole())) {
                Map<String, Object> contentObj = new LinkedHashMap<>();
                contentObj.put("role", "model");
                List<Map<String, Object>> parts = new ArrayList<>();

                if (msg.hasToolCalls()) {
                    for (ToolCall tc : msg.getToolCalls()) {
                        if (tc.getThoughtSignature() != null && !tc.getThoughtSignature().isEmpty()) {
                            Map<String, Object> fnPart = new LinkedHashMap<>();
                            Map<String, Object> fnCall = new LinkedHashMap<>();
                            fnCall.put("name", tc.getName());
                            fnCall.put("args", tc.getArguments() != null ? tc.getArguments() : Collections.emptyMap());
                            fnPart.put("functionCall", fnCall);
                            fnPart.put("thoughtSignature", tc.getThoughtSignature());
                            parts.add(fnPart);
                        } else {
                            Map<String, Object> textPart = new LinkedHashMap<>();
                            textPart.put("text", "Checking studio records for " + tc.getName());
                            parts.add(textPart);
                        }
                    }
                } else {
                    Map<String, Object> textPart = new LinkedHashMap<>();
                    textPart.put("text", msg.getContent() != null ? msg.getContent() : "");
                    parts.add(textPart);
                }
                contentObj.put("parts", parts);
                contents.add(contentObj);
            } else if ("tool".equalsIgnoreCase(msg.getRole())) {
                Map<String, Object> contentObj = new LinkedHashMap<>();
                contentObj.put("role", "user");
                List<Map<String, Object>> parts = new ArrayList<>();
                Map<String, Object> textPart = new LinkedHashMap<>();
                textPart.put("text", "Database observation for " + (msg.getName() != null ? msg.getName() : "action") + ": " + msg.getContent());
                parts.add(textPart);
                contentObj.put("parts", parts);
                contents.add(contentObj);
            }
        }

        if (systemText.length() > 0) {
            Map<String, Object> sysInstruction = new LinkedHashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> textPart = new LinkedHashMap<>();
            textPart.put("text", systemText.toString());
            parts.add(textPart);
            sysInstruction.put("parts", parts);
            requestBody.put("systemInstruction", sysInstruction);
        }

        requestBody.put("contents", contents);

        // Tool declarations in Gemini format
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> declarations = new ArrayList<>();
            for (ToolDefinition td : tools) {
                Map<String, Object> decl = new LinkedHashMap<>();
                decl.put("name", td.getName());
                decl.put("description", td.getDescription());
                if (td.getParameters() != null) {
                    decl.put("parameters", td.getParameters());
                }
                declarations.add(decl);
            }
            Map<String, Object> toolsWrapper = new LinkedHashMap<>();
            toolsWrapper.put("functionDeclarations", declarations);
            requestBody.put("tools", List.of(toolsWrapper));
        }

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle temporary 503 / high demand spikes on experimental models by falling back to stable gemini-3.5-flash
        if (response.statusCode() == 503 || (response.statusCode() >= 400 && response.body().contains("experiencing high demand"))) {
            if (!"gemini-3.5-flash".equalsIgnoreCase(model)) {
                log.warn("Gemini model '{}' is experiencing high demand (HTTP 503). Retrying automatically with stable production model: gemini-3.5-flash", model);
                String fallbackEndpoint = properties.getGemini().getBaseUrl() + "/models/gemini-3.5-flash:generateContent?key=" + apiKey;
                HttpRequest fallbackReq = HttpRequest.newBuilder()
                        .uri(URI.create(fallbackEndpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(20))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();
                response = httpClient.send(fallbackReq, HttpResponse.BodyHandlers.ofString());
            }
        }

        if (response.statusCode() >= 400) {
            log.warn("Gemini API returned HTTP {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API error HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new RuntimeException("No candidates returned from Gemini: " + response.body());
        }

        JsonNode partsNode = candidates.get(0).path("content").path("parts");
        AssistantMessage result = new AssistantMessage("assistant", "");
        StringBuilder textAccumulator = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();

        if (partsNode.isArray()) {
            for (JsonNode part : partsNode) {
                if (part.has("text")) {
                    textAccumulator.append(part.path("text").asText());
                }
                if (part.has("functionCall")) {
                    JsonNode fnCall = part.path("functionCall");
                    String fnName = fnCall.path("name").asText();
                    Map<String, Object> args = new LinkedHashMap<>();
                    if (fnCall.has("args")) {
                        args = objectMapper.convertValue(fnCall.path("args"), new TypeReference<Map<String, Object>>() {});
                    }
                    String callId = "call_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                    ToolCall tc = new ToolCall(callId, fnName, args);
                    if (part.has("thoughtSignature")) {
                        tc.setThoughtSignature(part.path("thoughtSignature").asText());
                    }
                    toolCalls.add(tc);
                }
            }
        }

        result.setContent(textAccumulator.toString().trim());
        if (!toolCalls.isEmpty()) {
            result.setToolCalls(toolCalls);
        }

        return result;
    }
}
