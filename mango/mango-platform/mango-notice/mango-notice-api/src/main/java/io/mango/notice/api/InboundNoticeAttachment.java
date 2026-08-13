package io.mango.notice.api;

import java.io.IOException;
import java.io.InputStream;

/**
 * Attachment stream transferred by an inbound adapter.
 *
 * <p>The receiver owns the stream after {@code receive} is called and closes it after the
 * file service has consumed it. Adapters must not reuse the stream afterwards.</p>
 */
public record InboundNoticeAttachment(
        int index,
        String fileName,
        String contentType,
        long fileSize,
        InputStream content) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        if (content != null) {
            content.close();
        }
    }
}
