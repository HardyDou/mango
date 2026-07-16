package io.mango.file.core.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Lookup key for an enabled file storage configuration. */
@Getter
@RequiredArgsConstructor
public class EnabledFileStorageKey {

    private final Long id;

    private final String storageType;

    private final String bucketName;
}
