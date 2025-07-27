package com.ZAP_Backend.ZapServices.Service;

import com.ZAP_Backend.ZapServices.Model.User;
import com.ZAP_Backend.ZapServices.Model.Review;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import com.ZAP_Backend.ZapServices.Repository.UserRepository;
import com.ZAP_Backend.ZapServices.Repository.ReviewRepository;
import com.ZAP_Backend.ZapServices.Repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    ReviewRepository reviewRepository;
    
    @Autowired
    ProviderRepository providerRepository;

    public void register_user(User user){
        userRepository.save(user);
    }
    
    public User get_user_With_details_id(Long id){
        return userRepository.findById(id).orElseThrow();
    }
    
    public List<User> GetAllUsers(){
        return userRepository.findAll();
    }

    public List<Review> getProviderRatings(Long providerId) {
        // Verify provider exists
        providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with ID: " + providerId));
        
        // Get all reviews for the provider
        return reviewRepository.findByProviderId(providerId);
    }

    public Review addProviderReview(Long userId, Long providerId, String comment, Integer rating, 
                                  MultipartFile image1, MultipartFile image2) throws IOException {
        // Get user and provider
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with ID: " + providerId));

        // Create new review
        Review review = new Review();
        review.setUser(user);
        review.setProvider(provider);
        review.setComment(comment);
        review.setRating(rating);

        // Handle image 1 if provided
        if (image1 != null && !image1.isEmpty()) {
            review.setReview_imageName1(image1.getOriginalFilename());
            review.setReview_imageType1(image1.getContentType());
            review.setReview_imageData1(image1.getBytes());
        }

        // Handle image 2 if provided
        if (image2 != null && !image2.isEmpty()) {
            review.setReview_imageName2(image2.getOriginalFilename());
            review.setReview_imageType2(image2.getContentType());
            review.setReview_imageData2(image2.getBytes());
        }

        // Save and return the review
        return reviewRepository.save(review);
    }
}
