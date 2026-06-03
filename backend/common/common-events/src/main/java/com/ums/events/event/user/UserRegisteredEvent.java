package com.ums.events.event.user;

import java.io.Serializable;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private UUID userId;
	private String email;
	private String firstName;
	private String lastName;
}