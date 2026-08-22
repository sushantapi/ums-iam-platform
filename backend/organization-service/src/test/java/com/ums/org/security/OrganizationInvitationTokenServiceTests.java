package com.ums.org.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrganizationInvitationTokenServiceTests {

	private final OrganizationInvitationTokenService tokenService = new OrganizationInvitationTokenService();

	@Test
	void issuesUrlSafeRandomTokenAndLowercaseSha256Hash() {
		IssuedInvitationToken issued = tokenService.issue();

		assertThat(issued.rawToken()).hasSize(43).matches("[A-Za-z0-9_-]{43}");
		assertThat(issued.tokenHash()).matches("[0-9a-f]{64}");
		assertThat(issued.tokenHash()).isEqualTo(tokenService.hash(issued.rawToken()));
		assertThat(issued.toString()).doesNotContain(issued.rawToken()).doesNotContain(issued.tokenHash());
	}

	@Test
	void separateIssuanceProducesDifferentBearerMaterial() {
		IssuedInvitationToken first = tokenService.issue();
		IssuedInvitationToken second = tokenService.issue();

		assertThat(second.rawToken()).isNotEqualTo(first.rawToken());
		assertThat(second.tokenHash()).isNotEqualTo(first.tokenHash());
	}

	@Test
	void rejectsBlankRawTokenForHashing() {
		assertThatThrownBy(() -> tokenService.hash("  "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("raw invitation token is required");
	}
}
