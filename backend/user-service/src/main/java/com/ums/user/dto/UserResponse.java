package com.ums.user.dto;

import java.util.UUID;

public record UserResponse(UUID userId, String email, String firstName, String lastName) {
}