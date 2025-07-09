package com.ZAP_Backend.ZapServices.Controller;

import com.ZAP_Backend.ZapServices.Model.Document;
import com.ZAP_Backend.ZapServices.Model.Professional_Document;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Model.Servicee;
import com.ZAP_Backend.ZapServices.Model.BankAccount;
import com.ZAP_Backend.ZapServices.Repository.ProfessionalRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import com.ZAP_Backend.ZapServices.Repository.ServiceRepository;
import com.ZAP_Backend.ZapServices.Repository.BankAccountRepository;
import com.ZAP_Backend.ZapServices.Service.DocumentsService;
import com.ZAP_Backend.ZapServices.Service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class providerRegistrationController {
    @Autowired
    DocumentsService documentsService;
    @Autowired
    ProviderService providerService;
    @Autowired
    ProviderRepository providerRepository;
    @Autowired
    ProfessionalRepository professionalRepository;
    @Autowired
    ServiceRepository serviceRepository;
    @Autowired
    BankAccountRepository bankAccountRepository;


    @PostMapping("/provider/register")
    public ResponseEntity<ServiceProvider> CreateProvider(@RequestParam String phone_no){
        ServiceProvider pro =new ServiceProvider();
        pro.setPhone_no(phone_no);
        pro.setVerified(false);
        ServiceProvider saved=providerService.create_provider(pro);
        return  new ResponseEntity<>(saved,HttpStatus.CREATED);
    }
      @PostMapping(value = "/provider/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
      public ResponseEntity<?> uploadDocuments(@PathVariable Long id, @RequestParam("aadhaar_no") Long aadhaarNo, @RequestParam("pan_no") Long panNo, @RequestPart("aadhaar_imageName") MultipartFile aadhaarImage, @RequestPart("pan_imageName") MultipartFile panImage, @RequestPart("provider_imageName") MultipartFile providerImage) {
       try {
           ServiceProvider provider = providerService.FindById(id);
           Document doc = documentsService.uploadDocuments(aadhaarNo, panNo, aadhaarImage, panImage, providerImage);
           provider.setDocument(doc);
           providerService.create_provider(provider);
           return new ResponseEntity<>(provider, HttpStatus.CREATED);
       } catch (IllegalArgumentException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
       } catch (Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
       }
    }
    @PostMapping(value = "/provider/{id}/professional-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfessionalDocuments(
            @PathVariable Long id,
            @RequestPart("legal_license") MultipartFile legal_license,
            @RequestParam int total_experience,
            @RequestParam int total_projects_completed,
            @RequestParam String domain_name,
            @RequestParam int long_duration,
            @RequestParam int short_duration,
            @RequestParam String service_level) {
        try {
            ServiceProvider provider = providerService.FindById(id);

            Professional_Document doc = new Professional_Document();
            doc.setLegal_License_imageName(legal_license.getOriginalFilename());
            doc.setLegal_License_imageType(legal_license.getContentType());
            doc.setLegal_License_imageData(legal_license.getBytes());
            doc.setTotal_experience(total_experience);
            doc.setTotal_projects_completed(total_projects_completed);
            doc.setDomain_name(domain_name);
            doc.setLong_duration(long_duration);
            doc.setShort_duration(short_duration);
            doc.setService_level(service_level);

            Professional_Document savedDoc = professionalRepository.save(doc);
            provider.setProfessional(savedDoc);
            providerService.create_provider(provider);

            return new ResponseEntity<>(provider, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping("/provider/{providerId}/service")
    public ResponseEntity<?> assignServiceToProvider(
            @PathVariable Long providerId,
            @RequestParam String service_name) {

        try {
            // Step 1: Find the provider
            ServiceProvider provider = providerService.FindById(providerId);
            if (provider == null) {
                return new ResponseEntity<>(
                        Map.of("error", "Provider not found with ID: " + providerId),
                        HttpStatus.NOT_FOUND
                );
            }

            // Step 2: Find service by name (existing)
            Servicee existingService = serviceRepository.findByServiceName(service_name);

            // Step 3: Create new service if not found
            if (existingService == null) {
                existingService = new Servicee();
                existingService.setServiceName(service_name);
                existingService = serviceRepository.save(existingService);
            }

            // Step 4: Check if the provider already has this service assigned
            if (provider.getService() != null &&
                    provider.getService().getId().equals(existingService.getId())) {
                return new ResponseEntity<>(
                        Map.of("message", "Provider already has this service assigned."),
                        HttpStatus.OK
                );
            }

            // Step 5: Assign service to provider
            provider.setService(existingService);
            ServiceProvider updatedProvider = providerRepository.save(provider);

            return new ResponseEntity<>(
                    Map.of(
                            "message", "Service assigned successfully",
                            "providerId", providerId,
                            "serviceId", existingService.getId(),
                            "provider", updatedProvider
                    ),
                    HttpStatus.OK
            );

        } catch (Exception e) {
            return new ResponseEntity<>(
                    Map.of("error", "Failed to assign service: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }


    //    @PostMapping("/provider/{id}/service")
//    public ResponseEntity<?> assignServiceToProvider(@PathVariable Long id, @RequestParam String service_name ) {
//        try {
//            ServiceProvider provider = providerService.FindById(id);
//
//            Servicee service = new Servicee();
//            service.setService_name(service_name);
//
//            Servicee savedService = serviceRepository.save(service);
//            provider.setService(savedService);
//            providerService.create_provider(provider);
//
//            return new ResponseEntity<>(provider, HttpStatus.OK);
//        } catch (Exception e) {
//            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//        }
//    }
    @PutMapping("/provider/{id}/finalize")
    public ResponseEntity<?> finalizeProvider(
            @PathVariable Long id,
            @RequestParam String address,
            @RequestParam String secondary_phone_no
            ,@RequestParam String providername
    ) {
        try {
            ServiceProvider provider=providerService.FindById(id);

            provider.setAddress(address);
            provider.setSecondary_phone_no(secondary_phone_no);
            provider.setProvider_name(providername);

            return new ResponseEntity<>(providerService.create_provider(provider), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/provider/{id}")
    public ResponseEntity<?> getProviderWithDetails(@PathVariable Long id) {
        ServiceProvider provider=providerService.FindById(id);
        if (provider == null) {
            return new ResponseEntity<>("Provider not found", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(provider, HttpStatus.OK);
    }
    @PostMapping(value = "/provider/{id}/bank-details", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBankDetails(
            @PathVariable Long id,
            @RequestParam String accountNumber,
            @RequestParam String ifscCode,
            @RequestParam String accountHolderName,
            @RequestParam String bankName,
            @RequestPart("passbook_image") MultipartFile passbook_image) {
        try {
            ServiceProvider provider = providerService.FindById(id);

            BankAccount bankAccount = new BankAccount();
            bankAccount.setAccountNumber(accountNumber);
            bankAccount.setIfscCode(ifscCode);
            bankAccount.setAccountHolderName(accountHolderName);
            bankAccount.setBankName(bankName);
            
            // Handle passbook image
            bankAccount.setPassbook_imageName(passbook_image.getOriginalFilename());
            bankAccount.setPassbook_imageType(passbook_image.getContentType());
            bankAccount.setPassbook_imageData(passbook_image.getBytes());

            BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
            provider.setBankAccount(savedBankAccount);
            providerService.create_provider(provider);

            return new ResponseEntity<>(provider, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
