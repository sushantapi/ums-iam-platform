package com.ums.org.repositoty;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.org.entity.OrganizationProfile;

public interface OrganizationProfileRepository extends JpaRepository<OrganizationProfile, UUID> {
}
