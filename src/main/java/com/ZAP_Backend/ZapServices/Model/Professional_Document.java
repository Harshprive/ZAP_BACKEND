package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

@Entity
public class Professional_Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    private String Legal_License_imageName;
    private String Legal_License_imageType;
    @Lob
    private byte[] Legal_License_imageData;

    private int total_experience;
    private  int total_projects_completed;

    private String domain_name;

    private  int long_duration;
    private  int short_duration;
    private String service_level;

    public Professional_Document(Long id, String legal_License_imageName, String legal_License_imageType, byte[] legal_License_imageData, int total_experience, int total_projects_completed, String domain_name, int long_duration, int short_duration, String service_level) {
        this.id = id;
        Legal_License_imageName = legal_License_imageName;
        Legal_License_imageType = legal_License_imageType;
        Legal_License_imageData = legal_License_imageData;
        this.total_experience = total_experience;
        this.total_projects_completed = total_projects_completed;
        this.domain_name = domain_name;
        this.long_duration = long_duration;
        this.short_duration = short_duration;
        this.service_level = service_level;
    }

    public Professional_Document() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLegal_License_imageName() {
        return Legal_License_imageName;
    }

    public void setLegal_License_imageName(String legal_License_imageName) {
        Legal_License_imageName = legal_License_imageName;
    }

    public String getLegal_License_imageType() {
        return Legal_License_imageType;
    }

    public void setLegal_License_imageType(String legal_License_imageType) {
        Legal_License_imageType = legal_License_imageType;
    }

    public byte[] getLegal_License_imageData() {
        return Legal_License_imageData;
    }

    public void setLegal_License_imageData(byte[] legal_License_imageData) {
        Legal_License_imageData = legal_License_imageData;
    }

    public int getTotal_experience() {
        return total_experience;
    }

    public void setTotal_experience(int total_experience) {
        this.total_experience = total_experience;
    }

    public int getTotal_projects_completed() {
        return total_projects_completed;
    }

    public void setTotal_projects_completed(int total_projects_completed) {
        this.total_projects_completed = total_projects_completed;
    }

    public String getDomain_name() {
        return domain_name;
    }

    public void setDomain_name(String domain_name) {
        this.domain_name = domain_name;
    }

    public int getLong_duration() {
        return long_duration;
    }

    public void setLong_duration(int long_duration) {
        this.long_duration = long_duration;
    }

    public int getShort_duration() {
        return short_duration;
    }

    public void setShort_duration(int short_duration) {
        this.short_duration = short_duration;
    }

    public String getService_level() {
        return service_level;
    }

    public void setService_level(String service_level) {
        this.service_level = service_level;
    }
}
