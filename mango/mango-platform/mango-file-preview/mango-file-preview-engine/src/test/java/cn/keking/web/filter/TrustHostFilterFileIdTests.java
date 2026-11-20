package cn.keking.web.filter;

import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验收 #1357 三条：带 {@code fileId} 放行、带非可信 {@code url} 仍 403、两者都缺仍 403。
 */
class TrustHostFilterFileIdTests {

    private final TrustHostFilter filter = new TrustHostFilter();
    private final AtomicBoolean chainCalled = new AtomicBoolean();
    private final AtomicInteger status = new AtomicInteger();
    private final StringWriter body = new StringWriter();
    private final PrintWriter writer = new PrintWriter(body);
    private final FileDownloader downloader = new FileDownloader() {
        @Override
        public boolean supports(FileAttribute fileAttribute) {
            return true;
        }

        @Override
        public ReturnResponse<String> download(FileAttribute fileAttribute, String fileName) {
            return new ReturnResponse<>(ReturnResponse.SUCCESS_CODE, "ok", fileName);
        }
    };

    private final FilterChain chain = (request, response) -> chainCalled.set(true);

    @BeforeEach
    void setUp() {
        filter.init(null);
    }

    @AfterEach
    void tearDown() {
        FileDownloaders.unregister(downloader);
        chainCalled.set(false);
    }

    @Test
    void allowsFileIdRequestWhenDownloaderRegistered() throws Exception {
        FileDownloaders.register(downloader);

        filter.doFilter(request(Map.of("fileId", "short-lived-token")), response(), chain);

        assertTrue(chainCalled.get(), "带 fileId 的进程内取源请求应放行");
        assertEquals(0, status.get());
    }

    @Test
    void blocksUrlRequestFromUntrustedHost() throws Exception {
        FileDownloaders.register(downloader);

        filter.doFilter(request(Map.of("url", "http://untrusted.example.com/a.docx")), response(), chain);

        writer.flush();
        assertFalse(chainCalled.get());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, status.get());
        assertTrue(body.toString().contains("current_host") || body.toString().contains("UNKNOWN"), body.toString());
    }

    @Test
    void blocksRequestWithoutAnySource() throws Exception {
        FileDownloaders.register(downloader);

        filter.doFilter(request(Map.of()), response(), chain);

        assertFalse(chainCalled.get());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, status.get());
    }

    private ServletRequest request(Map<String, String> parameters) {
        return (ServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "getServletPath" -> "/onlinePreview";
                    case "getContextPath" -> "";
                    default -> null;
                });
    }

    private ServletResponse response() {
        return (ServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        status.set((Integer) args[0]);
                        yield null;
                    }
                    case "getWriter" -> writer;
                    case "isCommitted" -> false;
                    default -> null;
                });
    }
}
