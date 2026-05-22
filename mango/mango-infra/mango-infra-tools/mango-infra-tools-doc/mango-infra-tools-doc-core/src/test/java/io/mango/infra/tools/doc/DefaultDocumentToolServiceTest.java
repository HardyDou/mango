package io.mango.infra.tools.doc;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDocumentToolServiceTest {

    private final DocumentToolService documentToolService = new DefaultDocumentToolService(
            new DocumentToolRegistry(List.of(new HtmlToTextDocumentConverter())));

    @Test
    void convert_withSameFormat_copiesContent() {
        DocumentConvertResult result = documentToolService.convert(DocumentConvertRequest.builder()
                .sourceFormat(DocumentFormat.TEXT)
                .targetFormat(DocumentFormat.TEXT)
                .fileName("notice")
                .inputStream(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)))
                .build());

        assertThat(result.format()).isEqualTo(DocumentFormat.TEXT);
        assertThat(result.fileName()).isEqualTo("notice.txt");
        assertThat(result.contentType()).isEqualTo("text/plain");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    void convert_withHtmlToText_removesTagsAndEntities() {
        DocumentConvertResult result = documentToolService.convert(DocumentConvertRequest.builder()
                .sourceFormat(DocumentFormat.HTML)
                .targetFormat(DocumentFormat.TEXT)
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
        assertThatThrownBy(() -> documentToolService.convert(DocumentConvertRequest.builder()
                .sourceFormat(DocumentFormat.DOCX)
                .targetFormat(DocumentFormat.PDF)
                .inputStream(new ByteArrayInputStream(new byte[0]))
                .build()))
                .isInstanceOf(DocumentToolException.class)
                .hasMessageContaining("Unsupported document conversion");
    }
}
