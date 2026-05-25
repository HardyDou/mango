package cn.keking;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePreviewMessageTests {

    @Test
    void fileNotSupportedPage_usesMangoMessage() throws IOException {
        String template = readResource("/web/fileNotSupported.ftl");

        assertTrue(template.contains("Mango 文件预览"));
        assertTrue(template.contains("文件暂时无法在线预览"));
        assertTrue(template.contains("请联系管理员检查预览服务配置"));
        assertFalse(template.contains("kk开源社区"));
        assertFalse(template.contains("知识星球"));
    }

    @Test
    void trustPages_doNotExposeKkSupportMessage() throws IOException {
        String notTrustHost = readResource("/web/notTrustHost.html");
        String notTrustDir = readResource("/web/notTrustDir.html");

        assertTrue(notTrustHost.contains("Mango 文件预览"));
        assertTrue(notTrustDir.contains("Mango 文件预览"));
        assertFalse(notTrustHost.contains("kk开源社区"));
        assertFalse(notTrustHost.contains("知识星球"));
        assertFalse(notTrustDir.contains("kk开源社区"));
        assertFalse(notTrustDir.contains("知识星球"));
    }

    @Test
    void tiffFallbackMessage_usesMangoMessage() throws IOException {
        String template = readResource("/web/tiff.ftl");

        assertTrue(template.contains("Mango 当前无法解析该 tif 文件"));
        assertFalse(template.contains("kk开源社区"));
        assertFalse(template.contains("知识星球"));
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
