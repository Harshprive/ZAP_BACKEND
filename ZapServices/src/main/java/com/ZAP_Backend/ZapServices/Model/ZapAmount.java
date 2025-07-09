package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "zap_amounts")
public class ZapAmount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    // Default constructor
    public ZapAmount() {
    }

    // Parameterized constructor
    public ZapAmount(Double amount, Long serviceId, String serviceName) {
        this.amount = amount;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
} 