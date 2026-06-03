package com.ums.org.repositoty;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.org.entity.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

	Optional<Organization> findBySlug(String slug);
}