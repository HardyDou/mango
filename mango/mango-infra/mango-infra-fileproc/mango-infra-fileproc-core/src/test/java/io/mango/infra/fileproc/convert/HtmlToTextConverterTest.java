package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertToolException;
import io.mango.infra.fileproc.convert.convert.HtmlToTextConverter;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlToTextConverterTest {

    private final HtmlToTextConverter converter = new HtmlToTextConverter();

    @Test
    void supports_onlyHtmlToText() {
        assertThat(converter.supports(ConvertFormat.HTML, ConvertFormat.TEXT)).isTrue();
        assertThat(converter.supports(ConvertFormat.TEXT, ConvertFormat.TEXT)).isFalse();
    }

    @Test
    void convert_stripsMarkupAndRenamesFile() {
        ConvertResultVO result = converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.HTML)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("notice.html")
                .inputStream(new java.io.ByteArrayInputStream("""
                        <html>
                        <head><style>.x{color:red}</style></head>
                        <body><p>Hello&nbsp;<strong>Mango</strong></p><script>alert(1)</script><br>Next</body>
                        </html>
                        """.getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.format()).isEqualTo(ConvertFormat.TEXT);
        assertThat(result.contentType()).isEqualTo(ConvertFormat.TEXT.contentType());
        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("Hello Mango\n\nNext");
    }

    @Test
    void convert_handlesBlankFileNameAndInputFailure() {
        ConvertResultVO result = converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.HTML)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("   ")
                .inputStream(new java.io.ByteArrayInputStream("<p>Hi</p>".getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.fileName()).isNull();

        assertThatThrownBy(() -> converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.HTML)
                .targetFormat(ConvertFormat.TEXT)
                .inputStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom");
                    }
                })
                .build()))
                .isInstanceOf(ConvertToolException.class)
                .hasMessageContaining("HTML 转文本失败");
    }
}
