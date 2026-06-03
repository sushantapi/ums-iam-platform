package com.ums.authorization.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ums.authorization.entity.UserRole;
import com.ums.authorization.repository.UserRoleRepository;
import com.ums.authorization.service.UserRoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

	private final UserRoleRepository userRoleRepository;

	@Override
	public List<UserRole> getUserRoles(UUID userId) {

		return userRoleRepository.findByUserId(userId);
	}

	@Override
	public UserRole assignRole(UserRole userRole) {

		return userRoleRepository.save(userRole);
	}
}