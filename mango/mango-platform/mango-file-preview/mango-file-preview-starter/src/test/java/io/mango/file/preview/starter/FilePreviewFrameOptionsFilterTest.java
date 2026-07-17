package io.mango.file.preview.starter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FilePreviewFrameOptionsFilterTest {

    @Test
    void generatedOfficePdf_keepsSameOriginFramePolicy() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/static/file-preview/file-100docx.pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ignoredRequest, wrappedResponse) ->
                ((jakarta.servlet.http.HttpServletResponse) wrappedResponse)
                        .setHeader("X-Frame-Options", "DENY");

        new FilePreviewFrameOptionsFilter().doFilter(request, response, chain);

        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }
}
