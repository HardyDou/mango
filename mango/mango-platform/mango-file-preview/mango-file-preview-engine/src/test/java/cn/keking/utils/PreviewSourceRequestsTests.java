package cn.keking.utils;

import cn.keking.spi.FileDownloader;
import cn.keking.spi.FileDownloaders;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import jakarta.servlet.ServletRequest;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewSourceRequestsTests {

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

    @AfterEach
    void tearDown() {
        FileDownloaders.unregister(downloader);
    }

    @Test
    void treatsFileIdAsInternalSourceWhenDownloaderRegistered() {
        FileDownloaders.register(downloader);

        assertTrue(PreviewSourceRequests.isInternalSource(request("fileId", "token-1")));
    }

    @Test
    void ignoresFileIdWhenNoDownloaderRegistered() {
        assertFalse(PreviewSourceRequests.isInternalSource(request("fileId", "token-1")));
    }

    @Test
    void ignoresRequestsWithoutFileId() {
        FileDownloaders.register(downloader);

        assertFalse(PreviewSourceRequests.isInternalSource(request("url", "http://127.0.0.1:1/a.docx")));
        assertFalse(PreviewSourceRequests.isInternalSource(request("fileId", "  ")));
    }

    private ServletRequest request(String name, String value) {
        return (ServletRequest) Proxy.newProxyInstance(
                ServletRequest.class.getClassLoader(),
                new Class<?>[] {ServletRequest.class},
                (proxy, method, args) -> "getParameter".equals(method.getName()) && args != null
                        && name.equals(args[0])
                        ? value
                        : null);
    }
}
