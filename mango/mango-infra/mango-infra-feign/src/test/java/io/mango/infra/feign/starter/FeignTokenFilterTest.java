package io.mango.infra.feign.starter;

import io.mango.infra.context.api.MangoContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FeignTokenFilterTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void doFilter_authorizationPresent_exposesTokenOnlyInsideRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer current");

        new FeignTokenFilter().doFilter(request, new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) ->
                        assertThat(MangoContextHolder.token()).isEqualTo("Bearer current"));

        assertThat(MangoContextHolder.token()).isNull();
    }

    @Test
    void doFilter_authorizationMissing_hidesStaleThreadToken() throws Exception {
        MangoContextHolder.setToken("Bearer stale");

        new FeignTokenFilter().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> assertThat(MangoContextHolder.token()).isNull());

        assertThat(MangoContextHolder.token()).isNull();
    }
}
