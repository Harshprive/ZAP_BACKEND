package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Completed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompletedRepository extends JpaRepository<Completed, Long> {
    List<Completed> findByUserId(Long userId);
    List<Completed> findByProviderId(Long providerId);
    List<Completed> findByServiceId(Long serviceId);
    List<Completed> findByCategoryId(Long categoryId);
    List<Completed> findByProblemOccurs(boolean problemOccurs);

    // Count completed services with problems
    long countByProblemOccursTrue();

    // Count completed services without problems
    long countByProblemOccursFalse();
} 