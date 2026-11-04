package com.geetanjali.backend.dto.assistant;

/**
 * ChatRequest: Inbound payload from the floating UI widget.
 */
public class ChatRequest {
    private String message;
    private PageContext context;

    public ChatRequest() {}

    public ChatRequest(String message, PageContext context) {
        this.message = message;
        this.context = context;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PageContext getContext() {
        return context;
    }

    public void setContext(PageContext context) {
        this.context = context;
    }
}
