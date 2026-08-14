package com.ums.org.repositoty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.org.entity.OrganizationMember;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

	boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

	List<OrganizationMember> findByOrganizationId(UUID organizationId);

	List<OrganizationMember> findByUserId(UUID userId);
}
