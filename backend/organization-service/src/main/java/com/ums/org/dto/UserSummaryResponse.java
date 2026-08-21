package com.ums.org.dto;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String firstName, String lastName, String email) {
}
