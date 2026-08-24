package com.ums.org.dto;

public record OrganizationLogoDocument(
        byte[] content,
        String contentType,
        String fileName) {
}
