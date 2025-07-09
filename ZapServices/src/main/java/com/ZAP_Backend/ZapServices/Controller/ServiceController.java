package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.DataTransferObject.ServiceResponse;
import com.ZAP_Backend.ZapServices.Service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceController {
    @Autowired
    ServiceService serviceService;
//    User------->>>>>
    @PostMapping("/user/{userId}/service/{serviceId}/category/{categoryId}/provider{providerId}/")
    public ServiceResponse Servicerequest(
            @PathVariable Long userId,
            @PathVariable Long serviceId,
            @PathVariable Long categoryId,
            @RequestBody Long providerId ,@RequestBody String address) {
        return serviceService.serviceResponse(userId, serviceId, categoryId,providerId,address);
    }
}
