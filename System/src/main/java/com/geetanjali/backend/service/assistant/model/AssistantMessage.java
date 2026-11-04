package com.geetanjali.backend.service.assistant.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AssistantMessage: Represents a conversation message turn (user, assistant, system, tool).
 */
public class AssistantMessage {
    private String role; // "system", "user", "assistant", "tool"
    private String content;
    private List<ToolCall> toolCalls = new ArrayList<>();
    private String toolCallId;
    private String name;

    public AssistantMessage() {}

    public AssistantMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static AssistantMessage system(String content) {
        return new AssistantMessage("system", content);
    }

    public static AssistantMessage user(String content) {
        return new AssistantMessage("user", content);
    }

    public static AssistantMessage assistant(String content) {
        return new AssistantMessage("assistant", content);
    }

    public static AssistantMessage tool(String toolCallId, String name, String content) {
        AssistantMessage msg = new AssistantMessage("tool", content);
        msg.setToolCallId(toolCallId);
        msg.setName(name);
        return msg;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls != null ? toolCalls : new ArrayList<>();
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
