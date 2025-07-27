package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Schedule;
import com.ZAP_Backend.ZapServices.Model.User;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByUserId(Long userId);
    List<Schedule> findByProviderId(Long providerId);
    List<Schedule> findByServiceId(Long serviceId);
    List<Schedule> findByUser(User user);
    List<Schedule> findByProvider(ServiceProvider provider);
    List<Schedule> findByProviderAndStatus(ServiceProvider provider, String status);
    
    // Count by status
    long countByStatus(String status);
} 