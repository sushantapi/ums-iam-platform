package com.ums.org.storage;

public interface OrganizationLogoStorage {

    void store(String storageKey, byte[] content);

    byte[] read(String storageKey);
}
