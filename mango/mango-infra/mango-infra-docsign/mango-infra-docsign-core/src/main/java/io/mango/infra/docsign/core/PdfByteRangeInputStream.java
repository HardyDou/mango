package io.mango.infra.docsign.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Streams the two PDF byte ranges protected by a detached signature.
 */
final class PdfByteRangeInputStream extends InputStream {

    private static final int BYTE_RANGE_COMPONENT_COUNT = 4;
    private static final int SECOND_RANGE_START_INDEX = 2;
    private static final int SECOND_RANGE_LENGTH_INDEX = 3;
    private static final int UNSIGNED_BYTE_MASK = 0xFF;

    private final FileChannel channel;
    private final long[] starts;
    private final long[] lengths;
    private final byte[] oneByte = new byte[1];
    private int rangeIndex;
    private long remaining;

    PdfByteRangeInputStream(Path document, int[] byteRange) throws IOException {
        if (byteRange == null || byteRange.length != BYTE_RANGE_COMPONENT_COUNT) {
            throw new IllegalArgumentException("PDF 签名 ByteRange 格式无效");
        }
        this.channel = FileChannel.open(document, StandardOpenOption.READ);
        this.starts = new long[]{byteRange[0], byteRange[SECOND_RANGE_START_INDEX]};
        this.lengths = new long[]{byteRange[1], byteRange[SECOND_RANGE_LENGTH_INDEX]};
        this.rangeIndex = 0;
        this.remaining = lengths[0];
        channel.position(starts[0]);
    }

    @Override
    public int read() throws IOException {
        int read = read(oneByte, 0, 1);
        return read < 0 ? -1 : oneByte[0] & UNSIGNED_BYTE_MASK;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, buffer.length);
        if (length == 0) {
            return 0;
        }
        while (remaining == 0) {
            if (!advanceRange()) {
                return -1;
            }
        }
        int requested = (int) Math.min(length, remaining);
        int read = channel.read(ByteBuffer.wrap(buffer, offset, requested));
        if (read < 0) {
            throw new IOException("PDF 签名 ByteRange 提前结束");
        }
        remaining -= read;
        return read;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    private boolean advanceRange() throws IOException {
        rangeIndex++;
        if (rangeIndex >= starts.length) {
            return false;
        }
        remaining = lengths[rangeIndex];
        channel.position(starts[rangeIndex]);
        return true;
    }
}
