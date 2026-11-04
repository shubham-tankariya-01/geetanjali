package com.geetanjali.backend.service.assistant.model;

/**
 * ToolResult: Encapsulates the execution output of a database operational tool.
 */
public class ToolResult {
    private final boolean success;
    private final Object data;
    private final String errorMessage;

    private ToolResult(boolean success, Object data, String errorMessage) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static ToolResult ok(Object data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
