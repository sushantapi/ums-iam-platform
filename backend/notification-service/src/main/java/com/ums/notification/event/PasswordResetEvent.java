package com.ums.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEvent {

	private Long userId;

	private String email;

	private String firstName;

	private String resetLink;
}