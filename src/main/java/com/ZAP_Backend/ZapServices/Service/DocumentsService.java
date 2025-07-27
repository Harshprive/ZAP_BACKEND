package com.ZAP_Backend.ZapServices.Service;

import com.ZAP_Backend.ZapServices.Model.Document;
import com.ZAP_Backend.ZapServices.Repository.DocumentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class DocumentsService {
    @Autowired
    DocumentsRepository documentsRepository;
    public Document uploadDocuments(Long aadhaarNo, Long panNo,
                                    MultipartFile aadhaarImage,
                                    MultipartFile panImage,
                                    MultipartFile providerImage) throws IOException {

        if (aadhaarImage.isEmpty() || panImage.isEmpty() || providerImage.isEmpty()) {
            throw new IllegalArgumentException("One or more image files are empty.");
        }

        Document doc = new Document();

        doc.setAadhaar_no(aadhaarNo);
        doc.setAadhaar_imageName(aadhaarImage.getOriginalFilename());
        doc.setAadhaar_imageType(aadhaarImage.getContentType());
        doc.setAadhaar_imageData(aadhaarImage.getBytes());

        doc.setPan_no(panNo);
        doc.setPan_imageName(panImage.getOriginalFilename());
        doc.setPan_imageType(panImage.getContentType());
        doc.setPan_imageData(panImage.getBytes());

        doc.setProvider_imageName(providerImage.getOriginalFilename());
        doc.setProvider_imageType(providerImage.getContentType());
        doc.setProvider_imageData(providerImage.getBytes());

        return documentsRepository.save(doc);
    }

}
