package io.mango.file.core.service.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** File download options assembled by the binary HTTP adapter. */
@Getter
@RequiredArgsConstructor
public class FileDownloadOptions {

    private final Long id;

    private final String compression;

    private final Long perFileTargetSizeBytes;
}
