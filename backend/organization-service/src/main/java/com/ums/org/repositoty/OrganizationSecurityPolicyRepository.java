package com.ums.org.repositoty;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.org.entity.OrganizationSecurityPolicy;

public interface OrganizationSecurityPolicyRepository extends JpaRepository<OrganizationSecurityPolicy, UUID> {
}
