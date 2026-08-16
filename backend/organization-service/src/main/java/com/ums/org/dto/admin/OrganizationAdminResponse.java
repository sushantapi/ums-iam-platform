package com.ums.org.dto.admin;

import java.util.UUID;

public record OrganizationAdminResponse(
		UUID id, String name, String slug, String description, UUID ownerId, String status) {
}
