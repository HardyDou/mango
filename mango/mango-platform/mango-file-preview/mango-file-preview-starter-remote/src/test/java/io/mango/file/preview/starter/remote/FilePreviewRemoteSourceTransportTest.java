package io.mango.file.preview.starter.remote;

import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilePreviewRemoteSourceTransportTest {

    @TempDir
    Path tempDir;

    private final FilePreviewSourceFeignClient sourceFeignClient = mock(FilePreviewSourceFeignClient.class);

    private final FilePreviewRemoteSourceTransport transport =
            new FilePreviewRemoteSourceTransport(sourceFeignClient);

    @Test
    void downloadSource_按响应体流式写盘不整体缓冲() throws IOException {
        byte[] payload = new byte[2 * 1024 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 251);
        }
        TrackingInputStream body = new TrackingInputStream(payload);
        when(sourceFeignClient.downloadSource("ticket-1"))
                .thenReturn(response(body));

        Path target = tempDir.resolve("nested/dir/source.mp4");
        Files.createDirectories(target.getParent());

        transport.downloadSource("ticket-1", target.toFile());

        assertThat(Files.readAllBytes(target)).isEqualTo(payload);
        assertThat(body.isClosed()).isTrue();
        verify(sourceFeignClient).downloadSource("ticket-1");
    }

    @Test
    void downloadSource_响应体为空时抛出可读异常() throws IOException {
        when(sourceFeignClient.downloadSource("ticket-1")).thenReturn(response(null));

        assertThatThrownBy(() -> transport.downloadSource("ticket-1", tempDir.resolve("demo.txt").toFile()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("预览源文件响应为空");
    }

    private Response response(InputStream body) {
        Request request = Request.create(Request.HttpMethod.GET, "http://127.0.0.1/file-preview/sources",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response.Builder builder = Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(Map.of("content-type", List.of("application/octet-stream")));
        if (body == null) {
            return builder.build();
        }
        return builder.body(body, 2 * 1024 * 1024).build();
    }

    /**
     * 记录是否被关闭的输入流：用于断言取源响应体在使用后被正确释放。
     */
    private static final class TrackingInputStream extends InputStream {

        private final byte[] payload;

        private int position;

        private boolean closed;

        private TrackingInputStream(byte[] payload) {
            this.payload = payload;
        }

        @Override
        public int read() {
            return position < payload.length ? payload[position++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position >= payload.length) {
                return -1;
            }
            int size = Math.min(length, payload.length - position);
            System.arraycopy(payload, position, buffer, offset, size);
            position += size;
            return size;
        }

        @Override
        public void close() {
            closed = true;
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
