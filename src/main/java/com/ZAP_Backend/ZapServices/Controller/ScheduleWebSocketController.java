package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.Model.Schedule;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Repository.ScheduleRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import com.ZAP_Backend.ZapServices.Service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.List;

@Controller
public class ScheduleWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public void broadcastNewSchedule(Schedule schedule) {
        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("action", "NEW_SCHEDULE");
        scheduleData.put("scheduleId", schedule.getId());
        scheduleData.put("userId", schedule.getUser().getId());
        scheduleData.put("serviceName", schedule.getService().getServiceName());
        scheduleData.put("categoryName", schedule.getCategory().getCategory_name());
        scheduleData.put("date", schedule.getScheduledDateTime().toLocalDate().toString());
        scheduleData.put("time", schedule.getScheduledDateTime().toLocalTime().toString());
        scheduleData.put("address", schedule.getAddress());

        // Get all providers for this service
        List<ServiceProvider> providers = providerService.findByServiceId(schedule.getService().getId());
        
        System.out.println("📝 Broadcasting schedule request to " + providers.size() + " providers for service ID: " + schedule.getService().getId());
        
        // Broadcast to the service topic for all providers
        messagingTemplate.convertAndSend("/topic/service/" + schedule.getService().getId() + "/schedules", scheduleData);
        
        // Also send to each provider's individual topic
        for (ServiceProvider provider : providers) {
            messagingTemplate.convertAndSend("/topic/service/provider/" + provider.getId() + "/schedules", scheduleData);
        }

        // Notify the user that their schedule request has been created
        try {
            messagingTemplate.convertAndSend(
                "/topic/user/" + schedule.getUser().getId() + "/schedules",
                Map.of(
                    "message", "Your schedule request has been created and providers have been notified",
                    "scheduleId", schedule.getId(),
                    "status", "CREATED",
                    "action", "NEW_SCHEDULE",
                    "serviceName", schedule.getService().getServiceName(),
                    "date", schedule.getScheduledDateTime().toLocalDate().toString(),
                    "time", schedule.getScheduledDateTime().toLocalTime().toString()
                )
            );
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not send schedule creation notification to user: " + e.getMessage());
        }
    }

    @MessageMapping("/schedule/response")
    @Transactional
    public void handleScheduleResponse(@Payload Map<String, Object> response) {
        try {
            // Get the raw objects first
            final Object scheduleIdObj = response.get("scheduleId");
            final Object providerIdObj = response.get("providerId");
            final String action = (String) response.get("action");

            // Convert to Long values
            final Long scheduleId;
            final Long providerId;

            // Convert scheduleId
            if (scheduleIdObj instanceof String) {
                scheduleId = Long.parseLong((String) scheduleIdObj);
            } else if (scheduleIdObj instanceof Number) {
                scheduleId = ((Number) scheduleIdObj).longValue();
            } else {
                System.out.println("❌ Invalid schedule ID type: " + (scheduleIdObj != null ? scheduleIdObj.getClass() : "null"));
                return;
            }

            // Convert providerId
            if (providerIdObj instanceof String) {
                providerId = Long.parseLong((String) providerIdObj);
            } else if (providerIdObj instanceof Number) {
                providerId = ((Number) providerIdObj).longValue();
            } else {
                System.out.println("❌ Invalid provider ID type: " + (providerIdObj != null ? providerIdObj.getClass() : "null"));
                return;
            }

            System.out.println("📝 Processing schedule response - Schedule ID: " + scheduleId + ", Provider ID: " + providerId + ", Action: " + action);

            // Use transactionTemplate to ensure transaction boundaries
            transactionTemplate.execute(status -> {
                Optional<Schedule> scheduleOpt = scheduleRepository.findById(scheduleId);
                Optional<ServiceProvider> providerOpt = providerRepository.findByIdWithService(providerId);

                if (!scheduleOpt.isPresent()) {
                    System.out.println("❌ Schedule not found with ID: " + scheduleId);
                    return null;
                }
                if (!providerOpt.isPresent()) {
                    System.out.println("❌ Provider not found with ID: " + providerId);
                    return null;
                }

                Schedule schedule = scheduleOpt.get();
                ServiceProvider provider = providerOpt.get();

                System.out.println("📝 Found Schedule: " + schedule.getId() + " and Provider: " + provider.getId());

                switch (action) {
                    case "ACCEPT":
                        // First check if the schedule is still available
                        if (schedule.getProvider() != null) {
                            System.out.println("❌ Schedule already taken by provider: " + schedule.getProvider().getId());
                            // Schedule already taken by another provider
                            messagingTemplate.convertAndSend(
                                "/topic/service/schedule/confirmation/" + providerId,
                                Map.of(
                                    "status", "ALREADY_TAKEN",
                                    "scheduleId", schedule.getId()
                                )
                            );
                            return null;
                        }

                        // Update schedule with accepted provider
                        schedule.setProvider(provider);
                        schedule.setStatus("ACCEPTED");
                        System.out.println("📝 Setting provider " + provider.getId() + " for schedule " + schedule.getId());
                        
                        // Flush to ensure the changes are persisted
                        entityManager.flush();
                        
                        Schedule savedSchedule = scheduleRepository.save(schedule);
                        System.out.println("✅ Saved schedule. Provider ID after save: " + 
                            (savedSchedule.getProvider() != null ? savedSchedule.getProvider().getId() : "null"));

                        // Verify the save was successful
                        entityManager.refresh(savedSchedule);
                        System.out.println("✅ Verified saved schedule. Provider ID after refresh: " + 
                            (savedSchedule.getProvider() != null ? savedSchedule.getProvider().getId() : "null"));

                        // Notify the accepting provider
                        messagingTemplate.convertAndSend(
                            "/topic/service/schedule/confirmation/" + providerId,
                            Map.of(
                                "status", "CONFIRMED",
                                "scheduleId", savedSchedule.getId(),
                                "userId", savedSchedule.getUser().getId()
                            )
                        );

                        // Notify the user about acceptance
                        messagingTemplate.convertAndSend(
                            "/topic/user/" + savedSchedule.getUser().getId() + "/schedules",
                            Map.of(
                                "message", "Your schedule has been accepted by " + provider.getProvider_name(),
                                "scheduleId", savedSchedule.getId(),
                                "status", "ACCEPTED",
                                "action", "ACCEPTED",
                                "providerId", provider.getId(),
                                "providerName", provider.getProvider_name(),
                                "date", savedSchedule.getScheduledDateTime().toLocalDate().toString(),
                                "time", savedSchedule.getScheduledDateTime().toLocalTime().toString()
                            )
                        );

                        // Notify other providers that the schedule is no longer available
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("action", "TAKEN");
                        notification.put("scheduleId", savedSchedule.getId());
                        notification.put("providerId", providerId);
                        
                        // Send to both the service topic and individual provider topics
                        messagingTemplate.convertAndSend(
                            "/topic/service/" + savedSchedule.getService().getId() + "/schedules", 
                            notification
                        );
                        
                        // Also notify each provider individually
                        List<ServiceProvider> otherProviders = providerService.findByServiceId(savedSchedule.getService().getId());
                        for (ServiceProvider otherProvider : otherProviders) {
                            if (!otherProvider.getId().equals(providerId)) {
                                messagingTemplate.convertAndSend(
                                    "/topic/service/provider/" + otherProvider.getId() + "/schedules",
                                    notification
                                );
                            }
                        }
                        break;

                    case "REJECT":
                        // Only update if this provider hasn't already rejected
                        if (schedule.getProvider() == null) {
                            schedule.setStatus("REJECTED");
                            scheduleRepository.save(schedule);

                            // Notify the user about rejection
                            messagingTemplate.convertAndSend(
                                "/topic/user/" + schedule.getUser().getId() + "/schedules",
                                Map.of(
                                    "action", "REJECTED",
                                    "scheduleId", schedule.getId(),
                                    "providerId", providerId
                                )
                            );
                        }
                        break;
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("❌ Error handling schedule response: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 