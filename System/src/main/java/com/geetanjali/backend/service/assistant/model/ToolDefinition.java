package com.geetanjali.backend.service.assistant.model;

import java.util.Map;

/**
 * ToolDefinition: JSON schema describing a tool/function available to the LLM.
 */
public class ToolDefinition {
    private String name;
    private String description;
    private Map<String, Object> parameters;

    public ToolDefinition() {}

    public ToolDefinition(String name, String description, Map<String, Object> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
