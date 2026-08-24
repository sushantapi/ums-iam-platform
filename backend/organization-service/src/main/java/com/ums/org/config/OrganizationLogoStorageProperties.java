package com.ums.org.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "organization.logo.storage")
@Getter
@Setter
public class OrganizationLogoStorageProperties {

    private String root = "./data/organization-assets";

    private long maxBytes = 2L * 1024L * 1024L;
}
