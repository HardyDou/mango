package io.mango.file.starter.remoteimport;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.mango.common.exception.BizException;
import io.mango.file.api.enums.FileCode;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.service.remote.RemoteImageAddressPolicy;
import io.mango.file.core.service.remote.RemoteImageContent;
import io.mango.file.core.service.remote.RemoteImageTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApacheRemoteImageFetcherTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void fetch_acceptsValidatedPngAndReturnsManagedContent() throws Exception {
        server = startServer("/image", exchange -> respond(exchange, 200, "image/png", PNG));
        RemoteImageAddressPolicy policy = mock(RemoteImageAddressPolicy.class);
        URI uri = publicUri("public.example", "/image");
        when(policy.validate(uri.toString())).thenReturn(target(uri));

        RemoteImageContent content = fetcher(policy, 1024).fetch(uri.toString());

        assertThat(content.contentType()).isEqualTo("image/png");
        assertThat(content.extension()).isEqualTo("png");
        assertThat(content.bytes()).containsExactly(PNG);
    }

    @Test
    void fetch_revalidatesRedirectAndDoesNotForwardCredentials() throws Exception {
        AtomicReference<Headers> redirectedHeaders = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        URI redirected = publicUri("redirect.example", "/image");
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", redirected.toString());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/image", exchange -> {
            redirectedHeaders.set(exchange.getRequestHeaders());
            respond(exchange, 200, "image/png", PNG);
        });
        server.start();
        URI initial = publicUri("public.example", "/redirect");
        RemoteImageAddressPolicy policy = mock(RemoteImageAddressPolicy.class);
        when(policy.validate(initial.toString())).thenReturn(target(initial));
        when(policy.validate(any(URI.class))).thenReturn(target(redirected));

        RemoteImageContent content = fetcher(policy, 1024).fetch(initial.toString());

        assertThat(content.contentType()).isEqualTo("image/png");
        verify(policy).validate(redirected);
        Headers headers = redirectedHeaders.get();
        assertThat(headers).isNotNull();
        assertThat(headers.getFirst("Authorization")).isNull();
        assertThat(headers.getFirst("Cookie")).isNull();
        assertThat(headers.getFirst("Referer")).isNull();
        assertThat(headers.getFirst("Accept")).isEqualTo("image/*");
    }

    @Test
    void fetch_rejectsOversizedAndMismatchedContent() throws Exception {
        server = startServer("/oversized", exchange -> respond(
                exchange, 200, "image/png", new byte[32]));
        RemoteImageAddressPolicy oversizedPolicy = mock(RemoteImageAddressPolicy.class);
        URI oversized = publicUri("public.example", "/oversized");
        when(oversizedPolicy.validate(oversized.toString())).thenReturn(target(oversized));

        assertThatThrownBy(() -> fetcher(oversizedPolicy, 8).fetch(oversized.toString()))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_IMAGE_TOO_LARGE.getCode()));

        server.stop(0);
        server = startServer("/svg", exchange -> respond(
                exchange, 200, "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8)));
        RemoteImageAddressPolicy svgPolicy = mock(RemoteImageAddressPolicy.class);
        URI svg = publicUri("public.example", "/svg");
        when(svgPolicy.validate(svg.toString())).thenReturn(target(svg));

        assertThatThrownBy(() -> fetcher(svgPolicy, 1024).fetch(svg.toString()))
                .isInstanceOfSatisfying(BizException.class,
                        error -> assertThat(error.getCode()).isEqualTo(FileCode.FILE_REMOTE_CONTENT_INVALID.getCode()));
    }

    private ApacheRemoteImageFetcher fetcher(RemoteImageAddressPolicy policy, long maxSize) {
        FileProperties.RemoteImport properties = new FileProperties.RemoteImport();
        properties.setConnectTimeoutMillis(2000);
        properties.setReadTimeoutMillis(3000);
        properties.setMaxSize(maxSize);
        return new ApacheRemoteImageFetcher(policy, properties);
    }

    private HttpServer startServer(String path, ThrowingHandler handler) throws IOException {
        HttpServer result = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        result.createContext(path, exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception exception) {
                exchange.close();
                if (exception instanceof IOException ioException) {
                    throw ioException;
                }
                throw new IOException("Test HTTP handler failed", exception);
            }
        });
        result.start();
        return result;
    }

    private URI publicUri(String host, String path) {
        return URI.create("http://" + host + ":" + server.getAddress().getPort() + path);
    }

    private RemoteImageTarget target(URI uri) {
        return new RemoteImageTarget(uri, new InetAddress[]{InetAddress.getLoopbackAddress()});
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
