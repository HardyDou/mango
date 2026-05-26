package io.mango.file.preview.starter;

import io.mango.file.preview.core.config.FilePreviewProperties;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewStandaloneUiBlockFilterTest {

    @Test
    void blocksStandaloneUiPathsByDefault() throws ServletException, IOException {
        MockHttpServletResponse response = filter("/index");

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("\"code\":404");
    }

    @Test
    void keepsPreviewAndArchiveDirectoryPathsAvailable() throws ServletException, IOException {
        MockFilterChain chain = new MockFilterChain();
        FilePreviewStandaloneUiBlockFilter filter = new FilePreviewStandaloneUiBlockFilter(new FilePreviewProperties());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/onlinePreview"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/directory"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);

        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("GET", "/compressed-file"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsStandaloneUiWhenExplicitlyEnabled() throws ServletException, IOException {
        FilePreviewProperties properties = new FilePreviewProperties();
        properties.setStandaloneUiEnabled(true);
        FilePreviewStandaloneUiBlockFilter filter = new FilePreviewStandaloneUiBlockFilter(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/"), response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletResponse filter(String path) throws ServletException, IOException {
        FilePreviewStandaloneUiBlockFilter filter = new FilePreviewStandaloneUiBlockFilter(new FilePreviewProperties());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", path), response, new MockFilterChain());
        return response;
    }
}
