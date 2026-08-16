package com.ums.authorization.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ums.authorization.entity.RoleRevocationOutbox;

import jakarta.persistence.LockModeType;

public interface RoleRevocationOutboxRepository extends JpaRepository<RoleRevocationOutbox, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select event from RoleRevocationOutbox event
			where event.status in :statuses
			order by event.createdAt asc
			""")
	List<RoleRevocationOutbox> findPublishable(
			@Param("statuses") Collection<String> statuses,
			Pageable pageable);
}
