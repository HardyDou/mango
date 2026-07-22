package io.mango.file.core.service.remote;

import java.util.Arrays;

/** Validated in-memory image content ready for the existing file service. */
public record RemoteImageContent(byte[] bytes, String contentType, String extension) {

    public RemoteImageContent {
        bytes = Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }
}
