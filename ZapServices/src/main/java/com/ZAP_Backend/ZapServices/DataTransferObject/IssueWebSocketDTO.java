package com.ZAP_Backend.ZapServices.DataTransferObject;

import com.ZAP_Backend.ZapServices.Model.Issue;

public class IssueWebSocketDTO {
    private Long issueId;
    private String mediaName;
    private String mediaType;
    private String mediaCategory;
    private String description;
    private int weekNumber;
    private Long userId;
    private Long serviceId;
    private String status;
    private String action; // "NEW_ISSUE", "ACCEPT", "REJECT", "REATTACHMENT"
    private Long providerId;

    public IssueWebSocketDTO() {
    }

    public static IssueWebSocketDTO fromIssue(Issue issue, String action) {
        IssueWebSocketDTO dto = new IssueWebSocketDTO();
        dto.setIssueId(issue.getId());
        dto.setMediaName(issue.getMediaName());
        dto.setMediaType(issue.getMediaType());
        dto.setMediaCategory(issue.getMediaCategory());
        dto.setDescription(issue.getDescription());
        dto.setWeekNumber(issue.getWeekNumber());
        dto.setUserId(issue.getUser().getId());
        dto.setServiceId(issue.getService().getId());
        dto.setStatus(issue.getStatus());
        dto.setAction(action);
        if (issue.getServiceProvider() != null) {
            dto.setProviderId(issue.getServiceProvider().getId());
        }
        return dto;
    }

    // Getters and Setters
    public Long getIssueId() {
        return issueId;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaCategory() {
        return mediaCategory;
    }

    public void setMediaCategory(String mediaCategory) {
        this.mediaCategory = mediaCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }
} 