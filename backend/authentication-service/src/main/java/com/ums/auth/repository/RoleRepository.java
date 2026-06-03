package com.ums.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.auth.entity.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);
    
    

}