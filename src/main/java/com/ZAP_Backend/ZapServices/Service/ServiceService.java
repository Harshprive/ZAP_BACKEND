package com.ZAP_Backend.ZapServices.Service;


import com.ZAP_Backend.ZapServices.DataTransferObject.*;
import com.ZAP_Backend.ZapServices.Model.Category;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Model.Servicee;
import com.ZAP_Backend.ZapServices.Model.User;
import com.ZAP_Backend.ZapServices.Model.Booking;
import com.ZAP_Backend.ZapServices.Model.Bookeds;
import com.ZAP_Backend.ZapServices.Model.ZapAmount;
import com.ZAP_Backend.ZapServices.Repository.CategoryRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import com.ZAP_Backend.ZapServices.Repository.ServiceRepository;
import com.ZAP_Backend.ZapServices.Repository.UserRepository;
import com.ZAP_Backend.ZapServices.Repository.BookingRepository;
import com.ZAP_Backend.ZapServices.Repository.BookedsRepository;
import com.ZAP_Backend.ZapServices.Repository.ZapAmountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.Random;

@Service
public class ServiceService {
    @Autowired
    ProviderRepository providerRepository;
    @Autowired
    GeocodingService geocodingService;


    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Long, CompletableFuture<String>> responseFutures = new ConcurrentHashMap<>();

    @Autowired
    public ServiceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    public void sendUpdate(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ServiceRepository serviceRepository;
    @Autowired
    UserRepository userRepository;


    @Autowired
    ProviderService providerService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookedsRepository bookedsRepository;

    @Autowired
    private ZapAmountRepository zapAmountRepository;

    @Autowired
    private WebSocketConnectionService webSocketConnectionService;

    @Autowired
    private LocationService locationService;

    // Helper method to generate OTP
    private String generateOTP() {
        return String.format("%04d", (int)(Math.random() * 10000));
    }

    public FindServiceResponse findService(Long userId, Long serviceId, Long categoryId, Double userLat, Double userLon) {
        System.out.println("\n🔎 Finding service for:");
        System.out.println("   └─ User ID: " + userId);
        System.out.println("   └─ Service ID: " + serviceId);
        System.out.println("   └─ Category ID: " + categoryId);
        System.out.println("   └─ User Location: " + userLat + ", " + userLon);

        // Get all providers for this service
        List<ServiceProvider> providers = providerRepository.findByServiceId(serviceId);
        System.out.println("📋 Total providers found: " + providers.size());
        
        // Filter only connected providers and within radius (10km by default)
        final double MAX_RADIUS_KM = 10.0;  // You can adjust this value or make it configurable
        List<ServiceProvider> eligibleProviders = providers.stream()
            .filter(provider -> {
                boolean isConnected = webSocketConnectionService.isProviderConnected(provider.getId());
                boolean isInRange = false;
                
                if (provider.getLatitude() != null && provider.getLongitude() != null && userLat != null && userLon != null) {
                    isInRange = locationService.isProviderWithinRadius(
                        userLat, userLon,
                        provider.getLatitude(), provider.getLongitude(),
                        MAX_RADIUS_KM
                    );
                }
                
                System.out.println("   └─ Provider " + provider.getId() + 
                                 " connected: " + isConnected +
                                 " in range: " + isInRange);
                                 
                return isConnected && (isInRange || userLat == null || userLon == null);
            })
            .collect(Collectors.toList());

        System.out.println("🌐 Eligible providers: " + eligibleProviders.size());

        if (eligibleProviders.isEmpty()) {
            System.out.println("❌ No eligible providers available");
            return null;
        }

        // Get a random eligible provider
        Random random = new Random();
        ServiceProvider selectedProvider = eligibleProviders.get(random.nextInt(eligibleProviders.size()));
        System.out.println("✅ Selected provider: " + selectedProvider.getId());
        
        Category category = categoryRepository.findById(categoryId).orElse(null);
        
        if (category == null) {
            System.out.println("❌ Category not found: " + categoryId);
            return null;
        }

        return new FindServiceResponse(selectedProvider, category);
    }

    public FindServiceResponse findNewProvider(Long userId, Long serviceId, Long categoryId, Long excludeProviderId, Double userLat, Double userLon) {
        // Get all providers for this service
        List<ServiceProvider> providers = providerRepository.findByServiceId(serviceId);
        
        // Filter connected providers, exclude specified provider, and check location
        final double MAX_RADIUS_KM = 10.0;
        List<ServiceProvider> eligibleProviders = providers.stream()
            .filter(provider -> webSocketConnectionService.isProviderConnected(provider.getId()))
            .filter(provider -> !provider.getId().equals(excludeProviderId))
            .filter(provider -> {
                if (provider.getLatitude() != null && provider.getLongitude() != null && userLat != null && userLon != null) {
                    return locationService.isProviderWithinRadius(
                        userLat, userLon,
                        provider.getLatitude(), provider.getLongitude(),
                        MAX_RADIUS_KM
                    );
                }
                return true;  // Include if location data is missing
            })
            .collect(Collectors.toList());

        if (eligibleProviders.isEmpty()) {
            return null;
        }

        // Get a random provider from the filtered list
        Random random = new Random();
        ServiceProvider selectedProvider = eligibleProviders.get(random.nextInt(eligibleProviders.size()));
        
        Category category = categoryRepository.findById(categoryId).orElse(null);
        
        if (category == null) {
            return null;
        }

        return new FindServiceResponse(selectedProvider, category);
    }

//     ----------> send to Provider App
public ServiceResponse serviceResponse(Long userId, Long serviceId, Long categoryId, Long providerId ,String address) {
    // 1. Prepare CompletableFuture
    CompletableFuture<String> future = new CompletableFuture<>();
    responseFutures.put(providerId, future);

    // 2. Prepare data
    ProviderRequest req = new ProviderRequest(userId, serviceId, categoryId, providerId);
    ServiceProvider providerData = providerRepository.findById(providerId)
            .orElseThrow(() -> new RuntimeException("Provider not found"));
    Servicee serviceData = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new RuntimeException("Service not found"));
    Category categoryData = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found"));
    User userData = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    // Set service and category names
    req.setServiceName(serviceData.getServiceName());
    req.setCategoryName(categoryData.getCategory_name());

    // 3. Send request via WebSocket
    try {
        messagingTemplate.convertAndSend("/topic/service/request/" + providerId, req);
        System.out.println("📤 Sent request to provider " + providerId);
    } catch (Exception e) {
        System.out.println("❌ Error sending WebSocket message: " + e.getMessage());
        throw new RuntimeException("Failed to send request to provider");
    }

    // 4. Wait for response
    ServiceResponse res = new ServiceResponse();
    try {
        String status = future.get(30, TimeUnit.SECONDS); // reduced timeout to 30 seconds
        System.out.println("✅ Provider responded with: " + status);

        res.setService_name(serviceData.getServiceName());
        res.setService_category(categoryData.getCategory_name());
        res.setProvide_name(providerData.getProvider_name());
        res.setStatus(status);
        
        if ("ACCEPTED".equals(status)) {
            // Create Bookeds entry
            Bookeds booked = new Bookeds();

            Booking booking = new Booking(); // new 
            booking.setUser(userData);
            booking.setProvider(providerData);
            booking.setServiceName(serviceData.getServiceName());
            booking.setServiceCategory(categoryData.getCategory_name());
            booking.setStatus("ACCEPTED");
            booking.setBookingDate(LocalDateTime.now());
            booking.setScheduledDate(LocalDateTime.now().plusDays(1)); // Schedule for tomorrow by default
            booking.setCreatedAt(LocalDateTime.now());
            booking.setService(serviceData);
            booking.setCategory(categoryData);
            booking.setServiceProvider(providerData);
            booking.setBookedAddress(address);


            booked.setProviderId(providerId);
            booked.setUserId(userId);
            booked.setServiceType("SERVICE");
            booked.setServiceId(serviceId);
            booked.setCategoryId(categoryId);
            booked.setAddress(address);
//             new
            GeocodingResponse georesp = geocodingService.geocodeAddress(address);
            booked.setLatitude(Double.parseDouble(georesp.getLatitude()));
            booked.setLongitude(Double.parseDouble(georesp.getLongitude()));
            booked.setStatus("ARRIVING");
            booked.setCancel(false);
            booked.setOtp(generateOTP());


            // Get ZapAmount for the service
            ZapAmount zapAmount = zapAmountRepository.findById(serviceId)
                .orElse(null);
            
            if (zapAmount != null) {
                booked.setFixedAmount(zapAmount.getAmount());
            }

            // Save the booking
            bookedsRepository.save(booked);
             // Save booking
             bookingRepository.save(booking);

            // Format date as ISO-8601
            res.setDate_coming(LocalDateTime.now().plusDays(1).toString());
            
            // Notify the provider about acceptance
            try {
                messagingTemplate.convertAndSend(
                    "/topic/service/confirmation/" + providerId,
                    Map.of(
                        "status", "CONFIRMED",
                        "userId", userId,
                        "bookingId", booked.getId(),
                        "otp", booked.getOtp()
                    )
                );
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not send confirmation to provider: " + e.getMessage());
            }

            // Notify the user about booking confirmation
            try {
                messagingTemplate.convertAndSend(
                    "/topic/user/" + userId + "/bookings",
                    Map.of(
                        "message", "Your booking has been confirmed with " + providerData.getProvider_name() + " for " + serviceData.getServiceName(),
                        "bookingId", booked.getId(),
                        "status", "CONFIRMED",
                        "scheduledDate", LocalDateTime.now().plusDays(1).toString(),
                        "providerName", providerData.getProvider_name(),
                        "otp", booked.getOtp()
                    )
                );
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not send confirmation to user: " + e.getMessage());
            }
        }

    } catch (TimeoutException e) {
        System.out.println("⏰ Provider timeout.");
        res.setStatus("TIMEOUT");
    } catch (Exception e) {
        System.out.println("❌ Error: " + e.getMessage());
        res.setStatus("ERROR");
    } finally {
        responseFutures.remove(providerId); // clean up
    }

    return res;
}

    @MessageMapping("/service/response")
    public void receiveProviderResponse(ProviderResponse resp) {
        System.out.println("📥 [RECEIVED] WebSocket response from provider:");
        System.out.println("   └─ Provider ID : " + resp.getProviderId());
        System.out.println("   └─ Accepted    : " + resp.isAccepted());

        CompletableFuture<String> future = responseFutures.get(resp.getProviderId());

        if (future != null) {
            String result = resp.isAccepted() ? "ACCEPTED" : "REJECTED";
            System.out.println("✅ Completing future with status: " + result);
            future.complete(result);
        } else {
            System.out.println("❌ No pending request found for provider ID: " + resp.getProviderId());
        }
    }
}