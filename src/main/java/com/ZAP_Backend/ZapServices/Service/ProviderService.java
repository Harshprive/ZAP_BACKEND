package com.ZAP_Backend.ZapServices.Service;

import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderService {
    @Autowired
    private ProviderRepository providerRepository;

    public ServiceProvider create_provider(ServiceProvider provider){
        return providerRepository.save(provider);
    }

    public ServiceProvider FindById(Long id){
         ServiceProvider prov = providerRepository.findById(id).orElseThrow();
        return prov;
    }
    public List<ServiceProvider> get_all_service_provider(){
        return providerRepository.findAll();
    }

    public List<ServiceProvider> findByServiceId(Long serviceId) {
        return providerRepository.findByServiceId(serviceId);
    }
}
