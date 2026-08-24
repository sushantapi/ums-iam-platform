package com.ums.org.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_logo_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationLogoAsset extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "organization_id", nullable = false, length = 36, updatable = false)
    private UUID organizationId;

    @Column(name = "asset_version", nullable = false, updatable = false)
    private int version;

    @Column(name = "content_type", nullable = false, length = 50, updatable = false)
    private String contentType;

    @Column(name = "byte_size", nullable = false, updatable = false)
    private long byteSize;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sha256", nullable = false, length = 64, updatable = false)
    private String sha256;

    @Column(name = "storage_key", nullable = false, length = 500, unique = true, updatable = false)
    private String storageKey;
}
