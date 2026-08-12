package cn.keking.web.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class XlsxWebPreviewTemplateTest {

    @Test
    void hidesExternalButtonsAndFillsLuckysheetWhenDisabled() throws Exception {
        String output = render(false);

        assertThat(output)
                .doesNotContain("跳转HTML预览")
                .doesNotContain("id=\"confirm-button\"")
                .doesNotContain("id=\"button-area\"")
                .contains("top: 0px;");
    }

    @Test
    void keepsExternalButtonsAndCurrentSpacingWhenEnabled() throws Exception {
        String output = render(true);

        assertThat(output)
                .contains("跳转HTML预览")
                .contains("onclick=\"tiaozhuan()\"")
                .contains("id=\"confirm-button\"")
                .contains("onclick=\"print()\"")
                .contains("id=\"button-area\"")
                .contains("top: 20px;");
    }

    @Test
    void keepsTheSwitchScopedToTheXlsxWebTemplate() throws IOException {
        String officeWebTemplate = readResource("/web/officeweb.ftl");

        assertThat(officeWebTemplate).contains("officeXlsxWebButtonsEnabled");
        assertThat(readResource("/web/pdf.ftl")).doesNotContain("officeXlsxWebButtonsEnabled");
        assertThat(readResource("/web/picture.ftl")).doesNotContain("officeXlsxWebButtonsEnabled");
        assertThat(readResource("/web/media.ftl")).doesNotContain("officeXlsxWebButtonsEnabled");
        assertThat(readResource("/web/tiff.ftl")).doesNotContain("officeXlsxWebButtonsEnabled");
    }

    @Test
    void documentsTheMangoAndKkConfigurationContract() throws IOException {
        String properties = readResource("/mango-file-preview-engine.properties");
        String testProperties = readResource("/test.properties");

        String expectedConfiguration =
                "office.xlsx.web.buttons.enabled = ${KK_OFFICE_XLSX_WEB_BUTTONS_ENABLED:true}";
        assertThat(properties).contains(expectedConfiguration);
        assertThat(testProperties).contains(expectedConfiguration);
    }

    private String render(boolean buttonsEnabled) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "web");
        Template template = configuration.getTemplate("officeweb.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("officeXlsxWebButtonsEnabled", buttonsEnabled);
        model.put("pdfUrl", "file.xlsx");
        model.put("baseUrl", "/");
        model.put("kkResourceBaseUrl", "/");
        model.put("kkagent", "false");
        model.put("kkkey", "false");
        model.put("file", Map.of("name", "sample.xlsx"));
        model.put("xlsxshowtoolbar", false);
        model.put("xlsxallowEdit", false);
        model.put("watermarkTxt", "");

        StringWriter output = new StringWriter();
        template.process(model, output);
        return output.toString();
    }

    private String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
