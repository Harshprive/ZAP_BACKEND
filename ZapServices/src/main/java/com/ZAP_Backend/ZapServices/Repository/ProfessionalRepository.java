package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Professional_Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional_Document,Long> {
}
