package com.ZAP_Backend.ZapServices.Model;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String comment;

    // Image 1
    private String review_imageName1;
    private String review_imageType1;

    @Lob
    private byte[] review_imageData1;

    // Image 2
    private String review_imageName2;
    private String review_imageType2;

    @Lob
    private byte[] review_imageData2;

    private Integer rating;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;


   

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReview_imageName1() {
        return review_imageName1;
    }

    public void setReview_imageName1(String review_imageName1) {
        this.review_imageName1 = review_imageName1;
    }

    public String getReview_imageType1() {
        return review_imageType1;
    }

    public void setReview_imageType1(String review_imageType1) {
        this.review_imageType1 = review_imageType1;
    }

    public byte[] getReview_imageData1() {
        return review_imageData1;
    }

    public void setReview_imageData1(byte[] review_imageData1) {
        this.review_imageData1 = review_imageData1;
    }

    public String getReview_imageName2() {
        return review_imageName2;
    }

    public void setReview_imageName2(String review_imageName2) {
        this.review_imageName2 = review_imageName2;
    }

    public String getReview_imageType2() {
        return review_imageType2;
    }

    public void setReview_imageType2(String review_imageType2) {
        this.review_imageType2 = review_imageType2;
    }

    public byte[] getReview_imageData2() {
        return review_imageData2;
    }

    public void setReview_imageData2(byte[] review_imageData2) {
        this.review_imageData2 = review_imageData2;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceProvider getProvider() {
        return provider;
    }

    public void setProvider(ServiceProvider provider) {
        this.provider = provider;
    }
}
