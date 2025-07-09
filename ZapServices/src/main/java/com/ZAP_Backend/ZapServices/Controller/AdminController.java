package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.Model.*;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import com.ZAP_Backend.ZapServices.Repository.ServiceRepository;
import com.ZAP_Backend.ZapServices.Repository.ZapAmountRepository;
import com.ZAP_Backend.ZapServices.Repository.CompletedRepository;
import com.ZAP_Backend.ZapServices.Repository.BookedsRepository;
import com.ZAP_Backend.ZapServices.Repository.ScheduleRepository;
import com.ZAP_Backend.ZapServices.Repository.IssueRepository;
import com.ZAP_Backend.ZapServices.Service.ProviderService;
import com.ZAP_Backend.ZapServices.Service.ServiceService;
import com.ZAP_Backend.ZapServices.Service.UserService;
import com.ZAP_Backend.ZapServices.DataTransferObject.ZapAmountRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/Admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    UserService userService;
    @Autowired
    ProviderService providerService;
    @Autowired
    ServiceRepository serviceRepository;
    @Autowired
    ServiceService serviceService;
    @Autowired
    ProviderRepository providerRepository;
    @Autowired
    ZapAmountRepository zapAmountRepository;
    @Autowired
    CompletedRepository completedRepository;
    @Autowired
    BookedsRepository bookedsRepository;
    @Autowired
    ScheduleRepository scheduleRepository;
    @Autowired
    IssueRepository issueRepository;

    @GetMapping("/completed/count")
    public ResponseEntity<?> getCompletedServicesCount() {
        try {
            // Get total count of completed services
            long totalCompleted = completedRepository.count();

            // Get count of completed services with problems
            long problemServices = completedRepository.countByProblemOccursTrue();

            // Get count of completed services without problems
            long successfulServices = completedRepository.countByProblemOccursFalse();

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "totalCompletedServices", totalCompleted,
                "problemServices", problemServices,
                "successfulServices", successfulServices
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching completed services count: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

//    OK
    @GetMapping("/provider/{id}")
    public ResponseEntity<?> getProviderWithDetails(@PathVariable Long id) {
        ServiceProvider provider=providerService.FindById(id);
        if (provider == null) {
            return new ResponseEntity<>("Provider not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }

//    OK
    @GetMapping("/user/{id}")
    public  ResponseEntity<?> getUserWithDetailsById(@PathVariable Long id) {
        User currentuser=userService.get_user_With_details_id(id);
        if (currentuser == null) {
            return new ResponseEntity<>("Provider not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(currentuser, HttpStatus.OK);
    }

//    OK
    @GetMapping("/user/user-all")
    public  ResponseEntity<List<User>>  GetAllUsers(){

        return  new ResponseEntity<>(userService.GetAllUsers(),HttpStatus.CREATED);
    }

//    OK
    @GetMapping("/provider/provider-all")
    public  ResponseEntity<List<ServiceProvider>>  GetAllServiceProvider(){
        return  new ResponseEntity<>(providerService.get_all_service_provider(),HttpStatus.CREATED);
    }

//    ---------------------------------------------------------------------------------------------------------------
//    OK
    @PostMapping(value = "/service/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadServiceImage(@PathVariable Long id, @RequestPart("service_imageName") MultipartFile imageFile) {
        try {
            // Fetch existing service by ID
            Servicee service = serviceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + id));

        // Update image fields
        service.setService_imageName(imageFile.getOriginalFilename());
        service.setService_imageType(imageFile.getContentType());
        service.setService_imageData(imageFile.getBytes());

        // Save updated service
        Servicee updatedService = serviceRepository.save(service);

        return new ResponseEntity<>(updatedService, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    }
//    OK
    @GetMapping("/service-all")
    public  ResponseEntity<List<Servicee>>  GetAll_services(){
        return  new ResponseEntity<>(serviceRepository.findAll(),HttpStatus.CREATED);
    }


    @PostMapping(value = "/service/{id}/category", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addCategoryToService(
            @PathVariable Long id,
            @RequestParam String category_name,
            @RequestPart("category_image") MultipartFile categoryImage) {
        try {
            // Fetch existing service
            Servicee service = serviceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + id));

            // Create new Category
            Category category = new Category();
            category.setCategory_name(category_name);
            category.setCategory_imageName(categoryImage.getOriginalFilename());
            category.setCategory_imageType(categoryImage.getContentType());
            category.setCategory_imageData(categoryImage.getBytes());

            // Add category to service
            service.getCategories().add(category);

            // Save updated service
            serviceRepository.save(service);

            return new ResponseEntity<>(service, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/service/{id}")
    public ResponseEntity<?> getServiceWithCategories(@PathVariable Long id) {
        try {
            Servicee service = serviceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + id));

            // Return the full service object which includes categories
            return new ResponseEntity<>(service, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/provider/{id}/verified")
    public ResponseEntity<?> verifiedProvider(@PathVariable Long id,@RequestParam  Boolean verify) {
        try {
            ServiceProvider provider  = providerRepository.findById(id).orElseThrow();

            provider.setVerified(verify);

            return new ResponseEntity<>(providerRepository.save(provider), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/add-amount/service/{serviceId}")
    public ResponseEntity<?> addServiceAmount(@PathVariable Long serviceId, @RequestBody ZapAmountRequest request) {
        try {
            // Find the service first
            Servicee service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found with id: " + serviceId));

            // Create new ZapAmount
            ZapAmount zapAmount = new ZapAmount(
                request.getAmount(),
                serviceId,
                service.getServiceName()
            );

            // Save the amount
            ZapAmount savedAmount = zapAmountRepository.save(zapAmount);

            return new ResponseEntity<>(savedAmount, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Error adding amount: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/bookings/count")
    public ResponseEntity<?> getBookingsCount() {
        try {
            // Get total count of bookings
            long totalBookings = bookedsRepository.count();

            // Get count of bookings by status
            long arrivingBookings = bookedsRepository.countByStatus("ARRIVING");
            long startedBookings = bookedsRepository.countByStatus("START");
            long completedBookings = bookedsRepository.countByStatus("COMPLETED");
            long cancelledBookings = bookedsRepository.countByCancel(true);

            // Get count by service type
            long scheduleTypeBookings = bookedsRepository.countByServiceType("SCHEDULE");
            long issueTypeBookings = bookedsRepository.countByServiceType("ISSUE");

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "totalBookings", totalBookings,
                "byStatus", Map.of(
                    "arriving", arrivingBookings,
                    "started", startedBookings,
                    "completed", completedBookings,
                    "cancelled", cancelledBookings
                ),
                "byType", Map.of(
                    "schedule", scheduleTypeBookings,
                    "issue", issueTypeBookings
                )
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching bookings count: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/schedules/count")
    public ResponseEntity<?> getSchedulesCount() {
        try {
            // Get total count of schedules
            long totalSchedules = scheduleRepository.count();

            // Get count of schedules by status
            long pendingSchedules = scheduleRepository.countByStatus("PENDING");
            long acceptedSchedules = scheduleRepository.countByStatus("ACCEPTED");
            long rejectedSchedules = scheduleRepository.countByStatus("REJECTED");

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "totalSchedules", totalSchedules,
                "byStatus", Map.of(
                    "pending", pendingSchedules,
                    "accepted", acceptedSchedules,
                    "rejected", rejectedSchedules
                )
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching schedules count: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/issues/count")
    public ResponseEntity<?> getIssuesCount() {
        try {
            // Get total count of issues
            long totalIssues = issueRepository.count();

            // Get count of issues by status
            long submittedIssues = issueRepository.countByStatus("SUBMITED");
            long acceptedIssues = issueRepository.countByStatus("ACCEPTED");
            long scheduledIssues = issueRepository.countByStatus("SCHEDULED");
            long reattachmentIssues = issueRepository.countByReattachment(true);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "totalIssues", totalIssues,
                "byStatus", Map.of(
                    "submitted", submittedIssues,
                    "accepted", acceptedIssues,
                    "scheduled", scheduledIssues,
                    "reattachment", reattachmentIssues
                )
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching issues count: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/services/count")
    public ResponseEntity<?> getServicesCount() {
        try {
            // Get total count of services
            long totalServices = serviceRepository.count();

            // Get count of active and inactive services
//            long activeServices = serviceRepository.countByStatus(true);
//            long inactiveServices = serviceRepository.countByStatus(false);

            return new ResponseEntity<>(Map.of(
                "status", "SUCCESS",
                "totalServices", totalServices,
                "byStatus", Map.of(
//                    "active", activeServices,
//                    "inactive", inactiveServices
                )
            ), HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(Map.of(
                "status", "ERROR",
                "message", "Error fetching services count: " + e.getMessage()
            ), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
