package io.mango.infra.feign.starter;

import feign.RequestTemplate;
import io.mango.infra.context.api.MangoContextHeaders;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeignRequestInterceptorTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void apply_fullRuntimeContext_propagatesEverySupportedHeader() {
        MangoContextHolder.set(new MangoContextSnapshot(
                "request-1", "trace-1", "tenant-1", 101L, 202L,
                "principal-1", "admin", "USER", "TENANT", 303L,
                "admin-app", "127.0.0.1"));
        MangoContextHolder.setToken("Bearer token-1");
        RequestTemplate template = new RequestTemplate();

        new FeignRequestInterceptor().apply(template);

        assertThat(firstHeader(template, "Authorization")).isEqualTo("Bearer token-1");
        assertThat(firstHeader(template, MangoContextHeaders.REQUEST_ID)).isEqualTo("request-1");
        assertThat(firstHeader(template, MangoContextHeaders.TRACE_ID)).isEqualTo("trace-1");
        assertThat(firstHeader(template, MangoContextHeaders.TENANT_ID)).isEqualTo("tenant-1");
        assertThat(firstHeader(template, MangoContextHeaders.USER_ID)).isEqualTo("101");
        assertThat(firstHeader(template, MangoContextHeaders.MEMBER_ID)).isEqualTo("202");
        assertThat(firstHeader(template, MangoContextHeaders.PRINCIPAL_NAME)).isEqualTo("principal-1");
        assertThat(firstHeader(template, MangoContextHeaders.REALM)).isEqualTo("admin");
        assertThat(firstHeader(template, MangoContextHeaders.ACTOR_TYPE)).isEqualTo("USER");
        assertThat(firstHeader(template, MangoContextHeaders.PARTY_TYPE)).isEqualTo("TENANT");
        assertThat(firstHeader(template, MangoContextHeaders.PARTY_ID)).isEqualTo("303");
        assertThat(firstHeader(template, MangoContextHeaders.APP_CODE)).isEqualTo("admin-app");
        assertThat(firstHeader(template, MangoContextHeaders.CLIENT_IP)).isEqualTo("127.0.0.1");
    }

    @Test
    void apply_emptyRuntimeContext_omitsOptionalHeaders() {
        RequestTemplate template = new RequestTemplate();

        new FeignRequestInterceptor().apply(template);

        assertThat(template.headers()).isEmpty();
    }

    private String firstHeader(RequestTemplate template, String name) {
        if (!template.headers().containsKey(name)) {
            return null;
        }
        return template.headers().get(name).iterator().next();
    }
}
