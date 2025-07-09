package com.ZAP_Backend.ZapServices.Repository;
import com.ZAP_Backend.ZapServices.Model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface DocumentsRepository extends JpaRepository<Document,Long> {
}

