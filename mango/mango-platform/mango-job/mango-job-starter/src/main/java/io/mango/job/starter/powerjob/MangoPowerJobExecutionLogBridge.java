package io.mango.job.starter.powerjob;

import tech.powerjob.worker.log.OmsLogger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * Bridges console output produced by the current PowerJob task thread to OmsLogger.
 */
final class MangoPowerJobExecutionLogBridge {

    private static final Object INSTALL_LOCK = new Object();
    private static final PrintStream ORIGINAL_OUT = System.out;
    private static final PrintStream ORIGINAL_ERR = System.err;
    private static final ThreadLocal<LogSession> SESSION = new ThreadLocal<>();
    private static volatile boolean installed;
    private static int activeSessions;

    private MangoPowerJobExecutionLogBridge() {
    }

    static <T> T capture(OmsLogger omsLogger, Callable<T> action) throws Exception {
        if (omsLogger == null) {
            return action.call();
        }
        LogSession previous;
        LogSession current = new LogSession(omsLogger);
        synchronized (INSTALL_LOCK) {
            installSystemStreams();
            activeSessions++;
            previous = SESSION.get();
            SESSION.set(current);
        }
        try {
            return action.call();
        } finally {
            current.flush();
            synchronized (INSTALL_LOCK) {
                if (previous == null) {
                    SESSION.remove();
                } else {
                    SESSION.set(previous);
                }
                activeSessions--;
                if (activeSessions == 0) {
                    System.setOut(ORIGINAL_OUT);
                    System.setErr(ORIGINAL_ERR);
                    installed = false;
                }
            }
        }
    }

    private static void installSystemStreams() {
        if (installed) {
            return;
        }
        synchronized (INSTALL_LOCK) {
            if (installed) {
                return;
            }
            System.setOut(new PrintStream(new CapturingOutputStream(ORIGINAL_OUT, false), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new CapturingOutputStream(ORIGINAL_ERR, true), true, StandardCharsets.UTF_8));
            installed = true;
        }
    }

    private static final class CapturingOutputStream extends OutputStream {

        private final PrintStream delegate;

        private final boolean error;

        private CapturingOutputStream(PrintStream delegate, boolean error) {
            this.delegate = delegate;
            this.error = error;
        }

        @Override
        public void write(int value) {
            delegate.write(value);
            capture((byte) value);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            delegate.write(bytes, offset, length);
            for (int i = offset; i < offset + length; i++) {
                capture(bytes[i]);
            }
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        private void capture(byte value) {
            LogSession session = SESSION.get();
            if (session != null) {
                session.append(error, value);
            }
        }
    }

    private static final class LogSession {

        private final OmsLogger omsLogger;

        private final StringBuilder out = new StringBuilder();

        private final StringBuilder err = new StringBuilder();

        private boolean writing;

        private LogSession(OmsLogger omsLogger) {
            this.omsLogger = omsLogger;
        }

        private void append(boolean error, byte value) {
            if (writing) {
                return;
            }
            char ch = (char) (value & 0xff);
            if (ch == '\r') {
                return;
            }
            StringBuilder target = error ? err : out;
            if (ch == '\n') {
                flushLine(error, target);
                return;
            }
            target.append(ch);
        }

        private void flush() {
            flushLine(false, out);
            flushLine(true, err);
        }

        private void flushLine(boolean error, StringBuilder target) {
            if (target.isEmpty()) {
                return;
            }
            String line = target.toString();
            target.setLength(0);
            writing = true;
            try {
                if (error) {
                    omsLogger.error("[System.err] {}", line);
                } else {
                    omsLogger.info("[System.out] {}", line);
                }
            } finally {
                writing = false;
            }
        }
    }
}
