package io.mango.infra.fileproc.render;

import io.mango.common.exception.BizException;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.service.DefaultRenderApi;
import io.mango.infra.fileproc.render.service.HtmlToTextRenderProvider;
import io.mango.infra.fileproc.render.service.RenderRegistry;
import io.mango.infra.fileproc.render.service.RenderToolException;
import io.mango.infra.fileproc.render.service.SameFormatRenderProvider;
import io.mango.infra.fileproc.render.service.UnsupportedRenderService;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentRenderContractTest {

    @Test
    void renderFormatParsesNameAndExtension() {
        assertThat(RenderFormat.parse("html")).contains(RenderFormat.HTML);
        assertThat(RenderFormat.parse("txt")).contains(RenderFormat.TEXT);
        assertThat(RenderFormat.parse("jpg")).contains(RenderFormat.JPEG);
        assertThat(RenderFormat.parse("unknown")).isEmpty();
    }

    @Test
    void renderCommandRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> RenderCommand.builder()
                .targetFormat(RenderFormat.TEXT)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build())
                .isInstanceOf(BizException.class)
                .hasMessage("源格式不能为空");

        assertThatThrownBy(() -> RenderCommand.builder()
                .sourceFormat(RenderFormat.HTML)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build())
                .isInstanceOf(BizException.class)
                .hasMessage("目标格式不能为空");

        assertThatThrownBy(() -> RenderCommand.builder()
                .sourceFormat(RenderFormat.HTML)
                .targetFormat(RenderFormat.TEXT)
                .build())
                .isInstanceOf(BizException.class)
                .hasMessage("渲染输入流不能为空");
    }

    @Test
    void resultDefensivelyCopiesContent() {
        byte[] content = {1, 2, 3};
        RenderResultVO result = RenderResultVO.builder()
                .format(RenderFormat.TEXT)
                .content(content)
                .build();

        content[0] = 9;
        assertThat(result.content()).containsExactly(1, 2, 3);

        byte[] exported = result.content();
        exported[1] = 8;
        assertThat(result.content()).containsExactly(1, 2, 3);
    }

    @Test
    void sameFormatRenderCopiesContentAndKeepsExtension() {
        DefaultRenderApi renderApi = newRenderApi();

        RenderResultVO result = renderApi.render(RenderCommand.builder()
                .sourceFormat(RenderFormat.DOCX)
                .targetFormat(RenderFormat.DOCX)
                .fileName("contract")
                .inputStream(new ByteArrayInputStream(new byte[] {1, 2, 3}))
                .build());

        assertThat(result.format()).isEqualTo(RenderFormat.DOCX);
        assertThat(result.fileName()).isEqualTo("contract.docx");
        assertThat(result.contentType()).isEqualTo(RenderFormat.DOCX.contentType());
        assertThat(result.content()).containsExactly(1, 2, 3);
    }

    @Test
    void htmlToTextRenderRemovesTagsAndEntities() {
        DefaultRenderApi renderApi = newRenderApi();

        RenderResultVO result = renderApi.render(RenderCommand.builder()
                .sourceFormat(RenderFormat.HTML)
                .targetFormat(RenderFormat.TEXT)
                .fileName("notice.html")
                .inputStream(new ByteArrayInputStream(("""
                        <style>.hidden{display:none}</style>
                        <script>alert(1)</script>
                        <p>Hello&nbsp;<strong>Mango</strong></p>
                        """).getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.format()).isEqualTo(RenderFormat.TEXT);
        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("Hello Mango");
    }

    @Test
    void defaultRenderApiExposesSupportedRenderingsWithoutSameFormatPairs() {
        DefaultRenderApi renderApi = newRenderApi();

        assertThat(renderApi.canRender(RenderFormat.HTML, RenderFormat.TEXT)).isTrue();
        assertThat(renderApi.canRender(RenderFormat.PDF, RenderFormat.TEXT)).isFalse();
        assertThat(renderApi.supportedRenderings())
                .extracting(pair -> pair.sourceFormat() + "->" + pair.targetFormat())
                .containsExactly("HTML->TEXT");
    }

    @Test
    void unsupportedRenderThrowsExplicitException() {
        DefaultRenderApi renderApi = new DefaultRenderApi(new RenderRegistry(List.of()),
                new UnsupportedRenderService());

        assertThatThrownBy(() -> renderApi.render(RenderCommand.builder()
                .sourceFormat(RenderFormat.PDF)
                .targetFormat(RenderFormat.TEXT)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build()))
                .isInstanceOf(RenderToolException.class)
                .hasMessage("不支持的文档渲染：PDF -> TEXT");
    }

    private DefaultRenderApi newRenderApi() {
        return new DefaultRenderApi(new RenderRegistry(List.of(
                new SameFormatRenderProvider(),
                new HtmlToTextRenderProvider())), new UnsupportedRenderService());
    }
}
