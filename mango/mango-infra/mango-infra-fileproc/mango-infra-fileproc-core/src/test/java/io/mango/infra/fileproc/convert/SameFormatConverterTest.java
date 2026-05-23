package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertToolException;
import io.mango.infra.fileproc.convert.convert.SameFormatConverter;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SameFormatConverterTest {

    private final SameFormatConverter converter = new SameFormatConverter();

    @Test
    void supports_onlySameFormatPairs() {
        assertThat(converter.supports(ConvertFormat.TEXT, ConvertFormat.TEXT)).isTrue();
        assertThat(converter.supports(ConvertFormat.HTML, ConvertFormat.TEXT)).isFalse();
        assertThat(converter.supports(null, ConvertFormat.TEXT)).isFalse();
    }

    @Test
    void convert_copiesBytesAndAppendsMissingExtension() {
        ConvertResultVO result = converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TEXT)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("notice")
                .inputStream(new java.io.ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(result.contentType()).isEqualTo(ConvertFormat.TEXT.contentType());
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void convert_preservesExistingExtensionAndPropagatesReadFailure() {
        ConvertResultVO result = converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TEXT)
                .targetFormat(ConvertFormat.TEXT)
                .fileName("report.txt")
                .inputStream(new java.io.ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.fileName()).isEqualTo("report.txt");

        assertThatThrownBy(() -> converter.convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.TEXT)
                .targetFormat(ConvertFormat.TEXT)
                .inputStream(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("boom");
                    }
                })
                .build()))
                .isInstanceOf(ConvertToolException.class)
                .hasMessageContaining("同格式内容复制失败");
    }
}
