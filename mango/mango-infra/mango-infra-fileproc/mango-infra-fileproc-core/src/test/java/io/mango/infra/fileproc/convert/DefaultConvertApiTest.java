package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertRegistry;
import io.mango.infra.fileproc.convert.convert.ConvertToolException;
import io.mango.infra.fileproc.convert.convert.DefaultConvertApi;
import io.mango.infra.fileproc.convert.convert.HtmlToTextConverter;
import io.mango.infra.fileproc.convert.convert.IConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertFormatPairVO;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultConvertApiTest {

    private final ConvertApi convertApi = new DefaultConvertApi(
            new ConvertRegistry(List.of(
                    new HtmlToTextConverter(),
                    new IConvertProvider() {
                        @Override
                        public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
                            return sourceFormat == ConvertFormat.DOCX && targetFormat == ConvertFormat.PDF;
                        }

                        @Override
                        public ConvertResultVO convert(ConvertCommand request) {
                            return ConvertResultVO.builder()
                                    .format(ConvertFormat.PDF)
                                    .fileName("converted.pdf")
                                    .content("pdf".getBytes(StandardCharsets.UTF_8))
                                    .build();
                        }
                    })));

    @Test
    void convert_withSameFormat_copiesContent() {
        ConvertResultVO result = convertApi.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TEXT)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("notice")
                .inputStream(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.TEXT);
        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(result.contentType()).isEqualTo("text/plain");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void convert_withHtmlToText_removesTagsAndEntities() {
        ConvertResultVO result = convertApi.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.HTML)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("notice.html")
                .inputStream(new ByteArrayInputStream("""
                        <html>
                        <head><style>.x{color:red}</style></head>
                        <body><p>Hello&nbsp;<strong>Mango</strong></p><script>alert(1)</script></body>
                        </html>
                        """.getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("Hello Mango");
    }

    @Test
    void convert_withoutConverter_throwsUnsupportedError() {
        assertThatThrownBy(() -> convertApi.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.PDF)
                .targetFormat(ConvertFormat.DOCX)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build()))
                .isInstanceOf(ConvertToolException.class)
                .hasMessageContaining("不支持的格式转换");
    }

    @Test
    void canConvert_handlesNullsSameFormatAndRegisteredConverters() {
        assertThat(convertApi.canConvert(null, ConvertFormat.TEXT)).isFalse();
        assertThat(convertApi.canConvert(ConvertFormat.HTML, null)).isFalse();
        assertThat(convertApi.canConvert(ConvertFormat.TEXT, ConvertFormat.TEXT)).isTrue();
        assertThat(convertApi.canConvert(ConvertFormat.HTML, ConvertFormat.TEXT)).isTrue();
        assertThat(convertApi.canConvert(ConvertFormat.DOCX, ConvertFormat.PDF)).isTrue();
        assertThat(convertApi.canConvert(ConvertFormat.PDF, ConvertFormat.DOCX)).isFalse();
    }

    @Test
    void supportedConversions_listsOnlyDistinctSupportedPairs() {
        assertThat(convertApi.supportedConversions())
                .containsExactly(
                        new ConvertFormatPairVO(ConvertFormat.HTML, ConvertFormat.TEXT),
                        new ConvertFormatPairVO(ConvertFormat.DOCX, ConvertFormat.PDF));
    }

    @Test
    void convert_withRegisteredConverterUsesMatchingImplementation() {
        ConvertResultVO result = convertApi.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.DOCX)
                .targetFormat(ConvertFormat.PDF)
                .fileName("report.docx")
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.fileName()).isEqualTo("converted.pdf");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("pdf");
    }

    @Test
    void convert_whenInputStreamFails_propagatesConvertToolExceptionFromSameFormatConverter() {
        assertThatThrownBy(() -> convertApi.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TEXT)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("notice")
                .inputStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom");
                    }
                })
                .build()))
                .isInstanceOf(ConvertToolException.class)
                .hasMessageContaining("同格式内容复制失败")
                .hasRootCauseInstanceOf(IOException.class);
    }
}
