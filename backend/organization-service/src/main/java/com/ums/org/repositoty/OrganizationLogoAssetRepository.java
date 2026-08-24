package com.ums.org.repositoty;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.org.entity.OrganizationLogoAsset;

public interface OrganizationLogoAssetRepository extends JpaRepository<OrganizationLogoAsset, UUID> {

    Optional<OrganizationLogoAsset> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("select coalesce(max(a.version), 0) from OrganizationLogoAsset a where a.organizationId = :organizationId")
    int findMaxVersion(@Param("organizationId") UUID organizationId);
}
