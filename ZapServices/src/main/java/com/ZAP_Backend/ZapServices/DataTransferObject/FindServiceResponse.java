package com.ZAP_Backend.ZapServices.DataTransferObject;

import com.ZAP_Backend.ZapServices.Model.Category;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;

public class FindServiceResponse {

    private ServiceProvider serviceProvider;
    private Category category;


    public FindServiceResponse(ServiceProvider serviceProvider, Category category) {
        this.serviceProvider = serviceProvider;
        this.category = category;
    }

    public FindServiceResponse() {
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
