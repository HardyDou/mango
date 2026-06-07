package io.mango.job.support.nativeengine;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * 同时写原始控制台和 Job 日志缓冲的输出流。
 */
final class MangoJobTeePrintStream extends PrintStream {

    private final PrintStream delegate;

    private final PrintStream capture;

    MangoJobTeePrintStream(PrintStream delegate, PrintStream capture) {
        super(OutputStream.nullOutputStream(), true);
        this.delegate = delegate;
        this.capture = capture;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        delegate.write(buf, off, len);
        capture.write(buf, off, len);
    }

    @Override
    public void write(int b) {
        delegate.write(b);
        capture.write(b);
    }

    @Override
    public void flush() {
        delegate.flush();
        capture.flush();
    }
}
