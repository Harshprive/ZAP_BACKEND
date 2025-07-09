package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.DataTransferObject.FindServiceRequest;
import com.ZAP_Backend.ZapServices.DataTransferObject.FindServiceResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.GeocodingResponse;
import com.ZAP_Backend.ZapServices.DataTransferObject.ServiceResponse;
import com.ZAP_Backend.ZapServices.Model.*;
import com.ZAP_Backend.ZapServices.Repository.BookingRepository;
import com.ZAP_Backend.ZapServices.Repository.IssueRepository;
import com.ZAP_Backend.ZapServices.Repository.ServiceRepository;
import com.ZAP_Backend.ZapServices.Repository.ScheduleRepository;
import com.ZAP_Backend.ZapServices.Repository.ReviewRepository;
import com.ZAP_Backend.ZapServices.Service.ServiceService;
import com.ZAP_Backend.ZapServices.Service.UserService;
import com.ZAP_Backend.ZapServices.Service.GeocodingService;
import com.ZAP_Backend.ZapServices.Service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
public class UserController {
    @Autowired
    private GeocodingService geocodingService;
    @Autowired
    UserService userService;
    @Autowired
    ServiceRepository serviceRepository;
    @Autowired
    IssueRepository issueRepository;
    @Autowired
    ServiceService serviceService;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private IssueWebSocketController issueWebSocketController;
    @Autowired
    private ScheduleWebSocketController scheduleWebSocketController;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private ReviewRepository reviewRepository;


//    OK
    @PostMapping("/register")
    public ResponseEntity<Boolean> Register_user(@RequestBody User user){
        Boolean created=false;
        if(user != null){
            userService.register_user(user);
            created=true;
            return new ResponseEntity<Boolean>(created,HttpStatus.CREATED);
        }
        return  new ResponseEntity<>(created,HttpStatus.BAD_REQUEST);
    }

//    OK
    @GetMapping("/service-all")
    public  ResponseEntity<List<Servicee>>  GetAll_services(){
        return  new ResponseEntity<>(serviceRepository.findAll(),HttpStatus.CREATED);
    }

//    OK
    @GetMapping("/service/{id}")
    public ResponseEntity<?> getServiceWithCategories(@PathVariable Long id) {
        try {
            Servicee service = serviceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + id));

            List<Category> categories = service.getCategories();

            if (categories == null || categories.isEmpty()) {
                return new ResponseEntity<>("No categories found for this service", HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(categories, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
//  OK
    @PostMapping(value = "/issue/user/{userId}/service/{serviceId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadIssue(
            @PathVariable Long userId,
            @PathVariable Long serviceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("week") int week,
            @RequestParam("mediaCategory") String category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "address", required = false) String address
    ) {
        try {
            // Validate user and service
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            Servicee service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceId));

            // Validate file
            if (file.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "File cannot be empty"
                ), HttpStatus.BAD_REQUEST);
            }

            // Create and populate the issue
            Issue media = new Issue();
            media.setMediaName(file.getOriginalFilename());
            media.setMediaType(file.getContentType());
            media.setMediaData(file.getBytes());
            media.setWeekNumber(week);
            media.setMediaCategory(category);
            media.setDescription(description);
            media.setUser(user);
            media.setService(service);
            media.setAddress(address);
            
            // Set initial status and reattachment flag
            media.setStatus("SUBMITED");  // Initial status is always SUBMITED
            media.setReattachment(false); // Initial reattachment is always false
            media.setUploadedAt(LocalDateTime.now());

            // Save the issue
            Issue saved = issueRepository.save(media);

            // Broadcast the new issue to all providers of this service
            issueWebSocketController.broadcastNewIssue(saved);

            // Return success response with issue details
            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Issue submitted successfully",
                "issue", Map.of(
                    "id", saved.getId(),
                    "status", saved.getStatus(),
                    "reattachment", saved.getReattachment(),
                    "mediaName", saved.getMediaName(),
                    "mediaCategory", saved.getMediaCategory(),
                    "description", saved.getDescription(),
                    "weekNumber", saved.getWeekNumber(),
                    "uploadedAt", saved.getUploadedAt().toString()
                )
            ), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Failed to submit issue: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
// OK
    @GetMapping("/api/user/{userId}/service/{serviceId}/category/{categoryId}/find-service")
    public FindServiceResponse FindService(
            @PathVariable Long userId,
            @PathVariable Long serviceId,
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long excludeProviderId,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLon) {
             
        // Get user's coordinates from address if provided but no direct coordinates
        if (address != null && (userLat == null || userLon == null)) {
            GeocodingResponse coordinates = geocodingService.geocodeAddress(address);
            if (coordinates != null && coordinates.getLatitude() != null && coordinates.getLongitude() != null) {
                try {
                    userLat = Double.parseDouble(coordinates.getLatitude());
                    userLon = Double.parseDouble(coordinates.getLongitude());
                } catch (NumberFormatException e) {
                    System.out.println("❌ Error parsing coordinates from geocoding response");
                }
            }
        }
        
        // Find an available provider based on the criteria
        FindServiceResponse response;
        if (excludeProviderId != null) {
            // If excludeProviderId is provided, find a new provider excluding the specified one
            response = serviceService.findNewProvider(userId, serviceId, categoryId, excludeProviderId, userLat, userLon);
        } else {
            // Find any available provider for the service and category
            response = serviceService.findService(userId, serviceId, categoryId, userLat, userLon);
        }

        // If a provider was found, send their details via WebSocket
        if (response != null) {
            try {
                // Extract provider and category details from the response
                ServiceProvider provider = response.getServiceProvider();
                Category category = response.getCategory();
                
                // Log the provider details being sent
                System.out.println("📤 Sending provider details via WebSocket:");
                System.out.println("   └─ Provider ID: " + provider.getId());
                System.out.println("   └─ Provider Name: " + provider.getProvider_name());
                System.out.println("   └─ Service: " + provider.getService().getServiceName());
                System.out.println("   └─ Category: " + category.getCategory_name());

                // Prepare the WebSocket response with provider details
                Map<String, Object> wsResponse = new HashMap<>();
                wsResponse.put("serviceProvider", provider);
                wsResponse.put("serviceId", serviceId);
                wsResponse.put("categoryId", categoryId);
                wsResponse.put("serviceName", provider.getService().getServiceName());
                wsResponse.put("categoryName", category.getCategory_name());

                // Send the provider details to the user's specific WebSocket topic
                messagingTemplate.convertAndSend(
                    "/topic/user/" + userId + "/provider-details", 
                    wsResponse
                );
            } catch (Exception e) {
                // Log any errors that occur while sending provider details
                System.out.println("❌ Error sending provider details: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // If no provider was found, log the situation
            System.out.println("⚠️ No provider found for service " + serviceId + " and category " + categoryId);
            
            // Notify the user via WebSocket that no provider is available
            messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/provider-details",
                Map.of(
                    "error", "No provider available at the moment",
                    "serviceId", serviceId,
                    "categoryId", categoryId
                )
            );
        }

        // Return the full response object (which may be null if no provider was found)
        return response;
    }

    @PostMapping("/api/user/{userId}/service/{serviceId}/category/{categoryId}/provider/{providerId}/book")
    public ServiceResponse bookService(
            @PathVariable Long userId,
            @PathVariable Long serviceId,
            @PathVariable Long categoryId,
            @PathVariable Long providerId,
            @RequestBody Map<String, String> request) {
        
        String address = request.get("address");
        return serviceService.serviceResponse(userId, serviceId, categoryId, providerId, address);
    }

    @GetMapping("/user/{userId}/bookings")
    public ResponseEntity<?> getUserBookings(@PathVariable Long userId) {
        try {
            // First verify if user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all bookings for the user
            List<Booking> bookings = bookingRepository.findByUserId(userId);
            
            if (bookings.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "NO_BOOKINGS",
                    "message", "No bookings found for this user"
                ), HttpStatus.OK);
            }

            // Transform bookings to summary response format
            List<Map<String, Object>> bookingsList = bookings.stream()
                .map(booking -> {
                    Map<String, Object> bookingMap = new HashMap<>();
                    bookingMap.put("id", booking.getId());
                    bookingMap.put("serviceName", booking.getService().getServiceName());
                    bookingMap.put("categoryName", booking.getCategory().getCategory_name());
                    bookingMap.put("providerName", booking.getServiceProvider().getProvider_name());
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

    @GetMapping("/user/{userId}/booking/{bookingId}")
    public ResponseEntity<?> getDetailedBookingById(
            @PathVariable Long userId,
            @PathVariable Long bookingId) {
        try {
            // First verify if user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get the specific booking
            Booking booking = bookingRepository.findById(bookingId)
                .orElse(null);

            // Check if booking exists and belongs to the user
            if (booking == null || !booking.getUser().getId().equals(userId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Booking not found or does not belong to this user"
                ), HttpStatus.NOT_FOUND);
            }

            // Build detailed response with all booking information
            Map<String, Object> bookingDetails = new HashMap<>();
            bookingDetails.put("id", booking.getId());
            bookingDetails.put("status", booking.getStatus());
            bookingDetails.put("bookingDate", booking.getBookingDate().toString());
            bookingDetails.put("scheduledDate", booking.getScheduledDate().toString());
            bookingDetails.put("createdAt", booking.getCreatedAt().toString());
            bookingDetails.put("bookedAddress", booking.getBookedAddress());

            // Add service details
            Map<String, Object> serviceInfo = new HashMap<>();
            Servicee service = booking.getService();
            serviceInfo.put("id", service.getId());
            serviceInfo.put("name", service.getServiceName());
            if (service.getService_imageData() != null) {
                serviceInfo.put("imageType", service.getService_imageType());
                serviceInfo.put("imageData", service.getService_imageData());
            }
            bookingDetails.put("service", serviceInfo);

            // Add category details
            Map<String, Object> categoryInfo = new HashMap<>();
            Category category = booking.getCategory();
            categoryInfo.put("id", category.getId());
            categoryInfo.put("name", category.getCategory_name());
            if (category.getCategory_imageData() != null) {
                categoryInfo.put("imageType", category.getCategory_imageType());
                categoryInfo.put("imageData", category.getCategory_imageData());
            }
            bookingDetails.put("category", categoryInfo);

            // Add provider details
            Map<String, Object> providerInfo = new HashMap<>();
            ServiceProvider provider = booking.getServiceProvider();
            providerInfo.put("id", provider.getId());
            providerInfo.put("name", provider.getProvider_name());
            providerInfo.put("phone", provider.getPhone_no());
            providerInfo.put("address", provider.getAddress());
            providerInfo.put("verified", provider.isVerified());
            bookingDetails.put("provider", providerInfo);

            // Add user details
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("phone", user.getPhone_no());
            userInfo.put("address", user.getAddress());
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

    // Get issue media
    @GetMapping("/issue/{issueId}/media")
    public ResponseEntity<?> getIssueMedia(@PathVariable Long issueId) {
        try {
            Issue issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(issue.getMediaType()))
                    .body(issue.getMediaData());
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving issue media: " + e.getMessage(), 
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get issue details
    @GetMapping("/user/{userId}/issue/{issueId}")
    public ResponseEntity<?> getIssueDetails(@PathVariable Long userId, @PathVariable Long issueId) {
        try {
            // Verify user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get issue and verify it belongs to the user
            Issue issue = issueRepository.findById(issueId)
                    .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));

            if (!issue.getUser().getId().equals(userId)) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Issue does not belong to this user"
                ), HttpStatus.FORBIDDEN);
            }

            // Build response with all issue details
            Map<String, Object> response = new HashMap<>();
            response.put("issueId", issue.getId());
            response.put("weekNumber", issue.getWeekNumber());
            response.put("mediaName", issue.getMediaName());
            response.put("mediaType", issue.getMediaType());
            response.put("mediaCategory", issue.getMediaCategory());
            response.put("description", issue.getDescription());
            response.put("status", issue.getStatus());
            response.put("reattachment", issue.getReattachment());
            response.put("uploadedAt", issue.getUploadedAt().toString());
            
            // Add service details
            if (issue.getService() != null) {
                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("serviceId", issue.getService().getId());
                serviceInfo.put("serviceName", issue.getService().getServiceName());
                response.put("service", serviceInfo);
            }

            // Add provider details if assigned
            if (issue.getServiceProvider() != null) {
                Map<String, Object> providerInfo = new HashMap<>();
                ServiceProvider provider = issue.getServiceProvider();
                providerInfo.put("providerId", provider.getId());
                providerInfo.put("name", provider.getProvider_name());
                providerInfo.put("phone", provider.getPhone_no());
                providerInfo.put("address", provider.getAddress());
                response.put("provider", providerInfo);
            } else {
                response.put("provider", null);
            }

            // Add user details
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("userId", user.getId());
            userInfo.put("name", user.getName());
            userInfo.put("phone", user.getPhone_no());
            userInfo.put("address", user.getAddress());
            response.put("user", userInfo);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "data", response
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error retrieving issue details: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get all issues for a user
    @GetMapping("/user/{userId}/issue-all")
    public ResponseEntity<?> getAllUserIssues(@PathVariable Long userId) {
        try {
            // Verify user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all issues for the user
            List<Issue> issues = issueRepository.findByUser(user);
            
            // Transform issues to response format
            List<Map<String, Object>> issuesList = issues.stream().map(issue -> {
                Map<String, Object> issueMap = new HashMap<>();
                issueMap.put("issueId", issue.getId());
                issueMap.put("weekNumber", issue.getWeekNumber());
                issueMap.put("mediaName", issue.getMediaName());
                issueMap.put("mediaType", issue.getMediaType());
                issueMap.put("mediaCategory", issue.getMediaCategory());
                issueMap.put("description", issue.getDescription());
                issueMap.put("status", issue.getStatus());
                issueMap.put("reattachment", issue.getReattachment());
                issueMap.put("uploadedAt", issue.getUploadedAt().toString());

                // Add service details
                if (issue.getService() != null) {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("serviceId", issue.getService().getId());
                    serviceInfo.put("serviceName", issue.getService().getServiceName());
                    issueMap.put("service", serviceInfo);
                }

                // Add provider details if assigned
                if (issue.getServiceProvider() != null) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    ServiceProvider provider = issue.getServiceProvider();
                    providerInfo.put("providerId", provider.getId());
                    providerInfo.put("name", provider.getProvider_name());
                    providerInfo.put("phone", provider.getPhone_no());
                    providerInfo.put("address", provider.getAddress());
                    issueMap.put("provider", providerInfo);
                } else {
                    issueMap.put("provider", null);
                }

                return issueMap;
            }).collect(Collectors.toList());

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalIssues", issues.size());
            summary.put("pendingIssues", issues.stream().filter(i -> i.getStatus().equals("SUBMITED")).count());
            summary.put("acceptedIssues", issues.stream().filter(i -> i.getStatus().equals("ACCEPTED")).count());
            summary.put("reattachmentRequested", issues.stream().filter(i -> i.getReattachment()).count());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "summary", summary,
                "data", issuesList
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error retrieving issues: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/user/{userId}/service/{serviceId}/category/{categoryId}/schedule")
    public ResponseEntity<?> scheduleService(
            @PathVariable Long userId,
            @PathVariable Long serviceId,
            @PathVariable Long categoryId,
            @RequestBody Map<String, String> scheduleRequest) {
        try {
            // Validate input
            String date = scheduleRequest.get("date");
            String time = scheduleRequest.get("time");
            String ampm = scheduleRequest.get("ampm");
            String address = scheduleRequest.get("address");

            if (date == null || time == null || ampm == null || address == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Missing required fields: date, time, ampm, address"
                ), HttpStatus.BAD_REQUEST);
            }

            // Parse date and time
            String timeStr = time + " " + ampm.toUpperCase();
            LocalDateTime scheduledDateTime;
            try {
                // Assuming date format is yyyy-MM-dd and time format is hh:mm
                scheduledDateTime = LocalDateTime.parse(date + " " + timeStr, 
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a"));
            } catch (Exception e) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Invalid date/time format. Use yyyy-MM-dd for date and hh:mm for time"
                ), HttpStatus.BAD_REQUEST);
            }

            // Get required entities
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found"
                ), HttpStatus.NOT_FOUND);
            }

            Servicee service = serviceRepository.findById(serviceId).orElse(null);
            if (service == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Service not found"
                ), HttpStatus.NOT_FOUND);
            }

            Category category = service.getCategories().stream()
                .filter(c -> c.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
            if (category == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Category not found"
                ), HttpStatus.NOT_FOUND);
            }

            // Create the schedule
            Schedule schedule = new Schedule();
            schedule.setUser(user);
            schedule.setService(service);
            schedule.setCategory(category);
            schedule.setStatus("PENDING");
            schedule.setScheduledDateTime(scheduledDateTime);
            schedule.setCreatedAt(LocalDateTime.now());
            schedule.setAddress(address);

            // Save the schedule
            Schedule savedSchedule = scheduleRepository.save(schedule);

            // Broadcast the schedule request to all providers with the same service ID
            scheduleWebSocketController.broadcastNewSchedule(savedSchedule);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Schedule request sent to providers",
                "scheduleId", savedSchedule.getId(),
                "scheduledDateTime", scheduledDateTime.toString()
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error scheduling service: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}/schedules")
    public ResponseEntity<?> getUserSchedules(@PathVariable Long userId) {
        try {
            // First verify if user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all schedules for the user
            List<Schedule> schedules = scheduleRepository.findByUser(user);
            
            if (schedules.isEmpty()) {
                return new ResponseEntity<>(Map.of(
                    "status", "NO_SCHEDULES",
                    "message", "No schedules found for this user"
                ), HttpStatus.OK);
            }

            // Transform schedules to response format
            List<Map<String, Object>> schedulesList = schedules.stream()
                .map(schedule -> {
                    Map<String, Object> scheduleMap = new HashMap<>();
                    scheduleMap.put("id", schedule.getId());
                    scheduleMap.put("serviceName", schedule.getService().getServiceName());
                    scheduleMap.put("categoryName", schedule.getCategory().getCategory_name());
                    scheduleMap.put("status", schedule.getStatus());
                    scheduleMap.put("scheduledDateTime", schedule.getScheduledDateTime().toString());
                    scheduleMap.put("address", schedule.getAddress());
                    scheduleMap.put("createdAt", schedule.getCreatedAt().toString());
                    
                    // Add provider info if assigned
                    if (schedule.getProvider() != null) {
                        scheduleMap.put("providerName", schedule.getProvider().getProvider_name());
                        scheduleMap.put("providerPhone", schedule.getProvider().getPhone_no());
                    }
                    
                    return scheduleMap;
                })
                .collect(Collectors.toList());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "schedules", schedulesList
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching schedules: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}/all-requests")
    public ResponseEntity<?> getAllUserRequests(@PathVariable Long userId) {
        try {
            // First verify if user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Get all types of requests
            List<Booking> bookings = bookingRepository.findByUserId(userId);
            List<Schedule> schedules = scheduleRepository.findByUser(user);
            List<Issue> issues = issueRepository.findByUser(user);

            // Transform bookings
            List<Map<String, Object>> bookingsList = bookings.stream()
                .map(booking -> {
                    Map<String, Object> bookingMap = new HashMap<>();
                    bookingMap.put("type", "BOOKING");
                    bookingMap.put("id", booking.getId());
                    bookingMap.put("serviceName", booking.getService().getServiceName());
                    bookingMap.put("categoryName", booking.getCategory().getCategory_name());
                    bookingMap.put("status", booking.getStatus());
                    bookingMap.put("createdAt", booking.getCreatedAt().toString());
                    if (booking.getServiceProvider() != null) {
                        bookingMap.put("providerName", booking.getServiceProvider().getProvider_name());
                    }
                    return bookingMap;
                })
                .collect(Collectors.toList());

            // Transform schedules
            List<Map<String, Object>> schedulesList = schedules.stream()
                .map(schedule -> {
                    Map<String, Object> scheduleMap = new HashMap<>();
                    scheduleMap.put("type", "SCHEDULE");
                    scheduleMap.put("id", schedule.getId());
                    scheduleMap.put("serviceName", schedule.getService().getServiceName());
                    scheduleMap.put("categoryName", schedule.getCategory().getCategory_name());
                    scheduleMap.put("status", schedule.getStatus());
                    scheduleMap.put("scheduledDateTime", schedule.getScheduledDateTime().toString());
                    scheduleMap.put("createdAt", schedule.getCreatedAt().toString());
                    if (schedule.getProvider() != null) {
                        scheduleMap.put("providerName", schedule.getProvider().getProvider_name());
                    }
                    return scheduleMap;
                })
                .collect(Collectors.toList());

            // Transform issues
            List<Map<String, Object>> issuesList = issues.stream()
                .map(issue -> {
                    Map<String, Object> issueMap = new HashMap<>();
                    issueMap.put("type", "ISSUE");
                    issueMap.put("id", issue.getId());
                    issueMap.put("serviceName", issue.getService().getServiceName());
                    issueMap.put("status", issue.getStatus());
                    issueMap.put("mediaCategory", issue.getMediaCategory());
                    issueMap.put("weekNumber", issue.getWeekNumber());
                    issueMap.put("uploadedAt", issue.getUploadedAt().toString());
                    if (issue.getServiceProvider() != null) {
                        issueMap.put("providerName", issue.getServiceProvider().getProvider_name());
                    }
                    return issueMap;
                })
                .collect(Collectors.toList());

            // Combine all requests and sort by creation date
            List<Map<String, Object>> allRequests = new ArrayList<>();
            allRequests.addAll(bookingsList);
            allRequests.addAll(schedulesList);
            allRequests.addAll(issuesList);

            // Add summary statistics
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalRequests", allRequests.size());
            summary.put("totalBookings", bookingsList.size());
            summary.put("totalSchedules", schedulesList.size());
            summary.put("totalIssues", issuesList.size());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "summary", summary,
                "requests", allRequests
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching requests: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}/all-requests/{requestId}")
    public ResponseEntity<?> getRequestDetails(@PathVariable Long userId, @PathVariable Long requestId) {
        try {
            // First verify if user exists
            User user = userService.get_user_With_details_id(userId);
            if (user == null) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "User not found with ID: " + userId
                ), HttpStatus.NOT_FOUND);
            }

            // Try to find the request in each type of repository
            Booking booking = bookingRepository.findById(requestId).orElse(null);
            Schedule schedule = scheduleRepository.findById(requestId).orElse(null);
            Issue issue = issueRepository.findById(requestId).orElse(null);

            // Check if request exists and belongs to the user
            if (booking != null && booking.getUser().getId().equals(userId)) {
                // Return detailed booking information
                Map<String, Object> bookingDetails = new HashMap<>();
                bookingDetails.put("type", "BOOKING");
                bookingDetails.put("id", booking.getId());
                bookingDetails.put("status", booking.getStatus());
                bookingDetails.put("createdAt", booking.getCreatedAt().toString());
                bookingDetails.put("bookedAddress", booking.getBookedAddress());
                if (booking.getBookingDate() != null) {
                    bookingDetails.put("bookingDate", booking.getBookingDate().toString());
                }
                if (booking.getScheduledDate() != null) {
                    bookingDetails.put("scheduledDate", booking.getScheduledDate().toString());
                }

                // Service info
                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("id", booking.getService().getId());
                serviceInfo.put("name", booking.getService().getServiceName());
                bookingDetails.put("service", serviceInfo);

                // Category info
                Map<String, Object> categoryInfo = new HashMap<>();
                categoryInfo.put("id", booking.getCategory().getId());
                categoryInfo.put("name", booking.getCategory().getCategory_name());
                bookingDetails.put("category", categoryInfo);

                // Provider info if assigned
                if (booking.getServiceProvider() != null) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    providerInfo.put("id", booking.getServiceProvider().getId());
                    providerInfo.put("name", booking.getServiceProvider().getProvider_name());
                    providerInfo.put("phone", booking.getServiceProvider().getPhone_no());
                    bookingDetails.put("provider", providerInfo);
                }

                return new ResponseEntity<>(Map.of(
                    "status", "SUCCESS",
                    "data", bookingDetails
                ), HttpStatus.OK);

            } else if (schedule != null && schedule.getUser().getId().equals(userId)) {
                // Return detailed schedule information
                Map<String, Object> scheduleDetails = new HashMap<>();
                scheduleDetails.put("type", "SCHEDULE");
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

                // Provider info if assigned
                if (schedule.getProvider() != null) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    providerInfo.put("id", schedule.getProvider().getId());
                    providerInfo.put("name", schedule.getProvider().getProvider_name());
                    providerInfo.put("phone", schedule.getProvider().getPhone_no());
                    scheduleDetails.put("provider", providerInfo);
                }

                return new ResponseEntity<>(Map.of(
                    "status", "SUCCESS",
                    "data", scheduleDetails
                ), HttpStatus.OK);

            } else if (issue != null && issue.getUser().getId().equals(userId)) {
                // Return detailed issue information
                Map<String, Object> issueDetails = new HashMap<>();
                issueDetails.put("type", "ISSUE");
                issueDetails.put("id", issue.getId());
                issueDetails.put("status", issue.getStatus());
                issueDetails.put("uploadedAt", issue.getUploadedAt().toString());
                issueDetails.put("mediaName", issue.getMediaName());
                issueDetails.put("mediaCategory", issue.getMediaCategory());
                issueDetails.put("weekNumber", issue.getWeekNumber());
                issueDetails.put("description", issue.getDescription());
                issueDetails.put("reattachment", issue.getReattachment());

                // Service info
                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("id", issue.getService().getId());
                serviceInfo.put("name", issue.getService().getServiceName());
                issueDetails.put("service", serviceInfo);

                // Provider info if assigned
                if (issue.getServiceProvider() != null) {
                    Map<String, Object> providerInfo = new HashMap<>();
                    providerInfo.put("id", issue.getServiceProvider().getId());
                    providerInfo.put("name", issue.getServiceProvider().getProvider_name());
                    providerInfo.put("phone", issue.getServiceProvider().getPhone_no());
                    issueDetails.put("provider", providerInfo);
                }

                return new ResponseEntity<>(Map.of(
                    "status", "SUCCESS",
                    "data", issueDetails
                ), HttpStatus.OK);
            }

            // If we get here, either the request doesn't exist or doesn't belong to the user
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Request not found or does not belong to this user"
            ), HttpStatus.NOT_FOUND);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching request details: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/user/{userId}/provider/{providerId}/rating", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> rateProvider(
            @PathVariable Long userId,
            @PathVariable Long providerId,
            @RequestParam("comment") String comment,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "image1", required = false) MultipartFile image1,
            @RequestParam(value = "image2", required = false) MultipartFile image2) {
        try {
            // Validate rating
            if (rating < 1 || rating > 5) {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Rating must be between 1 and 5"
                ), HttpStatus.BAD_REQUEST);
            }

            // Add the review
            Review review = userService.addProviderReview(userId, providerId, comment, rating, image1, image2);

            // Return success response
            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "message", "Review submitted successfully",
                "review", Map.of(
                    "id", review.getId(),
                    "comment", review.getComment(),
                    "rating", review.getRating(),
                    "userId", review.getUser().getId(),
                    "providerId", review.getProvider().getId()
                )
            ), HttpStatus.CREATED);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Failed to submit review: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userId}/provider/{providerId}/ratings")
    public ResponseEntity<?> getProviderRatings(
            @PathVariable Long userId,
            @PathVariable Long providerId) {
        try {
            // Verify user exists
            userService.get_user_With_details_id(userId);
            
            // Get all reviews
            List<Review> reviews = userService.getProviderRatings(providerId);
            
            // Transform reviews into response format
            List<Map<String, Object>> reviewResponses = reviews.stream()
                .map(review -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", review.getId());
                    response.put("userName", review.getUser().getName());
                    response.put("rating", review.getRating());
                    response.put("comment", review.getComment());
                    
                    // Add image1 info if exists
                    if (review.getReview_imageName1() != null) {
                        response.put("image1", Map.of(
                            "name", review.getReview_imageName1(),
                            "type", review.getReview_imageType1()
                        ));
                    }
                    
                    // Add image2 info if exists
                    if (review.getReview_imageName2() != null) {
                        response.put("image2", Map.of(
                            "name", review.getReview_imageName2(),
                            "type", review.getReview_imageType2()
                        ));
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "reviews", reviewResponses
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/provider/{providerId}/reviews")
    public ResponseEntity<?> getPublicProviderReviews(@PathVariable Long providerId) {
        try {
            // Get all reviews
            List<Review> reviews = userService.getProviderRatings(providerId);
            
            // Transform reviews into response format
            List<Map<String, Object>> reviewResponses = reviews.stream()
                .map(review -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", review.getId());
                    response.put("userName", review.getUser().getName());
                    response.put("rating", review.getRating());
                    response.put("comment", review.getComment());
                    
                    // Add image1 info if exists
                    if (review.getReview_imageName1() != null) {
                        response.put("image1", Map.of(
                            "name", review.getReview_imageName1(),
                            "type", review.getReview_imageType1(),
                            "imageUrl", "/provider/" + providerId + "/review/" + review.getId() + "/image/1"
                        ));
                    }
                    
                    // Add image2 info if exists
                    if (review.getReview_imageName2() != null) {
                        response.put("image2", Map.of(
                            "name", review.getReview_imageName2(),
                            "type", review.getReview_imageType2(),
                            "imageUrl", "/provider/" + providerId + "/review/" + review.getId() + "/image/2"
                        ));
                    }
                    
                    return response;
                })
                .collect(Collectors.toList());

            // Calculate average rating
            double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

            // Count total reviews
            int totalReviews = reviews.size();

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "providerId", providerId,
                "totalReviews", totalReviews,
                "averageRating", Math.round(averageRating * 10.0) / 10.0,  // Round to 1 decimal place
                "reviews", reviewResponses
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Add endpoint to get review images
    @GetMapping("/provider/{providerId}/review/{reviewId}/image/{imageNumber}")
    public ResponseEntity<?> getReviewImage(
            @PathVariable Long providerId,
            @PathVariable Long reviewId,
            @PathVariable int imageNumber) {
        try {
            Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

            // Verify this review belongs to the specified provider
            if (!review.getProvider().getId().equals(providerId)) {
                throw new RuntimeException("Review does not belong to the specified provider");
            }

            byte[] imageData;
            String imageType;
            
            if (imageNumber == 1 && review.getReview_imageData1() != null) {
                imageData = review.getReview_imageData1();
                imageType = review.getReview_imageType1();
            } else if (imageNumber == 2 && review.getReview_imageData2() != null) {
                imageData = review.getReview_imageData2();
                imageType = review.getReview_imageType2();
            } else {
                return new ResponseEntity<>(Map.of(
                    "status", "ERROR",
                    "message", "Image not found"
                ), HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageType))
                .body(imageData);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
