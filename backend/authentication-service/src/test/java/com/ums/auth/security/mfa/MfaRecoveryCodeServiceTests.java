package com.ums.auth.security.mfa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

class MfaRecoveryCodeServiceTests {

	@Test
	void generatesUniqueHighEntropyOneTimeCodes() {
		MfaProperties properties = new MfaProperties();
		properties.setRecoveryCodeCount(10);
		properties.setRecoveryCodeBytes(10);
		MfaRecoveryCodeService service = new MfaRecoveryCodeService(properties);

		List<String> codes = service.generateCodes();

		assertThat(codes).hasSize(10);
		assertThat(new HashSet<>(codes)).hasSize(10);
		assertThat(codes).allMatch(code -> code.matches("[A-Z2-7]{4}(?:-[A-Z2-7]{4}){3}"));
	}

	@Test
	void storesOnlyHashAndComparesNormalizedCodeInConstantTimePath() {
		MfaRecoveryCodeService service = new MfaRecoveryCodeService(new MfaProperties());
		String raw = service.generateCodes().getFirst();
		String hash = service.hash(raw);

		assertThat(hash).hasSize(64);
		assertThat(hash).doesNotContain(raw.replace("-", ""));
		assertThat(service.matches(raw.toLowerCase(), hash)).isTrue();
		assertThat(service.matches("AAAA-BBBB-CCCC-DDDD", hash)).isFalse();
	}
}
