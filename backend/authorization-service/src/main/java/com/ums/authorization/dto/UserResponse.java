package com.ums.authorization.dto;

import java.util.UUID;

public record UserResponse(UUID userId, String email, String firstName, String lastName) {
}
