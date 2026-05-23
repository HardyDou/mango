package io.mango.template.core.render;

import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import io.mango.infra.fileproc.render.RenderApi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRenderManagerTest {

    @Test
    void convertedTextAndHtmlResultsRemainInlineContent() throws Exception {
        TemplateRenderManager manager = new TemplateRenderManager(nullRenderApi(), nullConvertApi());
        Method method = TemplateRenderManager.class.getDeclaredMethod("output", ConvertResultVO.class);
        method.setAccessible(true);

        TemplateRenderOutput text = (TemplateRenderOutput) method.invoke(manager, ConvertResultVO.builder()
                .format(ConvertFormat.TEXT)
                .content("hello text".getBytes(StandardCharsets.UTF_8))
                .build());
        TemplateRenderOutput html = (TemplateRenderOutput) method.invoke(manager, ConvertResultVO.builder()
                .format(ConvertFormat.HTML)
                .content("<p>hello html</p>".getBytes(StandardCharsets.UTF_8))
                .build());

        assertThat(text.content()).isEqualTo("hello text");
        assertThat(text.fileBytes()).isNull();
        assertThat(html.content()).isEqualTo("<p>hello html</p>");
        assertThat(html.fileBytes()).isNull();
    }

    private RenderApi nullRenderApi() {
        return null;
    }

    private ConvertApi nullConvertApi() {
        return null;
    }
}
