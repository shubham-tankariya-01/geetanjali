package com.geetanjali.backend.dto.assistant;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatResponse: Outbound payload sent back to the floating UI widget.
 */
public class ChatResponse {
    private String reply;
    private String provider; // "groq", "gemini", "system"
    private List<SuggestedAction> actions = new ArrayList<>();

    public ChatResponse() {}

    public ChatResponse(String reply, String provider) {
        this.reply = reply;
        this.provider = provider;
    }

    public ChatResponse(String reply, String provider, List<SuggestedAction> actions) {
        this.reply = reply;
        this.provider = provider;
        this.actions = actions != null ? actions : new ArrayList<>();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<SuggestedAction> getActions() {
        return actions;
    }

    public void setActions(List<SuggestedAction> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
    }
}
