package com.ums.org.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Component;

import com.ums.org.config.OrganizationLogoStorageProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileSystemOrganizationLogoStorage implements OrganizationLogoStorage {

    private final OrganizationLogoStorageProperties properties;

    private Path root;

    @PostConstruct
    void initialize() {
        try {
            root = Path.of(properties.getRoot()).toAbsolutePath().normalize();
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize organization logo storage", ex);
        }
    }

    @Override
    public void store(String storageKey, byte[] content) {
        Path target = resolve(storageKey);

        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "logo-", ".tmp");
            try {
                Files.write(temporary, content);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store organization logo", ex);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        Path target = resolve(storageKey);
        try {
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read organization logo", ex);
        }
    }

    private Path resolve(String storageKey) {
        if (root == null) {
            initialize();
        }

        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid organization logo storage key");
        }
        return resolved;
    }
}
