package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.ZapAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZapAmountRepository extends JpaRepository<ZapAmount, Long> {
    // You can add custom query methods here if needed
} 