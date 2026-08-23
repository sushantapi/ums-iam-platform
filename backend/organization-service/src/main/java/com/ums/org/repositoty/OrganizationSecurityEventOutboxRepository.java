package com.ums.org.repositoty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.org.entity.OrganizationSecurityEventOutbox;
import com.ums.org.entity.OrganizationSecurityEventOutbox.Status;

import jakarta.persistence.LockModeType;

public interface OrganizationSecurityEventOutboxRepository
		extends JpaRepository<OrganizationSecurityEventOutbox, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from OrganizationSecurityEventOutbox e "
			+ "where e.status = :status "
			+ "and (e.nextAttemptAt is null or e.nextAttemptAt <= :now) "
			+ "order by e.createdAt asc")
	List<OrganizationSecurityEventOutbox> findReadyForDispatch(
			@Param("status") Status status,
			@Param("now") LocalDateTime now,
			Pageable pageable);
}
