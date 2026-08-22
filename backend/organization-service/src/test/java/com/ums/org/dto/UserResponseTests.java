package com.ums.org.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.json.JsonMapper;

class UserResponseTests {

	@Test
	void deserializesUserServiceUserIdAsId() throws Exception {
		UUID userId = UUID.randomUUID();
		String json = """
				{
					"userId": "%s",
					"email": "invitee@example.test",
					"firstName": "Runtime",
					"lastName": "Invitee"
				}
				""".formatted(userId);

		UserResponse response = JsonMapper.builder().build().readValue(json, UserResponse.class);

		assertThat(response.id()).isEqualTo(userId);
		assertThat(response.email()).isEqualTo("invitee@example.test");
	}
}
