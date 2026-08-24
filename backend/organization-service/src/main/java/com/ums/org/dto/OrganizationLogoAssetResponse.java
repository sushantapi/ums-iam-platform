package com.ums.org.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizationLogoAssetResponse(
        UUID assetId,
        UUID organizationId,
        int version,
        String contentType,
        long byteSize,
        String sha256,
        LocalDateTime createdAt) {
}
