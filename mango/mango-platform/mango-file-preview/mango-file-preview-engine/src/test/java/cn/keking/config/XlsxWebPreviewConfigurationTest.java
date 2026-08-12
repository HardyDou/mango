package cn.keking.config;

import cn.keking.utils.ConfigUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XlsxWebPreviewConfigurationTest {

    @AfterEach
    void restoreCompatibilityDefault() {
        ConfigConstants.setOfficeXlsxWebButtonsEnabledValue(true);
    }

    @Test
    void usesCompatibilityDefaultWhenConfigurationIsMissing() throws IOException {
        ConfigRefreshComponent refreshComponent = new ConfigRefreshComponent();
        Properties properties = loadEngineProperties();
        properties.remove("office.xlsx.web.buttons.enabled");

        ReflectionTestUtils.invokeMethod(refreshComponent, "updateConfigConstants", properties);

        assertThat(ConfigConstants.isOfficeXlsxWebButtonsEnabled()).isTrue();
    }

    @Test
    void refreshesTheConfiguredValueAndExposesItToFreeMarker() throws Exception {
        Properties properties = loadEngineProperties();
        properties.setProperty("office.xlsx.web.buttons.enabled", "false");
        ConfigRefreshComponent refreshComponent = new ConfigRefreshComponent();
        ReflectionTestUtils.invokeMethod(refreshComponent, "updateConfigConstants", properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        new cn.keking.web.filter.AttributeSetFilter().doFilter(
                request,
                new MockHttpServletResponse(),
                new MockFilterChain());

        assertThat(ConfigConstants.isOfficeXlsxWebButtonsEnabled()).isFalse();
        assertThat(request.getAttribute("officeXlsxWebButtonsEnabled")).isEqualTo(false);
    }

    private Properties loadEngineProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/mango-file-preview-engine.properties")) {
            assertNotNull(inputStream);
            properties.load(inputStream);
        }
        ConfigUtils.restorePropertiesFromEnvFormat(properties);
        return properties;
    }
}
