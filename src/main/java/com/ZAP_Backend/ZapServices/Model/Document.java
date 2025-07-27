package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    private  Long aadhaar_no;
    private String aadhaar_imageName;
    private String aadhaar_imageType;
    @Lob
    private byte[] aadhaar_imageData;

    private Long pan_no;
    private String pan_imageName;
    private String pan_imageType;
    @Lob
    private byte[] pan_imageData;

    private String provider_imageName;
    private String provider_imageType;
    @Lob
    private byte[] provider_imageData;

    public Document(Long id, Long aadhaar_no, String aadhaar_imageName, String aadhaar_imageType, byte[] aadhaar_imageData, Long pan_no, String pan_imageName, String pan_imageType, byte[] pan_imageData, String provider_imageName, String provider_imageType, byte[] provider_imageData) {
        this.id = id;
        this.aadhaar_no = aadhaar_no;
        this.aadhaar_imageName = aadhaar_imageName;
        this.aadhaar_imageType = aadhaar_imageType;
        this.aadhaar_imageData = aadhaar_imageData;
        this.pan_no = pan_no;
        this.pan_imageName = pan_imageName;
        this.pan_imageType = pan_imageType;
        this.pan_imageData = pan_imageData;
        this.provider_imageName = provider_imageName;
        this.provider_imageType = provider_imageType;
        this.provider_imageData = provider_imageData;
    }

    public Document() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAadhaar_no() {
        return aadhaar_no;
    }

    public void setAadhaar_no(Long aadhaar_no) {
        this.aadhaar_no = aadhaar_no;
    }

    public String getAadhaar_imageName() {
        return aadhaar_imageName;
    }

    public void setAadhaar_imageName(String aadhaar_imageName) {
        this.aadhaar_imageName = aadhaar_imageName;
    }

    public String getAadhaar_imageType() {
        return aadhaar_imageType;
    }

    public void setAadhaar_imageType(String aadhaar_imageType) {
        this.aadhaar_imageType = aadhaar_imageType;
    }

    public byte[] getAadhaar_imageData() {
        return aadhaar_imageData;
    }

    public void setAadhaar_imageData(byte[] aadhaar_imageData) {
        this.aadhaar_imageData = aadhaar_imageData;
    }

    public Long getPan_no() {
        return pan_no;
    }

    public void setPan_no(Long pan_no) {
        this.pan_no = pan_no;
    }

    public String getPan_imageName() {
        return pan_imageName;
    }

    public void setPan_imageName(String pan_imageName) {
        this.pan_imageName = pan_imageName;
    }

    public String getPan_imageType() {
        return pan_imageType;
    }

    public void setPan_imageType(String pan_imageType) {
        this.pan_imageType = pan_imageType;
    }

    public byte[] getPan_imageData() {
        return pan_imageData;
    }

    public void setPan_imageData(byte[] pan_imageData) {
        this.pan_imageData = pan_imageData;
    }

    public String getProvider_imageName() {
        return provider_imageName;
    }

    public void setProvider_imageName(String provider_imageName) {
        this.provider_imageName = provider_imageName;
    }

    public String getProvider_imageType() {
        return provider_imageType;
    }

    public void setProvider_imageType(String provider_imageType) {
        this.provider_imageType = provider_imageType;
    }

    public byte[] getProvider_imageData() {
        return provider_imageData;
    }

    public void setProvider_imageData(byte[] provider_imageData) {
        this.provider_imageData = provider_imageData;
    }
}
