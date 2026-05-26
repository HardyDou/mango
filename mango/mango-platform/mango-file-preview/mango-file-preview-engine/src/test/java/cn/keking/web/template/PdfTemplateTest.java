package cn.keking.web.template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTemplateTest {

    @Test
    void usesGatewayOriginForPdfJsWhenPreviewPageIsServedThroughApiGateway() throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "web");
        Template template = configuration.getTemplate("pdf.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("pdfUrl", "http://127.0.0.1:5581/api/compressed-file?kkCompressfileKey=a.zip_&kkCompressfilepath=a.zip_%2Fsample.pdf&fullfilename=sample.pdf");
        model.put("baseUrl", "http://127.0.0.1:7781/");
        model.put("kkResourceBaseUrl", "http://127.0.0.1:7781/");
        model.put("kkagent", "false");
        model.put("kkkey", "false");
        model.put("switchDisabled", "true");
        model.put("watermarkTxt", "");
        model.put("highlightall", "false");
        model.put("page", "0");
        model.put("pdfPresentationModeDisable", "true");
        model.put("pdfOpenFileDisable", "true");
        model.put("pdfPrintDisable", "true");
        model.put("pdfDownloadDisable", "true");
        model.put("pdfBookmarkDisable", "true");
        model.put("pdfDisableEditing", "false");
        model.put("pdfSidebarOpen", "false");

        StringWriter output = new StringWriter();
        template.process(model, output);

        assertThat(output.toString())
                .contains("window.location.origin + \"/api/\"")
                .contains("var viewerUrl = baseUrl + \"pdfjs/web/viewer.html?file=\"");
    }

    @Test
    void includesPdfJsBuildResourcesRequiredByViewerHtml() {
        assertThat(new ClassPathResource("static/pdfjs/build/pdf.mjs").exists()).isTrue();
        assertThat(new ClassPathResource("static/pdfjs/build/pdf.worker.mjs").exists()).isTrue();
        assertThat(new ClassPathResource("static/pdfjs/build/pdf.sandbox.mjs").exists()).isTrue();
    }
}
