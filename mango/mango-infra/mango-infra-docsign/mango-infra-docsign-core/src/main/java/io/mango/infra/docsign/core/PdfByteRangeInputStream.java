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

    private final FileChannel channel;
    private final long[] starts;
    private final long[] lengths;
    private final byte[] oneByte = new byte[1];
    private int rangeIndex;
    private long remaining;

    PdfByteRangeInputStream(Path document, int[] byteRange) throws IOException {
        if (byteRange == null || byteRange.length != 4) {
            throw new IllegalArgumentException("PDF 签名 ByteRange 格式无效");
        }
        this.channel = FileChannel.open(document, StandardOpenOption.READ);
        this.starts = new long[]{byteRange[0], byteRange[2]};
        this.lengths = new long[]{byteRange[1], byteRange[3]};
        this.rangeIndex = 0;
        this.remaining = lengths[0];
        channel.position(starts[0]);
    }

    @Override
    public int read() throws IOException {
        int read = read(oneByte, 0, 1);
        return read < 0 ? -1 : oneByte[0] & 0xff;
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
