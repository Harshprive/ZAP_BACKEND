package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.DataTransferObject.GeocodingResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.ProviderResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.ServiceProviderResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.ScheduleDateRequest;
import com.ZAP_Backend.ZapServices.Model.*;
import com.ZAP_Backend.ZapServices.Repository.BookingRepository;
import com.ZAP_Backend.ZapServices.Repository.IssueRepository;
import com.ZAP_Backend.ZapServices.Repository.ScheduleRepository;
import com.ZAP_Backend.ZapServices.Repository.BookedsRepository;
import com.ZAP_Backend.ZapServices.Repository.ZapAmountRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import com.ZAP_Backend.ZapServices.Repository.CompletedRepository;
import com.ZAP_Backend.ZapServices.Service.GeocodingService;
import com.ZAP_Backend.ZapServices.Service.ProviderService;
import com.ZAP_Backend.ZapServices.Service.ServiceService;
import com.ZAP_Backend.ZapServices.Service.WebSocketConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/provider")
@CrossOrigin(origins = "*")
public class ServiceProviderController {
//    @Autowired
//    ServiceService serviceService;

    private final ServiceService serviceService;
    private final ProviderService providerService;
    private final BookingRepository bookingRepository;
    private final IssueRepository issueRepository;
    private final ScheduleRepository scheduleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final BookedsRepository bookedsRepository;
    private final ZapAmountRepository zapAmountRepository;
    private final ProviderRepository providerRepository;
    private final WebSocketConnectionService webSocketConnectionService;
    private final CompletedRepository completedRepository;
    private final GeocodingService geocodingService;

    @Autowired
    public ServiceProviderController(ServiceService serviceService, ProviderService providerService, BookingRepository bookingRepository, IssueRepository issueRepository, ScheduleRepository scheduleRepository, SimpMessagingTemplate messagingTemplate, BookedsRepository bookedsRepository, ZapAmountRepository zapAmountRepository, ProviderRepository providerRepository, WebSocketConnectionService webSocketConnectionService, CompletedRepository completedRepository, GeocodingService geocodingService) {
        this.serviceService = serviceService;
        this.providerService = providerService;
        this.bookingRepository = bookingRepository;
        this.issueRepository = issueRepository;
        this.scheduleRepository = scheduleRepository;
        this.messagingTemplate = messagingTemplate;
        this.bookedsRepository = bookedsRepository;
        this.zapAmountRepository = zapAmountRepository;
        this.providerRepository = providerRepository;
        this.webSocketConnectionService = webSocketConnectionService;
        this.completedRepository = completedRepository;
        this.geocodingService = geocodingService;
    }
    

    @MessageMapping("/service/response")
    public void receiveProviderResponse(ProviderResponse resp) {
        System.out.println("📨 Received response from provider " + resp.getProviderId() +
                ": " + (resp.isAccepted() ? "ACCEPTED" : "REJECTED"));
        serviceService.receiveProviderResponse(resp);
    }

    @MessageMapping("/service/schedule/response")
    public void handleScheduleResponse(Map<String, Object> response) {
        Long providerId = ((Number) response.get("providerId")).longValue();
        Long scheduleId = ((Number) response.get("scheduleId")).longValue();
        boolean accepted = (boolean) response.get("accepted");

        System.out.println("📨 Received schedule response from provider " + providerId +
                ": " + (accepted ? "ACCEPTED" : "REJECTED"));

        // Get the schedule
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

        if (accepted) {
            // Update schedule with provider info and status
            schedule.setProvider(providerService.FindById(providerId));
            schedule.setStatus("ACCEPTED");
            scheduleRepository.save(schedule);

            // Notify the provider about confirmation
            try {
                messagingTemplate.convertAndSend(
                    "/topic/service/schedule/confirmation/" + providerId,
                    Map.of(
                        "status", "CONFIRMED",
                        "scheduleId", schedule.getId(),
                        "userId", schedule.getUser().getId()
                    )
                );

                // Notify other providers that the request is no longer available
                List<ServiceProvider> providers = providerService.findByServiceId(schedule.getService().getId());
                for (ServiceProvider provider : providers) {
                    if (!provider.getId().equals(providerId)) {
                        messagingTemplate.convertAndSend(
                            "/topic/service/schedule/" + provider.getId(),
                            Map.of(
                                "status", "TAKEN",
                                "scheduleId", schedule.getId()
                            )
                        );
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not send confirmation messages: " + e.getMessage());
            }
        } else {
            // Update schedule status to rejected by this provider
            schedule.setStatus("REJECTED");
            scheduleRepository.save(schedule);
        }
    }

    @GetMapping("/{providerId}/bookings")
    public ResponseEntity<?> getProviderBookings(@PathVariable Long providerId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all bookings for the provider
            List<Booking> bookings = bookingRepository.findByProviderId(providerId);
            
            if (bookings.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "NO_BOOKINGS",
                    "message", "No bookings found for this provider"
                ), HttpStatus.OK);
            }

            // Transform bookings to summary response format
            List<Map<String, Object>> bookingsList = bookings.stream()
                .map(booking -> {
                    Map<String, Object> bookingMap = new HashMap<>();
                    bookingMap.put("id", booking.getId());
                    bookingMap.put("userName", booking.getUser().getName());
                    bookingMap.put("userAddress", booking.getBookedAddress());
                    bookingMap.put("categoryName", booking.getCategory().getCategory_name());
                    bookingMap.put("status", booking.getStatus());
                    bookingMap.put("createdAt", booking.getCreatedAt().toString());
                    return bookingMap;
                })
                .collect(Collectors.toList());

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalBookings", bookings.size());
            summary.put("pendingBookings", bookings.stream().filter(b -> b.getStatus().equals("PENDING")).count());
            summary.put("confirmedBookings", bookings.stream().filter(b -> b.getStatus().equals("CONFIRMED")).count());
            summary.put("completedBookings", bookings.stream().filter(b -> b.getStatus().equals("COMPLETED")).count());
            summary.put("cancelledBookings", bookings.stream().filter(b -> b.getStatus().equals("CANCELLED")).count());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "summary", summary,
                "bookings", bookingsList
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching bookings: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{providerId}/booking/{bookingId}")
    public ResponseEntity<?> getProviderBookingDetails(
            @PathVariable Long providerId,
            @PathVariable Long bookingId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the specific booking
            Booking booking = bookingRepository.findById(bookingId)
                .orElse(null);

            // Check if booking exists and belongs to the provider
            if (booking == null || !booking.getServiceProvider().getId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booking not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Build detailed response
            Map<String, Object> bookingDetails = new HashMap<>();
            
            // Basic booking info
            bookingDetails.put("id", booking.getId());
            bookingDetails.put("status", booking.getStatus());
            bookingDetails.put("createdAt", booking.getCreatedAt().toString());
            bookingDetails.put("bookingDate", booking.getBookingDate().toString());
            bookingDetails.put("scheduledDate", booking.getScheduledDate().toString());
            bookingDetails.put("bookedAddress", booking.getBookedAddress());

            // Service and category info
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("id", booking.getService().getId());
            serviceInfo.put("name", booking.getService().getServiceName());
            bookingDetails.put("service", serviceInfo);

            Map<String, Object> categoryInfo = new HashMap<>();
            categoryInfo.put("id", booking.getCategory().getId());
            categoryInfo.put("name", booking.getCategory().getCategory_name());
            bookingDetails.put("category", categoryInfo);

            // User info
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", booking.getUser().getId());
            userInfo.put("name", booking.getUser().getName());
            userInfo.put("phone", booking.getUser().getPhone_no());
            userInfo.put("email", booking.getUser().getEmail());
            userInfo.put("address", booking.getUser().getAddress());
            bookingDetails.put("user", userInfo);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "data", bookingDetails
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching booking details: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{providerId}/issues")
    public ResponseEntity<?> getProviderIssues(@PathVariable Long providerId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all issues for the provider
            List<Issue> issues = issueRepository.findByServiceProvider(provider);
            
            if (issues.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "NO_ISSUES",
                    "message", "No issues found for this provider"
                ), HttpStatus.OK);
            }

            // Transform issues to response format
            List<Map<String, Object>> issuesList = issues.stream()
                .map(issue -> {
                    Map<String, Object> issueMap = new HashMap<>();
                    issueMap.put("id", issue.getId());
                    issueMap.put("weekNumber", issue.getWeekNumber());
                    issueMap.put("mediaName", issue.getMediaName());
                    issueMap.put("mediaType", issue.getMediaType());
                    issueMap.put("mediaCategory", issue.getMediaCategory());
                    issueMap.put("description", issue.getDescription());
                    issueMap.put("status", issue.getStatus());
                    issueMap.put("reattachment", issue.getReattachment());
                    issueMap.put("uploadedAt", issue.getUploadedAt().toString());

                    // Add user details
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", issue.getUser().getId());
                    userInfo.put("name", issue.getUser().getName());
                    userInfo.put("phone", issue.getUser().getPhone_no());
                    userInfo.put("address", issue.getUser().getAddress());
                    issueMap.put("user", userInfo);

                    // Add service details
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("id", issue.getService().getId());
                    serviceInfo.put("name", issue.getService().getServiceName());
                    issueMap.put("service", serviceInfo);

                    return issueMap;
                })
                .collect(Collectors.toList());

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalIssues", issues.size());
            summary.put("submittedIssues", issues.stream().filter(i -> i.getStatus().equals("SUBMITED")).count());
            summary.put("acceptedIssues", issues.stream().filter(i -> i.getStatus().equals("ACCEPTED")).count());
            summary.put("reattachmentRequested", issues.stream().filter(Issue::getReattachment).count());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "summary", summary,
                "data", issuesList
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching issues: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{providerId}/issue/{issueId}")
    public ResponseEntity<?> getProviderIssueDetails(
            @PathVariable Long providerId,
            @PathVariable Long issueId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the specific issue
            Issue issue = issueRepository.findById(issueId)
                .orElse(null);

            // Check if issue exists and belongs to the provider
            if (issue == null || 
                (issue.getServiceProvider() != null && !issue.getServiceProvider().getId().equals(providerId))) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Issue not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Build detailed response
            Map<String, Object> issueDetails = new HashMap<>();
            
            // Basic issue info
            issueDetails.put("id", issue.getId());
            issueDetails.put("weekNumber", issue.getWeekNumber());
            issueDetails.put("mediaName", issue.getMediaName());
            issueDetails.put("mediaType", issue.getMediaType());
            issueDetails.put("mediaCategory", issue.getMediaCategory());
            issueDetails.put("description", issue.getDescription());
            issueDetails.put("status", issue.getStatus());
            issueDetails.put("reattachment", issue.getReattachment());
            issueDetails.put("uploadedAt", issue.getUploadedAt().toString());

            // Add media data
            if (issue.getMediaData() != null) {
                issueDetails.put("mediaData", Base64.getEncoder().encodeToString(issue.getMediaData()));
            }

            // Add user details
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", issue.getUser().getId());
            userInfo.put("name", issue.getUser().getName());
            userInfo.put("phone", issue.getUser().getPhone_no());
            userInfo.put("email", issue.getUser().getEmail());
            userInfo.put("address", issue.getUser().getAddress());
            issueDetails.put("user", userInfo);

            // Add service details
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("id", issue.getService().getId());
            serviceInfo.put("name", issue.getService().getServiceName());
            if (issue.getService().getService_imageData() != null) {
                serviceInfo.put("imageType", issue.getService().getService_imageType());
                serviceInfo.put("imageData", Base64.getEncoder().encodeToString(issue.getService().getService_imageData()));
            }
            issueDetails.put("service", serviceInfo);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "data", issueDetails
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching issue details: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{providerId}/schedules")
    public ResponseEntity<?> getProviderSchedules(@PathVariable Long providerId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all accepted schedules for the provider
            List<Schedule> schedules = scheduleRepository.findByProviderAndStatus(provider, "ACCEPTED");
            
            if (schedules.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "NO_SCHEDULES",
                    "message", "No accepted schedules found for this provider"
                ), HttpStatus.OK);
            }

            // Transform schedules to response format
            List<Map<String, Object>> schedulesList = schedules.stream()
                .map(schedule -> {
                    Map<String, Object> scheduleMap = new HashMap<>();
                    scheduleMap.put("id", schedule.getId());
                    scheduleMap.put("userName", schedule.getUser().getName());
                    scheduleMap.put("userAddress", schedule.getAddress());
                    scheduleMap.put("serviceName", schedule.getService().getServiceName());
                    scheduleMap.put("categoryName", schedule.getCategory().getCategory_name());
                    scheduleMap.put("status", schedule.getStatus());
                    scheduleMap.put("scheduledDateTime", schedule.getScheduledDateTime().toString());
                    scheduleMap.put("createdAt", schedule.getCreatedAt().toString());
                    return scheduleMap;
                })
                .collect(Collectors.toList());

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalSchedules", schedules.size());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "summary", summary,
                "schedules", schedulesList
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching schedules: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{providerId}/schedule/{scheduleId}")
    public ResponseEntity<?> getProviderScheduleDetails(
            @PathVariable Long providerId,
            @PathVariable Long scheduleId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the specific schedule
            Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElse(null);

            // Check if schedule exists and belongs to the provider
            if (schedule == null || !schedule.getProvider().getId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Schedule not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Build detailed response
            Map<String, Object> scheduleDetails = new HashMap<>();
            
            // Basic schedule info
            scheduleDetails.put("id", schedule.getId());
            scheduleDetails.put("status", schedule.getStatus());
            scheduleDetails.put("createdAt", schedule.getCreatedAt().toString());
            scheduleDetails.put("scheduledDateTime", schedule.getScheduledDateTime().toString());
            scheduleDetails.put("address", schedule.getAddress());

            // Service info
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("id", schedule.getService().getId());
            serviceInfo.put("name", schedule.getService().getServiceName());
            scheduleDetails.put("service", serviceInfo);

            // Category info
            Map<String, Object> categoryInfo = new HashMap<>();
            categoryInfo.put("id", schedule.getCategory().getId());
            categoryInfo.put("name", schedule.getCategory().getCategory_name());
            scheduleDetails.put("category", categoryInfo);

            // User info
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", schedule.getUser().getId());
            userInfo.put("name", schedule.getUser().getName());
            userInfo.put("phone", schedule.getUser().getPhone_no());
            userInfo.put("email", schedule.getUser().getEmail());
            scheduleDetails.put("user", userInfo);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "data", scheduleDetails
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching schedule details: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateOTP() {
        // Generate a random 6-digit OTP
        return String.format("%04d", (int)(Math.random() * 10000));
    }

    @PostMapping("/{providerId}/schedule/{scheduleId}/bookings")
    public ResponseEntity<?> createScheduleBooking(
            @PathVariable Long providerId,
            @PathVariable Long scheduleId,
            @RequestParam String status) {
        try {
            // Verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the schedule
            Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found with ID: " + scheduleId));

            // Create new Bookeds entry
            Bookeds booked = new Bookeds();
            booked.setProviderId(providerId);
            booked.setUserId(schedule.getUser().getId());
            booked.setServiceType("SCHEDULE");
            booked.setServiceId(schedule.getService().getId());
            booked.setCategoryId(schedule.getCategory().getId());
            booked.setAddress(schedule.getAddress());
            GeocodingResponse georesp = geocodingService.geocodeAddress(schedule.getAddress());
            booked.setLatitude(Double.parseDouble(georesp.getLatitude()));
            booked.setLongitude(Double.parseDouble(georesp.getLongitude()));
            // booked.setLatitude(null);
            // booked.setLongitude(null);
            booked.setStatus("ARRIVING");
            booked.setCancel(false);
            booked.setOtp(generateOTP()); // Set the generated OTP

            // Get ZapAmount for the service
            ZapAmount zapAmount = zapAmountRepository.findById(schedule.getService().getId())
                .orElse(null);
            
            if (zapAmount != null) {
                booked.setFixedAmount(zapAmount.getAmount());
            }

            // Save the booking
            bookedsRepository.save(booked);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Booking created successfully",
                "bookingId", booked.getId(),
                "otp", booked.getOtp() // Include OTP in response
            ), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error creating booking: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{providerId}/issue/{issueId}/bookings")
    public ResponseEntity<?> createIssueBooking(
            @PathVariable Long providerId,
            @PathVariable Long issueId,
            @RequestParam String status) {
        try {
            // Verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the issue
            Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));

            // Create new Bookeds entry
            Bookeds booked = new Bookeds();
            booked.setProviderId(providerId);
            booked.setUserId(issue.getUser().getId());
            booked.setServiceType("ISSUE");
            booked.setServiceId(issue.getService().getId());
            booked.setCategoryId(0L); // Set empty value as 0L
            booked.setAddress(issue.getAddress());
            GeocodingResponse georesp = geocodingService.geocodeAddress(issue.getAddress());
            booked.setLatitude(Double.parseDouble(georesp.getLatitude()));
            booked.setLongitude(Double.parseDouble(georesp.getLongitude()));
            booked.setStatus("ARRIVING");
            booked.setCancel(false);
            booked.setOtp(generateOTP());

            // Get ZapAmount for the service
            ZapAmount zapAmount = zapAmountRepository.findById(issue.getService().getId())
                .orElse(null);
            
            if (zapAmount != null) {
                booked.setFixedAmount(zapAmount.getAmount());
            }

            // Save the booking
            bookedsRepository.save(booked);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Booking created successfully",
                "bookingId", booked.getId(),
                "otp", booked.getOtp() // Include OTP in response
            ), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error creating booking: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{providerId}/booked/{bookedId}/verification")
    public ResponseEntity<?> verifyBookedOTP(
            @PathVariable Long providerId,
            @PathVariable Long bookedId,
            @RequestParam String OTP) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the booked service
            Bookeds booked = bookedsRepository.findById(bookedId)
                .orElse(null);

            // Check if booking exists and belongs to the provider
            if (booked == null || !booked.getProviderId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booked service not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Verify OTP
            if (booked.getOtp() != null && booked.getOtp().equals(OTP)) {
                // Create service details map
                Map<String, Object> serviceDetails = new HashMap<>();
                
                // Add booked ID
                serviceDetails.put("bookedId", booked.getId());

                // Add user details
                User user = booked.getUser();
                if (user != null) {
                    serviceDetails.put("userName", user.getName());
                }

                // Add service details
                Servicee service = booked.getService();
                if (service != null) {
                    serviceDetails.put("serviceName", service.getServiceName());
                    
                    // Add categories if they exist
                    List<Category> categories = service.getCategories();
                    if (categories != null && !categories.isEmpty()) {
                        List<Map<String, Object>> categoryList = categories.stream()
                            .map(category -> {
                                Map<String, Object> categoryMap = new HashMap<>();
                                categoryMap.put("id", category.getId());
                                categoryMap.put("name", category.getCategory_name());
                                return categoryMap;
                            })
                            .collect(Collectors.toList());
                        serviceDetails.put("categories", categoryList);
                    }
                }

                // Add address and other booking details
                serviceDetails.put("address", booked.getAddress());
                serviceDetails.put("serviceType", booked.getServiceType());
                serviceDetails.put("totalAmount", booked.getTotalAmount());
                serviceDetails.put("status", booked.getStatus());
                serviceDetails.put("createdAt", booked.getCreatedAt());

                return new ResponseEntity<>(Map.of(
                    "status", "SUCCESS",
                    "message", "You got the right person!",
                    "verified", true,
                    "serviceDetails", serviceDetails
                ), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "You got the wrong person!",
                    "verified", false
                ), HttpStatus.OK);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error verifying OTP: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{providerId}/booked/{bookedId}/bookings")
    public ResponseEntity<?> updateBookedStatus(
            @PathVariable Long providerId,
            @PathVariable Long bookedId,
            @RequestParam String status) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the booked service
            Bookeds booked = bookedsRepository.findById(bookedId)
                .orElse(null);

            // Check if booking exists and belongs to the provider
            if (booked == null || !booked.getProviderId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booked service not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Update status based on the request
            if ("START".equals(status)) {    // new added_byme
                // Check if current status is ARRIVING
                if (!"ARRIVING".equals(booked.getStatus())) {
                    return new ResponseEntity<>(Map.of(
                        "status", "ERROR",
                        "message", "Cannot START service. Current status is not ARRIVING"
                    ), HttpStatus.BAD_REQUEST);
                }
                booked.setStatus("START");
                bookedsRepository.save(booked);

                return new ResponseEntity<>(Map.of(
                    "status", "SUCCESS",
                    "message", "Service status updated to ARRIVE",
                    "bookedId", booked.getId(),
                    "currentStatus", booked.getStatus()
                ), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Invalid status update request"
                ), HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error updating booked status: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{providerId}/booked/{bookedId}/estimated-cost")
    public ResponseEntity<?> updateEstimatedCost(
            @PathVariable Long providerId,
            @PathVariable Long bookedId,
            @RequestBody Map<String, Double> requestBody) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the booked service
            Bookeds booked = bookedsRepository.findById(bookedId)
                .orElse(null);

            // Check if booking exists and belongs to the provider
            if (booked == null || !booked.getProviderId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booked service not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Get the amount from request body
            Double additionalAmount = requestBody.get("amount");
            if (additionalAmount == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Amount is required in the request body"
                ), HttpStatus.BAD_REQUEST);
            }

            // Get current fixed amount or default to 0 if null
            Double currentFixedAmount = booked.getFixedAmount() != null ? booked.getFixedAmount() : 0.0;
            
            // Calculate new fixed amount
            Double newFixedAmount = currentFixedAmount + additionalAmount;
            booked.setProviderAmount(additionalAmount);
            
            // Calculate total amount (you might want to adjust this based on your business logic)
            Double totalAmount = newFixedAmount;
            booked.setTotalAmount(totalAmount);
            booked.setStatus("START");   // new added_byme
            
            // Save the updated booked service
            bookedsRepository.save(booked);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Estimated cost updated successfully",
                "bookedId", booked.getId(),
                "additionalAmount", additionalAmount,
                "fixedAmount", newFixedAmount,
                "totalAmount", totalAmount
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error updating estimated cost: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{providerId}/booked/{bookedId}/completed")
    public ResponseEntity<?> markBookingAsCompleted(
            @PathVariable Long providerId,
            @PathVariable Long bookedId) {
        try {
            // First verify if provider exists
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Provider not found with ID: " + providerId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the booked service
            Bookeds booked = bookedsRepository.findById(bookedId)
                .orElse(null);

            // Check if booking exists and belongs to the provider
            if (booked == null || !booked.getProviderId().equals(providerId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booked service not found or does not belong to this provider"
                ), HttpStatus.NOT_FOUND);
            }

            // Check if the booking is in a valid state to be completed
            if (!"START".equals(booked.getStatus())) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Cannot complete booking. Current status must be START"
                ), HttpStatus.BAD_REQUEST);
            }

            // Create a new Completed record
            Completed completed = new Completed();
            completed.setAddress(booked.getAddress());
            completed.setCompletedAt(LocalDateTime.now());
            completed.setServiceType(booked.getServiceType());
            completed.setStatus("COMPLETED");
            completed.setTotalAmount(booked.getTotalAmount());
            completed.setProblemOccurs(false); // Automatically set to false
            completed.setUser(booked.getUser());
            completed.setProvider(provider);
            completed.setService(booked.getService());
            // Get category from service since Bookeds doesn't have direct category reference
            if (booked.getService() != null && !booked.getService().getCategories().isEmpty()) {
                completed.setCategory(booked.getService().getCategories().get(0));
            }
            completed.setBookeds(booked);

            // Update the booked status
            booked.setStatus("COMPLETED");
            bookedsRepository.save(booked);

            // Save the completed record
            completedRepository.save(completed);

            // Notify the user about completion via WebSocket
            try {
                messagingTemplate.convertAndSend(
                    "/topic/user/" + booked.getUser().getId() + "/bookings",
                    Map.of(
                        "message", "Your service has been completed",
                        "bookedId", booked.getId(),
                        "status", "COMPLETED",
                        "action", "COMPLETED",
                        "providerId", provider.getId(),
                        "providerName", provider.getProvider_name(),
                        "completedAt", completed.getCompletedAt().toString()
                    )
                );
            } catch (Exception e) {
                System.out.println("⚠️ Warning: Could not send completion notification to user: " + e.getMessage());
            }

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Service marked as completed successfully",
                "bookedId", booked.getId(),
                "completedId", completed.getId(),
                "completedAt", completed.getCompletedAt().toString()
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error completing service: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{providerId}/issue/{issueId}/schedule-date")
    public ResponseEntity<?> scheduleIssueDate(
            @PathVariable Long providerId,
            @PathVariable Long issueId,
            @RequestBody ScheduleDateRequest request) {
        
        Optional<ServiceProvider> providerOpt = providerRepository.findById(providerId);
        Optional<Issue> issueOpt = issueRepository.findById(issueId);

        if (providerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (issueOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ServiceProvider provider = providerOpt.get();
        Issue issue = issueOpt.get();

        // Verify that this provider is assigned to this issue
        if (!issue.getServiceProvider().getId().equals(providerId)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "This provider is not assigned to this issue"));
        }

        // Update the scheduled date time
        issue.setScheduledDateTime(request.getScheduledDateTime());
        issue.setStatus("SCHEDULED");
        issueRepository.save(issue);

        // Notify the user about the scheduled date
        try {
            messagingTemplate.convertAndSend(
                "/topic/user/" + issue.getUser().getId() + "/issues",
                Map.of(
                    "message", "Your issue has been scheduled for " + request.getScheduledDateTime(),
                    "issueId", issue.getId(),
                    "status", "SCHEDULED",
                    "action", "SCHEDULED",
                    "providerId", provider.getId(),
                    "providerName", provider.getProvider_name(),
                    "scheduledDateTime", request.getScheduledDateTime().toString()
                )
            );
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not send schedule notification to user: " + e.getMessage());
        }

        return ResponseEntity.ok()
            .body(Map.of(
                "message", "Issue scheduled successfully",
                "scheduledDateTime", request.getScheduledDateTime()
            ));
    }

    @MessageMapping("/provider/register")
    public void handleProviderRegistration(@Payload Map<String, Object> registration) {
        try {
            Object providerIdObj = registration.get("providerId");
            if (providerIdObj != null) {
                Long providerId;
                if (providerIdObj instanceof String) {
                    providerId = Long.parseLong((String) providerIdObj);
                } else if (providerIdObj instanceof Number) {
                    providerId = ((Number) providerIdObj).longValue();
                } else {
                    System.out.println("❌ Invalid provider ID type: " + providerIdObj.getClass());
                    return;
                }
                
                webSocketConnectionService.addConnectedProvider(providerId);
                System.out.println("✅ Registered provider: " + providerId);
            }
        } catch (Exception e) {
            System.out.println("❌ Error registering provider: " + e.getMessage());
        }
    }
}
