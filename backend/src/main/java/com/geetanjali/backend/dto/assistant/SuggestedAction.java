package com.geetanjali.backend.dto.assistant;

/**
 * SuggestedAction: A clickable chip/button that navigates the user directly to a relevant page or action.
 */
public class SuggestedAction {
    private String label;
    private String url;
    private String actionType; // "LINK", "PROMPT"

    public SuggestedAction() {}

    public SuggestedAction(String label, String url, String actionType) {
        this.label = label;
        this.url = url;
        this.actionType = actionType;
    }

    public static SuggestedAction link(String label, String url) {
        return new SuggestedAction(label, url, "LINK");
    }

    public static SuggestedAction prompt(String label, String promptText) {
        return new SuggestedAction(label, promptText, "PROMPT");
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
}
