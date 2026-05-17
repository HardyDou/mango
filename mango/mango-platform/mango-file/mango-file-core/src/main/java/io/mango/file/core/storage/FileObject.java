package io.mango.file.core.storage;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件对象读取结果。
 *
 * @param inputStream 文件流
 * @param contentLength 文件大小
 * @param contentType 内容类型
 */
public record FileObject(InputStream inputStream, long contentLength, String contentType) {

    public static FileObject of(InputStream inputStream, long contentLength, String contentType, Runnable closeCallback) {
        return new FileObject(new CallbackInputStream(inputStream, closeCallback), contentLength, contentType);
    }

    private static final class CallbackInputStream extends FilterInputStream {

        private final Runnable closeCallback;
        private boolean closed;

        private CallbackInputStream(InputStream inputStream, Runnable closeCallback) {
            super(inputStream);
            this.closeCallback = closeCallback;
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            IOException thrown = null;
            try {
                super.close();
            } catch (IOException e) {
                thrown = e;
            } finally {
                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
            if (thrown != null) {
                throw thrown;
            }
        }
    }
}
