package com.ums.org.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.ums.org.config.OrganizationLogoStorageProperties;

class FileSystemOrganizationLogoStorageTests {

    @TempDir
    Path tempDir;

    @Test
    void storesAndReadsImmutableAssetPath() {
        OrganizationLogoStorageProperties properties = new OrganizationLogoStorageProperties();
        properties.setRoot(tempDir.toString());
        FileSystemOrganizationLogoStorage storage = new FileSystemOrganizationLogoStorage(properties);
        storage.initialize();
        byte[] content = new byte[] { 1, 2, 3, 4 };

        storage.store("org/logos/v1/logo.png", content);

        assertArrayEquals(content, storage.read("org/logos/v1/logo.png"));
    }

    @Test
    void rejectsStorageKeyEscapingConfiguredRoot() {
        OrganizationLogoStorageProperties properties = new OrganizationLogoStorageProperties();
        properties.setRoot(tempDir.toString());
        FileSystemOrganizationLogoStorage storage = new FileSystemOrganizationLogoStorage(properties);
        storage.initialize();

        assertThrows(IllegalArgumentException.class,
                () -> storage.store("../escape.png", new byte[] { 1 }));
    }
}
