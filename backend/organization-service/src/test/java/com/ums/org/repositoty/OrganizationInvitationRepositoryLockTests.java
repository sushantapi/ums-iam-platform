package com.ums.org.repositoty;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

class OrganizationInvitationRepositoryLockTests {

	@Test
	void scopedLifecycleLookupUsesPessimisticWriteLockAndOrganizationBoundary() throws Exception {
		Method method = OrganizationInvitationRepository.class.getMethod(
				"findScopedByIdForUpdate", UUID.class, UUID.class);

		Lock lock = method.getAnnotation(Lock.class);
		Query query = method.getAnnotation(Query.class);

		assertThat(lock).isNotNull();
		assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
		assertThat(query).isNotNull();
		assertThat(query.value())
				.contains("invitation.id = :invitationId")
				.contains("invitation.organizationId = :organizationId");
	}
}