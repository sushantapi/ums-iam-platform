package com.ums.org.service;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ums.events.event.AuditEvent;
import com.ums.events.publisher.AuditPublisher;
import com.ums.org.config.OrganizationLogoStorageProperties;
import com.ums.org.dto.OrganizationLogoAssetResponse;
import com.ums.org.dto.OrganizationLogoDocument;
import com.ums.org.entity.Organization;
import com.ums.org.entity.OrganizationLogoAsset;
import com.ums.org.entity.OrganizationProfile;
import com.ums.org.exception.BadRequestException;
import com.ums.org.exception.ResourceNotFoundException;
import com.ums.org.repositoty.OrganizationLogoAssetRepository;
import com.ums.org.repositoty.OrganizationProfileRepository;
import com.ums.org.repositoty.OrganizationRepository;
import com.ums.org.storage.OrganizationLogoStorage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationLogoService {

    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    private final OrganizationRepository organizationRepository;
    private final OrganizationProfileRepository profileRepository;
    private final OrganizationLogoAssetRepository logoAssetRepository;
    private final OrganizationAccessService accessService;
    private final OrganizationLogoStorage storage;
    private final OrganizationLogoStorageProperties storageProperties;
    private final AuditPublisher auditPublisher;

    @Transactional
    public OrganizationLogoAssetResponse upload(
            UUID organizationId,
            MultipartFile file,
            UUID actorUserId,
            boolean superAdmin) {
        Organization organization = organizationRepository.findByIdForUpdate(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        accessService.assertCanManageMembers(actorUserId, organization, superAdmin);

        ValidatedImage image = validate(file);
        int nextVersion = logoAssetRepository.findMaxVersion(organizationId) + 1;
        UUID storageToken = UUID.randomUUID();
        String storageKey = organizationId + "/logos/v" + nextVersion + "/" + storageToken + image.extension();

        storage.store(storageKey, image.content());

        OrganizationLogoAsset asset = OrganizationLogoAsset.builder()
                .organizationId(organizationId)
                .version(nextVersion)
                .contentType(image.contentType())
                .byteSize(image.content().length)
                .sha256(sha256(image.content()))
                .storageKey(storageKey)
                .build();
        OrganizationLogoAsset saved = logoAssetRepository.save(asset);

        OrganizationProfile profile = profileRepository.findById(organizationId)
                .orElseGet(() -> OrganizationProfile.builder()
                        .organizationId(organizationId)
                        .build());
        profile.setLogoAssetId(saved.getId());
        profile.setLogoAssetVersion(saved.getVersion());
        profileRepository.save(profile);

        publishAudit(actorUserId, organizationId, saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationLogoDocument getCurrent(
            UUID organizationId,
            UUID actorUserId,
            boolean superAdmin) {
        Organization organization = getOrganization(organizationId);
        accessService.assertCanViewOrganization(actorUserId, organization, superAdmin);

        OrganizationProfile profile = profileRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization logo not found"));
        if (profile.getLogoAssetId() == null) {
            throw new ResourceNotFoundException("Organization logo not found");
        }
        return loadAsset(organizationId, profile.getLogoAssetId());
    }

    @Transactional(readOnly = true)
    public OrganizationLogoDocument getVersion(
            UUID organizationId,
            UUID assetId,
            UUID actorUserId,
            boolean superAdmin) {
        Organization organization = getOrganization(organizationId);
        accessService.assertCanViewOrganization(actorUserId, organization, superAdmin);
        return loadAsset(organizationId, assetId);
    }

    @Transactional(readOnly = true)
    public OrganizationLogoDocument getInternalVersion(UUID organizationId, UUID assetId) {
        getOrganization(organizationId);
        return loadAsset(organizationId, assetId);
    }

    private OrganizationLogoDocument loadAsset(UUID organizationId, UUID assetId) {
        OrganizationLogoAsset asset = logoAssetRepository.findByIdAndOrganizationId(assetId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization logo asset not found"));
        byte[] content = storage.read(asset.getStorageKey());
        return new OrganizationLogoDocument(
                content,
                asset.getContentType(),
                "organization-logo-v" + asset.getVersion() + extensionFor(asset.getContentType()));
    }

    private Organization getOrganization(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Organization logo file is required");
        }
        if (file.getSize() > storageProperties.getMaxBytes()) {
            throw new BadRequestException("Organization logo exceeds maximum allowed size");
        }

        String declaredContentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!declaredContentType.equals("image/png")
                && !declaredContentType.equals("image/jpeg")
                && !declaredContentType.equals("image/jpg")) {
            throw new BadRequestException("Organization logo must be PNG or JPEG");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Organization logo could not be read");
        }
        if (content.length == 0 || content.length > storageProperties.getMaxBytes()) {
            throw new BadRequestException("Organization logo has invalid size");
        }

        if (hasPrefix(content, PNG_SIGNATURE)) {
            return new ValidatedImage(content, "image/png", ".png");
        }
        if (content.length >= 3
                && (content[0] & 0xff) == 0xff
                && (content[1] & 0xff) == 0xd8
                && (content[2] & 0xff) == 0xff) {
            return new ValidatedImage(content, "image/jpeg", ".jpg");
        }
        throw new BadRequestException("Organization logo content is not a valid PNG or JPEG image");
    }

    private boolean hasPrefix(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }

    private OrganizationLogoAssetResponse toResponse(OrganizationLogoAsset asset) {
        return new OrganizationLogoAssetResponse(
                asset.getId(),
                asset.getOrganizationId(),
                asset.getVersion(),
                asset.getContentType(),
                asset.getByteSize(),
                asset.getSha256(),
                asset.getCreatedAt());
    }

    private void publishAudit(UUID actorUserId, UUID organizationId, OrganizationLogoAsset asset) {
        try {
            auditPublisher.publish(AuditEvent.builder()
                    .eventType("organization.logo.updated")
                    .serviceName("organization-service")
                    .userId(actorUserId.toString())
                    .action("ORGANIZATION_LOGO_UPDATE")
                    .entityType("ORGANIZATION_LOGO_ASSET")
                    .entityId(asset.getId().toString())
                    .details("Organization logo version " + asset.getVersion() + " activated for organization " + organizationId)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            log.warn(
                    "Organization logo audit publication failed organizationId={} assetId={} failureType={}",
                    organizationId,
                    asset.getId(),
                    ex.getClass().getName());
        }
    }

    private record ValidatedImage(byte[] content, String contentType, String extension) {
    }
}
