package com.ZAP_Backend.ZapServices.DataTransferObject;

public class ServiceResponse {
    private String service_name;
    private String service_category;
    private String provide_name;
    private String status;
    private String date_coming;

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    private String Address;

    public String getService_name() {
        return service_name;
    }

    public void setService_name(String service_name) {
        this.service_name = service_name;
    }

    public String getService_category() {
        return service_category;
    }

    public void setService_category(String service_category) {
        this.service_category = service_category;
    }

    public String getProvide_name() {
        return provide_name;
    }

    public void setProvide_name(String provide_name) {
        this.provide_name = provide_name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate_coming() {
        return date_coming;
    }

    public void setDate_coming(String date_coming) {
        this.date_coming = date_coming;
    }
}
