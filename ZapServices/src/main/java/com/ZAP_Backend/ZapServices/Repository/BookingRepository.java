package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Booking;
import com.ZAP_Backend.ZapServices.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByProviderId(Long providerId);
    List<Booking> findByUser(User user);
}
