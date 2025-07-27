package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.DataTransferObject.ProviderLocationDTO;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ProviderLocationController {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/provider/location/update")
    public void updateProviderLocation(ProviderLocationDTO locationUpdate) {
        System.out.println("📍 Received location update from provider " + locationUpdate.getProviderId());
        System.out.println("   └─ Latitude: " + locationUpdate.getLatitude());
        System.out.println("   └─ Longitude: " + locationUpdate.getLongitude());

        try {
            // Update provider location in database
            ServiceProvider provider = providerRepository.findById(locationUpdate.getProviderId())
                    .orElseThrow(() -> new RuntimeException("Provider not found"));

            provider.setLatitude(locationUpdate.getLatitude());
            provider.setLongitude(locationUpdate.getLongitude());
            providerRepository.save(provider);

            // Send confirmation back to provider
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Location updated successfully");
            response.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(
                "/topic/provider/" + locationUpdate.getProviderId() + "/location/status",
                response
            );

            System.out.println("✅ Location updated successfully for provider " + locationUpdate.getProviderId());
        } catch (Exception e) {
            System.out.println("❌ Error updating location: " + e.getMessage());
            
            // Send error response back to provider
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to update location: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(
                "/topic/provider/" + locationUpdate.getProviderId() + "/location/status",
                errorResponse
            );
        }
    }
} 