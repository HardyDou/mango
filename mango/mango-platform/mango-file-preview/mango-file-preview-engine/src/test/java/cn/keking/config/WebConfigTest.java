package cn.keking.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UrlPathHelper;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void servesEngineFilesUnderTheSinglePublicPreviewFilePrefix() {
        ExposedRegistry registry = new ExposedRegistry();

        new WebConfig().addResourceHandlers(registry);

        Map<String, ?> urlMap = registry.mapping().getUrlMap();
        assertThat(urlMap).containsKey("/preview-file/**");
        ResourceHttpRequestHandler handler = assertInstanceOf(ResourceHttpRequestHandler.class,
                urlMap.get("/preview-file/**"));
        List<?> locations = handler.getLocations();
        assertThat(locations).hasSize(1);
        assertThat(locations.get(0).toString()).contains(Path.of(ConfigConstants.getFileDir()).toAbsolutePath().normalize().toString());
    }

    @Test
    void keepsRootLevelWildcardsOutOfTheFileDirMapping() {
        ExposedRegistry registry = new ExposedRegistry();

        new WebConfig().addResourceHandlers(registry);

        assertThat(registry.mapping().getUrlMap().keySet())
                .contains("/preview-file/**")
                .doesNotContain("/file-*", "/file-/**", "/**/*.mp4", "/**/*.png");
        assertThat(registry.mapping().getUrlMap().keySet().stream()
                .filter(pattern -> pattern.startsWith("/preview-file"))
                .toList()).containsExactly("/preview-file/**");
    }

    /**
     * 暴露 {@link ResourceHandlerRegistry#getHandlerMapping()}（Spring 侧为 protected）以便断言真实映射表。
     */
    private static final class ExposedRegistry extends ResourceHandlerRegistry {

        private ExposedRegistry() {
            super(new StaticApplicationContext(), new MockServletContext(),
                    new ContentNegotiationManager(), new UrlPathHelper());
        }

        private SimpleUrlHandlerMapping mapping() {
            return assertInstanceOf(SimpleUrlHandlerMapping.class, getHandlerMapping());
        }
    }
}
