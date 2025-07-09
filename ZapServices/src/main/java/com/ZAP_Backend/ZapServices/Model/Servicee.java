package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Servicee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    
    @Column(name = "name", nullable = false)  // new
    private  String serviceName;

    private String service_imageName;
    private String service_imageType;
    @Lob
    private byte[] service_imageData;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "service_id")
    private List<Category> categories = new ArrayList<>();

    public Servicee(Long id, String serviceName, String service_imageName, String service_imageType, byte[] service_imageData, List<Category> categories) {
        this.id = id;
        this.serviceName = serviceName;
        this.service_imageName = service_imageName;
        this.service_imageType = service_imageType;
        this.service_imageData = service_imageData;
        this.categories = categories;
    }

    public Servicee() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getService_imageName() {
        return service_imageName;
    }

    public void setService_imageName(String service_imageName) {
        this.service_imageName = service_imageName;
    }

    public String getService_imageType() {
        return service_imageType;
    }

    public void setService_imageType(String service_imageType) {
        this.service_imageType = service_imageType;
    }

    public byte[] getService_imageData() {
        return service_imageData;
    }

    public void setService_imageData(byte[] service_imageData) {
        this.service_imageData = service_imageData;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }


//    private List<String> shops;
}
