package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;

import io.mango.infra.web.starter.MangoWebProperties;
import io.mango.infra.web.starter.WebAutoConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * CORS 暴露头测试。
 *
 * <p>格兰特令牌通过响应头回传，跨域场景下浏览器只暴露白名单内的响应头，因此三个 Grant 头必须出现在
 * {@code Access-Control-Expose-Headers} 中，否则前端读不到、List 页面无法把 Grant 接力到详情页。</p>
 */
class DataGrantCorsExposureTest {

    @Test
    void dataGrantAutoConfigurationExposesHeadersOnWebProperties() {
        MangoWebProperties webProperties = new MangoWebProperties();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("mangoWebProperties", webProperties);

        new DataGrantAutoConfiguration(GrantTestSupport.properties(),
                beanFactory.getBeanProvider(MangoWebProperties.class));

        assertThat(webProperties.getCors().getExposedHeaders())
                .containsExactlyElementsOf(DataGrantHeaders.EXPOSED_HEADERS);
    }

    @Test
    void autoConfigurationExposureIsIdempotent() {
        MangoWebProperties webProperties = new MangoWebProperties();
        webProperties.getCors().setExposedHeaders(new ArrayList<>(List.of("X-Trace-Id")));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("mangoWebProperties", webProperties);

        new DataGrantAutoConfiguration(GrantTestSupport.properties(),
                beanFactory.getBeanProvider(MangoWebProperties.class));
        new DataGrantAutoConfiguration(GrantTestSupport.properties(),
                beanFactory.getBeanProvider(MangoWebProperties.class));

        assertThat(webProperties.getCors().getExposedHeaders())
                .containsExactly("X-Trace-Id",
                        DataGrantHeaders.GRANT,
                        DataGrantHeaders.REQUIRED,
                        DataGrantHeaders.SKIPPED);
    }

    @Test
    void autoConfigurationToleratesMissingWebProperties() {
        MangoWebProperties webProperties = new MangoWebProperties();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        new DataGrantAutoConfiguration(GrantTestSupport.properties(),
                beanFactory.getBeanProvider(MangoWebProperties.class));

        assertThat(webProperties.getCors().getExposedHeaders()).isEmpty();
    }


    @Test
    void autoConfigurationAllowsGrantRequestHeaderOnExplicitConfiguration() {
        MangoWebProperties webProperties = new MangoWebProperties();
        webProperties.getCors().setAllowedHeaders(new ArrayList<>(List.of("Content-Type", "Authorization")));
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("webProperties", webProperties);

        new DataGrantAutoConfiguration(GrantTestSupport.properties(),
                beanFactory.getBeanProvider(MangoWebProperties.class));

        assertThat(webProperties.getCors().getAllowedHeaders())
                .contains("Content-Type", "Authorization", DataGrantHeaders.GRANT);
    }

    @Test
    void springMvcCorsMappingExposesDataGrantHeaders() {
        MangoWebProperties properties = new MangoWebProperties();
        DataGrantHeaders.expose(properties.getCors().getExposedHeaders());

        CapturingCorsRegistry registry = new CapturingCorsRegistry();
        new WebAutoConfiguration(properties).addCorsMappings(registry);

        CorsConfiguration configuration = registry.getCorsConfigurations().get("/**");
        assertThat(configuration).isNotNull();
        assertThat(configuration.getExposedHeaders())
                .contains(DataGrantHeaders.GRANT, DataGrantHeaders.REQUIRED, DataGrantHeaders.SKIPPED);
    }

    @Test
    void springMvcCorsMappingKeepsCustomExposedHeaders() {
        MangoWebProperties properties = new MangoWebProperties();
        properties.getCors().setExposedHeaders(new ArrayList<>(List.of("X-Trace-Id")));
        DataGrantHeaders.expose(properties.getCors().getExposedHeaders());

        CapturingCorsRegistry registry = new CapturingCorsRegistry();
        new WebAutoConfiguration(properties).addCorsMappings(registry);

        assertThat(registry.getCorsConfigurations().get("/**").getExposedHeaders())
                .containsExactlyInAnyOrder("X-Trace-Id",
                        DataGrantHeaders.GRANT,
                        DataGrantHeaders.REQUIRED,
                        DataGrantHeaders.SKIPPED);
    }

    @Test
    void corsFilterEmitsExposeHeadersToBrowser() throws Exception {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("https://app.mango.test"));
        configuration.addAllowedMethod("GET");
        List<String> exposedHeaders = new ArrayList<>();
        DataGrantHeaders.expose(exposedHeaders);
        configuration.setExposedHeaders(exposedHeaders);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/fixture/items/page");
        request.addHeader(HttpHeaders.ORIGIN, "https://app.mango.test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CorsFilter(source).doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .contains(DataGrantHeaders.GRANT)
                .contains(DataGrantHeaders.REQUIRED)
                .contains(DataGrantHeaders.SKIPPED);
    }


    @Test
    void allowsGrantRequestHeaderWhenAllowedHeadersAreExplicit() {
        List<String> allowed = new ArrayList<>(List.of("Content-Type", "Authorization"));

        DataGrantHeaders.allowRequestHeader(allowed);

        assertThat(allowed).contains(DataGrantHeaders.GRANT);
    }

    @Test
    void allowedRequestHeaderKeepsWildcardUnchanged() {
        List<String> allowed = new ArrayList<>(List.of("*"));

        DataGrantHeaders.allowRequestHeader(allowed);

        assertThat(allowed).containsExactly("*");
    }

    @Test
    void allowedRequestHeaderIgnoresDuplicateAndNullList() {
        List<String> allowed = new ArrayList<>(List.of("x-mango-data-grant"));

        DataGrantHeaders.allowRequestHeader(allowed);
        DataGrantHeaders.allowRequestHeader(null);

        assertThat(allowed).containsExactly("x-mango-data-grant");
    }

    @Test
    void exposeIgnoresDuplicateHeaders() {
        List<String> headers = new ArrayList<>(List.of("x-mango-data-grant"));

        DataGrantHeaders.expose(headers);

        assertThat(headers).containsExactly("x-mango-data-grant",
                DataGrantHeaders.REQUIRED,
                DataGrantHeaders.SKIPPED);
    }

    /**
     * 暴露受保护的 {@code getCorsConfigurations()} 以便断言 Map 结果。
     */
    private static final class CapturingCorsRegistry extends CorsRegistry {

        @Override
        protected Map<String, CorsConfiguration> getCorsConfigurations() {
            return super.getCorsConfigurations();
        }
    }
}
