package com.geetanjali.backend.dto.assistant;

/**
 * PageContext: Captures the active webpage state where the user opened the assistant.
 */
public class PageContext {
    private String currentPath;
    private String entityType; // "ORDER", "CUSTOMER", "DASHBOARD", "SETTINGS", etc.
    private Long entityId;
    private Long orderNo;

    public PageContext() {}

    public PageContext(String currentPath, String entityType, Long entityId, Long orderNo) {
        this.currentPath = currentPath;
        this.entityType = entityType;
        this.entityId = entityId;
        this.orderNo = orderNo;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Long orderNo) {
        this.orderNo = orderNo;
    }
}
