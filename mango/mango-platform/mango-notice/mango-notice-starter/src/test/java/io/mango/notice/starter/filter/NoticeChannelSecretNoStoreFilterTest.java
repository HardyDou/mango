package io.mango.notice.starter.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class NoticeChannelSecretNoStoreFilterTest {

    @Test
    void revealResponseIsNeverCacheable() throws Exception {
        NoticeChannelSecretNoStoreFilter filter = new NoticeChannelSecretNoStoreFilter();
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/notice/channels/secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, max-age=0");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void unrelatedNoticeResponsesAreNotModified() throws Exception {
        NoticeChannelSecretNoStoreFilter filter = new NoticeChannelSecretNoStoreFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notice/channels");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {});

        assertThat(response.getHeader("Cache-Control")).isNull();
    }
}
