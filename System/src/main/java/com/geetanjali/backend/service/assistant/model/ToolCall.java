package com.geetanjali.backend.service.assistant.model;

import java.util.Map;

/**
 * ToolCall: Represents a function/tool call request emitted by an LLM.
 */
public class ToolCall {
    private String id;
    private String name;
    private Map<String, Object> arguments;
    private String thoughtSignature;

    public ToolCall() {}

    public ToolCall(String id, String name, Map<String, Object> arguments) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
    }

    public ToolCall(String id, String name, Map<String, Object> arguments, String thoughtSignature) {
        this.id = id;
        this.name = name;
        this.arguments = arguments;
        this.thoughtSignature = thoughtSignature;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public String getThoughtSignature() {
        return thoughtSignature;
    }

    public void setThoughtSignature(String thoughtSignature) {
        this.thoughtSignature = thoughtSignature;
    }
}
