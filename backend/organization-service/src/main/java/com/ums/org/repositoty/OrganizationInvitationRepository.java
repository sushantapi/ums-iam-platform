package com.ums.org.repositoty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationInvitationStatus;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

	Optional<OrganizationInvitation> findByTokenHash(String tokenHash);

	Optional<OrganizationInvitation> findByOrganizationIdAndNormalizedEmailAndStatus(
			UUID organizationId, String normalizedEmail, OrganizationInvitationStatus status);

	boolean existsByOrganizationIdAndNormalizedEmailAndStatus(
			UUID organizationId, String normalizedEmail, OrganizationInvitationStatus status);

	List<OrganizationInvitation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
