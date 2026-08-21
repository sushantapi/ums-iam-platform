package com.ums.org.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ums.org.entity.OrganizationInvitation;
import com.ums.org.repositoty.OrganizationInvitationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationInvitationDeliveryStateService {

	private final OrganizationInvitationRepository invitationRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markNotificationSent(UUID invitationId, LocalDateTime sentAt) {
		OrganizationInvitation invitation = invitationRepository.findById(invitationId).orElse(null);
		if (invitation == null) {
			log.warn("Invitation delivery state update skipped because invitation was not found invitationId={}", invitationId);
			return;
		}
		if (!invitation.isPending()) {
			log.info("Invitation delivery state update skipped for terminal invitation invitationId={} status={}",
					invitationId, invitation.getStatus());
			return;
		}

		invitation.markNotificationSent(sentAt);
		invitationRepository.saveAndFlush(invitation);
	}
}
