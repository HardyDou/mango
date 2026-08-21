package io.mango.infra.docsign.core;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Counts written bytes while retaining caller ownership of the wrapped stream.
 */
final class CountingNonClosingOutputStream extends FilterOutputStream {

    private long count;

    CountingNonClosingOutputStream(OutputStream output) {
        super(output);
    }

    @Override
    public void write(int value) throws IOException {
        out.write(value);
        count++;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        out.write(buffer, offset, length);
        count += length;
    }

    @Override
    public void close() throws IOException {
        flush();
    }

    long count() {
        return count;
    }
}
