package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BaseUrlFilter} 的基地址解析测试。
 *
 * <p>预览页面里的脚本、样式与取数接口都以基地址拼接，因此这里锁定三类来源的优先级：
 * 显式 {@code X-Base-Url}、配置项 {@code base.url}、以及按请求（含反向代理转发头）还原。</p>
 */
class BaseUrlFilterTest {

    private final BaseUrlFilter filter = new BaseUrlFilter();

    @AfterEach
    void resetBaseUrl() throws Exception {
        ConfigConstants.setBaseUrlValue(ConfigConstants.DEFAULT_VALUE);
        Field cached = BaseUrlFilter.class.getDeclaredField("BASE_URL");
        cached.setAccessible(true);
        cached.set(null, null);
    }

    @Test
    void resolvesProxyHeadersInsteadOfRewrittenHost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/onlinePreview");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(18001);
        request.addHeader("X-Forwarded-Proto", "http");
        request.addHeader("X-Forwarded-Host", "192.168.1.88:30001");
        request.addHeader("X-Forwarded-Prefix", "/api");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("baseUrl")).isEqualTo("http://192.168.1.88:30001/api/");
    }

    @Test
    void fallsBackToRequestHostWhenNoProxyHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/onlinePreview");
        request.setScheme("http");
        request.setServerName("engine.example.com");
        request.setServerPort(8012);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("baseUrl")).isEqualTo("http://engine.example.com:8012/");
    }

    @Test
    void keepsExplicitBaseUrlHeaderAuthoritative() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/onlinePreview");
        request.setServerName("127.0.0.1");
        request.setServerPort(18001);
        request.addHeader("X-Base-Url", "https://files.example.com/preview");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("baseUrl")).isEqualTo("https://files.example.com/preview/");
    }

    @Test
    void usesConfiguredBaseUrlBeforeRequestDerivation() throws Exception {
        ConfigConstants.setBaseUrlValue("https://cdn.example.com");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/onlinePreview");
        request.setServerName("127.0.0.1");
        request.setServerPort(18001);
        request.addHeader("X-Forwarded-Host", "192.168.1.88:30001");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute("baseUrl")).isEqualTo("https://cdn.example.com/");
    }
}
