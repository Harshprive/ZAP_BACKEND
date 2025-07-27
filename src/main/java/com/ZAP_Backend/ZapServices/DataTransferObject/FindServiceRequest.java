package com.ZAP_Backend.ZapServices.DataTransferObject;

public class FindServiceRequest {
    private Long userId;
    private String service_name;
    private String category_name;
    private String Address;
    private Long excludeProviderId;


    public FindServiceRequest(Long userId, String service_name, String category_name, String address, Long excludeProviderId) {
        this.userId = userId;
        this.service_name = service_name;
        this.category_name = category_name;
        Address = address;
        this.excludeProviderId = excludeProviderId;
    }

    public FindServiceRequest() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getService_name() {
        return service_name;
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public Long getExcludeProviderId() {
        return excludeProviderId;
    }

    public void setExcludeProviderId(Long excludeProviderId) {
        this.excludeProviderId = excludeProviderId;
    }
}
