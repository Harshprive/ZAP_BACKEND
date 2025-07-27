package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Bookeds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookedsRepository extends JpaRepository<Bookeds, Long> {
    List<Bookeds> findByUserId(Long userId);
    List<Bookeds> findByProviderId(Long providerId);
    List<Bookeds> findByServiceId(Long serviceId);
    List<Bookeds> findByStatus(String status);
    List<Bookeds> findByUserIdAndStatus(Long userId, String status);
    List<Bookeds> findByProviderIdAndStatus(Long providerId, String status);
    
    // Count by status
    long countByStatus(String status);
    
    // Count cancelled bookings
    long countByCancel(Boolean cancel);
    
    // Count by service type
    long countByServiceType(String serviceType);
} 