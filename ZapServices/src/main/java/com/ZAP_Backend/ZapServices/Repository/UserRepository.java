package com.ZAP_Backend.ZapServices.Repository;

import com.ZAP_Backend.ZapServices.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
}
