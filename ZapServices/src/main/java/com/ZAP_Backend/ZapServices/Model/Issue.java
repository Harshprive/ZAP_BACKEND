package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
//@Table(name = "issue")
@Table(name = " issues matching ")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int weekNumber; // User-defined week number (e.g., 1, 2, etc.)

    private String mediaName;
    private String mediaType; // e.g., "image/png", "audio/mpeg", "video/mp4"

    @Lob
    @Column(length = 10000000) // Optional: set a limit for large files
    private byte[] mediaData;  // Can be  image, video, or audio

    // Optional metadata
    private String mediaCategory; // e.g., "image", "audio", "video"

    private String description;  // optional user input

    private LocalDateTime uploadedAt = LocalDateTime.now();// Optional: "image", "video", "audio" for filtering

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serviceprovider_id")
    private ServiceProvider serviceProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Servicee service;

    @Column(nullable = false)
    private String status = "SUBMITED";

    @Column(nullable = false)
    private Boolean reattachment = false;

    
    private String address;
    
    @Column()
    private LocalDateTime scheduledDateTime;

    public Issue() {
        this.uploadedAt = LocalDateTime.now();
        this.status = "SUBMITED";
        this.reattachment = false;
    }

    public Issue(Long id, int weekNumber, String mediaName, String mediaType, byte[] mediaData, String mediaCategory, String description, LocalDateTime uploadedAt, User user, ServiceProvider serviceProvider, Servicee service, String status, Boolean reattachment, String address ,LocalDateTime scheduledDateTime) {
        this.id = id;
        this.weekNumber = weekNumber;
        this.mediaName = mediaName;
        this.mediaType = mediaType;
        this.mediaData = mediaData;
        this.mediaCategory = mediaCategory;
        this.description = description;
        this.uploadedAt = uploadedAt;
        this.user = user;
        this.serviceProvider = serviceProvider;
        this.service = service;
        this.status = status;
        this.reattachment = reattachment;
        this.address = address;
        this.scheduledDateTime = scheduledDateTime;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }
    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public byte[] getMediaData() {
        return mediaData;
    }

    public void setMediaData(byte[] mediaData) {
        this.mediaData = mediaData;
    }

    public String getMediaCategory() {
        return mediaCategory;
    }

    public void setMediaCategory(String mediaCategory) {
        this.mediaCategory = mediaCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ServiceProvider getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public Servicee getService() {
        return service;
    }

    public void setService(Servicee service) {
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getReattachment() {
        return reattachment;
    }

    public void setReattachment(Boolean reattachment) {
        this.reattachment = reattachment;
    }
}
