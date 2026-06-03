package com.ums.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ums.admin.dto.response.UserSummaryResponse;
import com.ums.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

	private final AdminUserService adminUserService;

	@GetMapping
	public List<UserSummaryResponse> getAllUsers() {

		System.out.println("ADMIN CONTROLLER HIT");

		return adminUserService.getAllUsers();
	}

	/*
	 * @GetMapping("/{id}") public UserDetailResponse getUserById(@PathVariable Long
	 * id) {
	 * 
	 * return adminUserService.getUserById(id); }
	 * 
	 * @PatchMapping("/{id}/block") public String blockUser(@PathVariable Long id) {
	 * 
	 * return adminUserService.blockUser(id); }
	 * 
	 * @PatchMapping("/{id}/activate") public String activateUser(@PathVariable Long
	 * id) {
	 * 
	 * return adminUserService.activateUser(id); }
	 */
}