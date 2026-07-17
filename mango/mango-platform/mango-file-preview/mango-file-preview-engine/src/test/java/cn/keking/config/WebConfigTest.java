package cn.keking.config;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class WebConfigTest {

    @Test
    void shouldNormalizeContainerRootPathWithoutTreatingDirectoryAsHost() {
        assumeFalse(File.separatorChar == '\\');

        URI resourceLocation = URI.create(WebConfig.fileResourceLocation("//server/src/main/file/"));

        assertEquals("file", resourceLocation.getScheme());
        assertNull(resourceLocation.getAuthority());
        assertEquals("/server/src/main/file/", resourceLocation.getPath());
    }
}
