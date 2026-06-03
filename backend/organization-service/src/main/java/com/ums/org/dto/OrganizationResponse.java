package com.ums.org.dto;

import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, String description) {
}