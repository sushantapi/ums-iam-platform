package com.ums.org.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.ums.events.publisher.AuditPublisher;
import com.ums.org.dto.OrganizationProfileResponse;
import com.ums.org.dto.UpdateOrganizationProfileRequest;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationProfile;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.repositoty.OrganizationProfileRepository;
import com.ums.org.repositoty.OrganizationRepository;

@ExtendWith(MockitoExtension.class)
class OrganizationProfileServiceTests {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationProfileRepository profileRepository;

    @Mock
    private OrganizationAccessService accessService;

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private OrganizationProfileService service;

    @Test
    void returnsOrganizationNameFallbackWhenProfileDoesNotExist() {
        Organization organization = organization();
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(profileRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

        OrganizationProfileResponse response = service.get(ORGANIZATION_ID, ACTOR_ID, false);

        verify(accessService).assertCanViewOrganization(ACTOR_ID, organization, false);
        assertEquals("Acme Technologies", response.legalName());
        assertEquals("Acme Technologies", response.displayName());
        assertNull(response.logoAssetId());
        assertNull(response.logoAssetVersion());
    }

    @Test
    void updatesAndNormalizesTenantProfile() {
        Organization organization = organization();
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(profileRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());
        when(profileRepository.save(any(OrganizationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrganizationProfileRequest request = new UpdateOrganizationProfileRequest(
                "  Acme Technologies Private Limited  ",
                "  Acme  ",
                "  Bengaluru, Karnataka  ",
                "  PAYROLL@ACME.EXAMPLE  ",
                "  +91 99999 99999  ",
                "  https://acme.example  ",
                "inr",
                "in",
                "  This is a system-generated payslip.  ",
                "  Authorized Signatory  ");

        OrganizationProfileResponse response = service.update(
                ORGANIZATION_ID,
                request,
                ACTOR_ID,
                false);

        ArgumentCaptor<OrganizationProfile> captor = ArgumentCaptor.forClass(OrganizationProfile.class);
        verify(accessService).assertCanManageMembers(ACTOR_ID, organization, false);
        verify(profileRepository).save(captor.capture());

        OrganizationProfile saved = captor.getValue();
        assertEquals("Acme Technologies Private Limited", saved.getLegalName());
        assertEquals("Acme", saved.getDisplayName());
        assertEquals("payroll@acme.example", saved.getBusinessEmail());
        assertEquals("INR", saved.getDefaultCurrency());
        assertEquals("IN", saved.getPayrollCountry());
        assertEquals("Acme", response.displayName());
    }

    @Test
    void doesNotReadProfileWhenOrganizationAccessIsDenied() {
        Organization organization = organization();
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(accessService)
                .assertCanViewOrganization(ACTOR_ID, organization, false);

        assertThrows(
                AccessDeniedException.class,
                () -> service.get(ORGANIZATION_ID, ACTOR_ID, false));

        verify(profileRepository, never()).findById(any());
    }

    private Organization organization() {
        return Organization.builder()
                .id(ORGANIZATION_ID)
                .name("Acme Technologies")
                .slug("acme-technologies")
                .ownerId(UUID.randomUUID())
                .status(OrganizationStatus.ACTIVE)
                .build();
    }
}
