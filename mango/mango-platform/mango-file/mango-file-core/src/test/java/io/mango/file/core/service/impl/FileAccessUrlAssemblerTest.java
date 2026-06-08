package io.mango.file.core.service.impl;

import io.mango.file.core.config.FileProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileAccessUrlAssemblerTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void downloadUrl_publicBaseUrlConfigured_usesPublicBaseUrl() {
        FileProperties properties = new FileProperties();
        properties.setPublicBaseUrl("https://files.example.com/api/");
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(properties);

        String url = assembler.downloadUrl(100L);

        assertThat(url).isEqualTo("https://files.example.com/api/file/files/download?id=100");
    }

    @Test
    void downloadUrl_forwardedHeadersPresent_usesExternalProxyAddress() {
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(new FileProperties());
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.header("X-Forwarded-Proto", "https");
        request.header("X-Forwarded-Host", "mango.example.com");
        request.header("X-Forwarded-Port", "443");
        request.header("X-Forwarded-Prefix", "/api");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String url = assembler.downloadUrl(101L);

        assertThat(url).isEqualTo("https://mango.example.com/api/file/files/download?id=101");
    }

    @Test
    void downloadUrl_noRequestAndNoPublicBaseUrl_fallsBackToRelativePath() {
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(new FileProperties());

        String url = assembler.downloadUrl(102L);

        assertThat(url).isEqualTo("/file/files/download?id=102");
    }

    @Test
    void externalize_publicBaseUrlConfigured_prefixesRelativePathWithoutDuplicatingBasePath() {
        FileProperties properties = new FileProperties();
        properties.setPublicBaseUrl("https://files.example.com/api/");
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(properties);

        String url = assembler.externalize("/api/file/local-objects/local/mango-file/a.txt");

        assertThat(url).isEqualTo("https://files.example.com/api/file/local-objects/local/mango-file/a.txt");
    }

    @Test
    void externalize_forwardedHeadersPresent_prefixesRelativePath() {
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(new FileProperties());
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.header("X-Forwarded-Proto", "https");
        request.header("X-Forwarded-Host", "files.example.com");
        request.header("X-Forwarded-Port", "443");
        request.header("X-Forwarded-Prefix", "/api");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        String url = assembler.externalize("/api/file/local-objects/local/mango-file/a.txt");

        assertThat(url).isEqualTo("https://files.example.com/api/file/local-objects/local/mango-file/a.txt");
    }

    @Test
    void externalize_absoluteUrl_keepsOriginalUrl() {
        FileProperties properties = new FileProperties();
        properties.setPublicBaseUrl("https://files.example.com/api/");
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(properties);

        String url = assembler.externalize("https://object.example.com/bucket/a.txt?token=1");

        assertThat(url).isEqualTo("https://object.example.com/bucket/a.txt?token=1");
    }

    @Test
    void externalize_noExternalBase_keepsRelativePath() {
        FileAccessUrlAssembler assembler = new FileAccessUrlAssembler(new FileProperties());

        String url = assembler.externalize("/api/file/local-objects/local/mango-file/a.txt");

        assertThat(url).isEqualTo("/api/file/local-objects/local/mango-file/a.txt");
    }

    private static final class TestHttpServletRequest extends HttpServletRequestWrapper {

        private final Map<String, String> headers = new HashMap<>();

        private TestHttpServletRequest() {
            super(emptyRequest());
        }

        private void header(String name, String value) {
            headers.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public String getServerName() {
            return "127.0.0.1";
        }

        @Override
        public int getServerPort() {
            return 8080;
        }

        @Override
        public String getContextPath() {
            return "";
        }

        private static HttpServletRequest emptyRequest() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> null);
        }
    }
}
