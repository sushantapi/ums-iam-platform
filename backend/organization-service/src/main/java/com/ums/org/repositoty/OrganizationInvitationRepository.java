package com.ums.org.repositoty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.enums.OrganizationInvitationStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, UUID> {

	Optional<OrganizationInvitation> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select invitation from OrganizationInvitation invitation where invitation.tokenHash = :tokenHash")
	Optional<OrganizationInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

	Optional<OrganizationInvitation> findByOrganizationIdAndNormalizedEmailAndStatus(
			UUID organizationId, String normalizedEmail, OrganizationInvitationStatus status);

	boolean existsByOrganizationIdAndNormalizedEmailAndStatus(
			UUID organizationId, String normalizedEmail, OrganizationInvitationStatus status);

	List<OrganizationInvitation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
