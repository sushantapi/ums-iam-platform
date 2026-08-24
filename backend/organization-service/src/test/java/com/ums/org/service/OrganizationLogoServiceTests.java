package com.ums.org.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.mock.web.MockMultipartFile;

import com.ums.events.publisher.AuditPublisher;
import com.ums.org.config.OrganizationLogoStorageProperties;
import com.ums.org.dto.OrganizationLogoAssetResponse;
import com.ums.org.dto.OrganizationLogoDocument;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationLogoAsset;
import com.ums.org.entity.OrganizationProfile;
import com.ums.org.enums.OrganizationStatus;
import com.ums.org.exception.BadRequestException;
import com.ums.org.repositoty.OrganizationLogoAssetRepository;
import com.ums.org.repositoty.OrganizationProfileRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.storage.OrganizationLogoStorage;

@ExtendWith(MockitoExtension.class)
class OrganizationLogoServiceTests {

    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01, 0x02
    };

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationProfileRepository profileRepository;

    @Mock
    private OrganizationLogoAssetRepository logoAssetRepository;

    @Mock
    private OrganizationAccessService accessService;

    @Mock
    private OrganizationLogoStorage storage;

    @Mock
    private OrganizationLogoStorageProperties storageProperties;

    @Mock
    private AuditPublisher auditPublisher;

    @InjectMocks
    private OrganizationLogoService service;

    @Test
    void uploadsFirstPngVersionAndActivatesProfilePointer() {
        Organization organization = organization();
        OrganizationProfile profile = OrganizationProfile.builder()
                .organizationId(ORGANIZATION_ID)
                .build();
        when(storageProperties.getMaxBytes()).thenReturn(2L * 1024L * 1024L);
        when(organizationRepository.findByIdForUpdate(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(logoAssetRepository.findMaxVersion(ORGANIZATION_ID)).thenReturn(0);
        when(profileRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(profile));
        when(logoAssetRepository.save(any())).thenAnswer(invocation -> {
            OrganizationLogoAsset asset = invocation.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);
        OrganizationLogoAssetResponse response = service.upload(ORGANIZATION_ID, file, ACTOR_ID, false);

        verify(accessService).assertCanManageMembers(ACTOR_ID, organization, false);
        verify(storage).store(anyString(), eq(PNG));
        assertEquals(1, response.version());
        assertEquals("image/png", response.contentType());
        assertEquals(response.assetId(), profile.getLogoAssetId());
        assertEquals(1, profile.getLogoAssetVersion());
        verify(profileRepository).save(profile);
    }

    @Test
    void replacementCreatesNextImmutableVersion() {
        Organization organization = organization();
        UUID oldAssetId = UUID.randomUUID();
        OrganizationProfile profile = OrganizationProfile.builder()
                .organizationId(ORGANIZATION_ID)
                .logoAssetId(oldAssetId)
                .logoAssetVersion(1)
                .build();
        when(storageProperties.getMaxBytes()).thenReturn(2L * 1024L * 1024L);
        when(organizationRepository.findByIdForUpdate(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(logoAssetRepository.findMaxVersion(ORGANIZATION_ID)).thenReturn(1);
        when(profileRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(profile));
        when(logoAssetRepository.save(any())).thenAnswer(invocation -> {
            OrganizationLogoAsset asset = invocation.getArgument(0);
            asset.setId(UUID.randomUUID());
            return asset;
        });

        service.upload(
                ORGANIZATION_ID,
                new MockMultipartFile("file", "logo.png", "image/png", PNG),
                ACTOR_ID,
                false);

        ArgumentCaptor<OrganizationLogoAsset> assetCaptor = ArgumentCaptor.forClass(OrganizationLogoAsset.class);
        verify(logoAssetRepository).save(assetCaptor.capture());
        assertEquals(2, assetCaptor.getValue().getVersion());
        assertEquals(2, profile.getLogoAssetVersion());
        verify(logoAssetRepository, never()).deleteById(oldAssetId);
    }

    @Test
    void rejectsDeclaredImageWithInvalidMagicBytes() {
        when(storageProperties.getMaxBytes()).thenReturn(2L * 1024L * 1024L);
        when(organizationRepository.findByIdForUpdate(ORGANIZATION_ID)).thenReturn(Optional.of(organization()));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "not-an-image".getBytes());

        assertThrows(BadRequestException.class,
                () -> service.upload(ORGANIZATION_ID, file, ACTOR_ID, false));

        verify(storage, never()).store(anyString(), any());
        verify(logoAssetRepository, never()).save(any());
    }

    @Test
    void rejectsLogoAboveConfiguredLimit() {
        when(storageProperties.getMaxBytes()).thenReturn(8L);
        when(organizationRepository.findByIdForUpdate(ORGANIZATION_ID)).thenReturn(Optional.of(organization()));
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);

        assertThrows(BadRequestException.class,
                () -> service.upload(ORGANIZATION_ID, file, ACTOR_ID, false));

        verify(storage, never()).store(anyString(), any());
    }

    @Test
    void readsHistoricalAssetByOrganizationAndAssetId() {
        Organization organization = organization();
        UUID assetId = UUID.randomUUID();
        OrganizationLogoAsset asset = OrganizationLogoAsset.builder()
                .id(assetId)
                .organizationId(ORGANIZATION_ID)
                .version(1)
                .contentType("image/png")
                .byteSize(PNG.length)
                .sha256("abc")
                .storageKey("org/logo.png")
                .build();
        when(organizationRepository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
        when(logoAssetRepository.findByIdAndOrganizationId(assetId, ORGANIZATION_ID)).thenReturn(Optional.of(asset));
        when(storage.read("org/logo.png")).thenReturn(PNG);

        OrganizationLogoDocument document = service.getVersion(ORGANIZATION_ID, assetId, ACTOR_ID, false);

        verify(accessService).assertCanViewOrganization(ACTOR_ID, organization, false);
        assertEquals("image/png", document.contentType());
        assertEquals("organization-logo-v1.png", document.fileName());
        assertArrayEquals(PNG, document.content());
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
