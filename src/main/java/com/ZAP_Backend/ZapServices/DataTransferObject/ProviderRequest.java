package com.ZAP_Backend.ZapServices.DataTransferObject;

public class ProviderRequest {
    private Long userId;
    private Long serviceId;
    private Long categoryId;
    private Long providerId;
    private String serviceName;
    private String categoryName;

    public ProviderRequest(Long userId, Long serviceId, Long categoryId, Long providerId) {
        this.userId = userId;
        this.serviceId = serviceId;
        this.categoryId = categoryId;
        this.providerId = providerId;
    }

    public ProviderRequest() {
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}