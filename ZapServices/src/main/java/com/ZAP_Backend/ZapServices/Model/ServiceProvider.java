package com.ZAP_Backend.ZapServices.Model;

import jakarta.persistence.*;

import java.util.List;

@Entity
public class ServiceProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;
    private  String phone_no;

    @OneToOne
    @JoinColumn(name = "document_id")  // foreign key column
    private Document document;

    @OneToOne
    @JoinColumn(name = "bank_account_id")  // foreign key column
    private BankAccount bankAccount;

    @OneToOne
    @JoinColumn(name = "professional_id")  // foreign key column
    private Professional_Document professional;

    @ManyToOne
    @JoinColumn(name = "service_id")  // foreign key column
    private Servicee service;

    private  String address;
    private Double latitude;  // Added field for latitude
    private Double longitude;  // Added field for longitude

    private  String secondary_phone_no;
    @Column(name = "verified")
    private  boolean verified;
    private String provider_name;

    private String name;
    private String phone;

//    private  String Status;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    private List<Review> reviews;


    public ServiceProvider(Long id, String phone_no, Document document, BankAccount bankAccount, Professional_Document professional, Servicee service, String address, String secondary_phone_no, boolean verified, String provider_name, Double latitude, Double longitude) {
        this.id = id;
        this.phone_no = phone_no;
        this.document = document;
        this.bankAccount = bankAccount;
        this.professional = professional;
        this.service = service;
        this.address = address;
        this.secondary_phone_no = secondary_phone_no;
        this.verified = verified;
        this.provider_name = provider_name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public ServiceProvider() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhone_no() {
        return phone_no;
    }

    public void setPhone_no(String phone_no) {
        this.phone_no = phone_no;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public Professional_Document getProfessional() {
        return professional;
    }

    public void setProfessional(Professional_Document professional) {
        this.professional = professional;
    }

    public Servicee getService() {
        return service;
    }

    public void setService(Servicee service) {
        this.service = service;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSecondary_phone_no() {
        return secondary_phone_no;
    }

    public void setSecondary_phone_no(String secondary_phone_no) {
        this.secondary_phone_no = secondary_phone_no;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getProvider_name() {
        return provider_name;
    }

    public void setProvider_name(String provider_name) {
        this.provider_name = provider_name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}