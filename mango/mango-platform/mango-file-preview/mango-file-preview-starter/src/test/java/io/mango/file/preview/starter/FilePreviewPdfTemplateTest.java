package io.mango.file.preview.starter;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Office/PDF 转换产物页依赖 source token 才能走 {@code /file-preview/generated}；
 * 进程内直取模式下预览地址只带 {@code fileId}，模板必须从该参数取票据，否则转换产物会回落到
 * 受保护的 {@code /pdf/**} 而被鉴权拦下。
 */
class FilePreviewPdfTemplateTest {

    @Test
    void pdfTemplateTakesSourceTokenFromFileIdWhenUrlIsAbsent() throws Exception {
        String html = render();

        assertThat(html).contains("params.get('fileId')");
        assertThat(html).contains("file-preview/generated?token=");
    }

    private String render() throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "web");
        configuration.setDefaultEncoding("UTF-8");
        Template template = configuration.getTemplate("pdf.ftl");
        Map<String, Object> model = new HashMap<>();
        model.put("sourceUrl", "/pdf/file-1-demo.pdf");
        model.put("finalUrl", "/pdf/file-1-demo.pdf");
        model.put("baseUrl", "http://127.0.0.1:18012/");
        model.put("kkResourceBaseUrl", "/");
        model.put("kkagent", "false");
        model.put("kkkey", "");
        model.put("page", "");
        model.put("watermarkTxt", "");
        model.put("highlightall", "");
        model.put("pdfPresentationModeDisable", "false");
        model.put("pdfOpenFileDisable", "false");
        model.put("pdfPrintDisable", "false");
        model.put("pdfDownloadDisable", "false");
        model.put("pdfDisableEditing", "false");
        model.put("pdfBookmarkDisable", "false");
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }
}
