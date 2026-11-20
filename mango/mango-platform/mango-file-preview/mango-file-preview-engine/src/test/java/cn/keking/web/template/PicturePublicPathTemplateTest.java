package cn.keking.web.template;

import cn.keking.service.FileHandlerService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图片页的子资源拼装：同源相对地址只补站点前缀，不重复补斜杠；绝对地址原样保留。
 */
class PicturePublicPathTemplateTest {

    @Test
    void resolvesPublicPreviewFilePrefixAgainstBaseUrl() throws Exception {
        String output = render(List.of(FileHandlerService.PUBLIC_FILE_PATH_PREFIX + "/demo.png"));

        assertThat(output).contains("data-original-url=\"http://127.0.0.1:8080/preview-file/demo.png\"");
        assertThat(output).doesNotContain("8080//preview-file");
    }

    @Test
    void keepsAbsoluteImageAddressUntouched() throws Exception {
        String output = render(List.of("https://cdn.example.com/a.png"));

        assertThat(output).contains("data-original-url=\"https://cdn.example.com/a.png\"");
        assertThat(output).doesNotContain("8080/https://cdn.example.com/a.png");
    }

    private String render(List<String> imgUrls) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "web");
        Template template = configuration.getTemplate("picture.ftl");

        Map<String, Object> model = new HashMap<>();
        model.put("imgUrls", imgUrls);
        model.put("baseUrl", "http://127.0.0.1:8080/");
        model.put("kkagent", "false");
        model.put("watermarkTxt", "");

        StringWriter output = new StringWriter();
        template.process(model, output);
        return output.toString();
    }
}
