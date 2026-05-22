package io.mango.infra.tools.doc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight HTML to plain text converter for fallback and tests.
 */
public class HtmlToTextDocumentConverter implements DocumentConverter {

    @Override
    public boolean supports(DocumentFormat sourceFormat, DocumentFormat targetFormat) {
        return sourceFormat == DocumentFormat.HTML && targetFormat == DocumentFormat.TEXT;
    }

    @Override
    public DocumentConvertResult convert(DocumentConvertRequest request) {
        try {
            String html = new String(request.inputStream().readAllBytes(), StandardCharsets.UTF_8);
            String text = html
                    .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                    .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                    .replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?i)</p>", "\n")
                    .replaceAll("<[^>]+>", "")
                    .replace("&nbsp;", " ")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .trim();
            return DocumentConvertResult.builder()
                    .format(DocumentFormat.TEXT)
                    .fileName(resolveFileName(request))
                    .contentType(DocumentFormat.TEXT.contentType())
                    .content(text.getBytes(StandardCharsets.UTF_8))
                    .build();
        } catch (IOException ex) {
            throw new DocumentToolException("Failed to convert HTML to text", ex);
        }
    }

    private String resolveFileName(DocumentConvertRequest request) {
        if (request.fileName() == null || request.fileName().isBlank()) {
            return null;
        }
        String fileName = request.fileName().trim();
        int index = fileName.lastIndexOf('.');
        String baseName = index > 0 ? fileName.substring(0, index) : fileName;
        return baseName + ".txt";
    }
}
