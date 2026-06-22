package com.ums.auth.dto.admin;

import java.time.LocalDate;
import java.util.UUID;

public record AdminSessionFilter(
		UUID userId,
		UUID organizationId,
		String status,
		LocalDate from,
		LocalDate to) {
}
