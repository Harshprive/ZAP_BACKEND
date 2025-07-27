package com.ZAP_Backend.ZapServices.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ZAP_Backend.ZapServices.Model.BankAccount;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    // You can add custom query methods here if needed
} 