package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.Issue;
import com.ZAP_Backend.ZapServices.Model.User;
import com.ZAP_Backend.ZapServices.Model.ServiceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByUser(User user);
    List<Issue> findByServiceProvider(ServiceProvider provider);
    
    // Count by status
    long countByStatus(String status);
    
    // Count reattachment issues
    long countByReattachment(Boolean reattachment);
}
