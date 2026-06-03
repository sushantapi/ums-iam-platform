/*
 * package com.ums.admin.service;
 * 
 * import java.util.List;
 * 
 * import com.ums.admin.dto.response.UserDetailResponse; import
 * com.ums.admin.dto.response.UserSummaryResponse;
 * 
 * public interface AdminUserService {
 * 
 * List<UserSummaryResponse> getAllUsers();
 * 
 * UserDetailResponse getUserById(Long id);
 * 
 * String blockUser(Long id);
 * 
 * String activateUser(Long id); }
 */

package com.ums.admin.service;

import java.util.List;

import com.ums.admin.dto.response.UserSummaryResponse;

public interface AdminUserService {

	List<UserSummaryResponse> getAllUsers();
}