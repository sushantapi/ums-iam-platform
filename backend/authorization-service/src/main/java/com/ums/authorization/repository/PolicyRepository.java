package com.ums.authorization.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ums.authorization.entity.Policy;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

	List<Policy> findByRole_IdInAndPermission_Resource_CodeIgnoreCaseAndPermission_ActionIgnoreCaseAndActiveTrueOrderByPriorityAsc(
			List<UUID> roleIds, String resourceCode, String action);
}
