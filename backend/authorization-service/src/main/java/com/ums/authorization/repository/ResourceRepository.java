package com.ums.authorization.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.authorization.entity.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

	Optional<Resource> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);
}
