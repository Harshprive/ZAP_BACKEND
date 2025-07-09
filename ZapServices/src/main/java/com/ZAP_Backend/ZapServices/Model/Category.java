package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    private String category_name;
    private String category_imageName;
    private String category_imageType;
    @Lob
    private byte[] category_imageData;

    public Category(Long id, String category_name, String category_imageName, String category_imageType, byte[] category_imageData) {
        this.id = id;
        this.category_name = category_name;
        this.category_imageName = category_imageName;
        this.category_imageType = category_imageType;
        this.category_imageData = category_imageData;
    }

    public Category() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory_name() {
        return category_name;
    }

    public void setCategory_name(String category_name) {
        this.category_name = category_name;
    }

    public String getCategory_imageName() {
        return category_imageName;
    }

    public void setCategory_imageName(String category_imageName) {
        this.category_imageName = category_imageName;
    }

    public String getCategory_imageType() {
        return category_imageType;
    }

    public void setCategory_imageType(String category_imageType) {
        this.category_imageType = category_imageType;
    }

    public byte[] getCategory_imageData() {
        return category_imageData;
    }

    public void setCategory_imageData(byte[] category_imageData) {
        this.category_imageData = category_imageData;
    }
}