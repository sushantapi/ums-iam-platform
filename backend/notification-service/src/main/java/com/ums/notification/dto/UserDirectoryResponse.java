package com.ums.notification.dto;

import java.util.UUID;

public record UserDirectoryResponse(UUID userId, String email, String firstName, String lastName) {
}
