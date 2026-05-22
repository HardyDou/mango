package io.mango.template.core.render;

import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryTemplateRendererTest {

    private final PlaceholderTemplateEngine engine = new PlaceholderTemplateEngine();

    @Test
    void renderDocxReplacesPlaceholdersInsideXmlEntries() throws Exception {
        BinaryTemplateRenderer renderer = new BinaryTemplateRenderer(engine, TemplateSourceFormat.DOCX);
        byte[] source = zipBytes(Map.of(
            "word/document.xml", "<w:t>客户：{{ customer.name }}</w:t>",
            "docProps/core.xml", "<dc:title>{{title}}</dc:title>"
        ));

        TemplateRenderOutput output = renderer.render(new TemplateRenderPayload(
            TemplateSourceFormat.DOCX,
            TemplateOutputFormat.DOCX,
            null,
            source,
            "contract.docx",
            Map.of("customer", Map.of("name", "张三"), "title", "合同")
        ));

        assertThat(output.fileName()).isEqualTo("contract-rendered.docx");
        assertThat(unzipText(output.fileBytes(), "word/document.xml")).contains("客户：张三");
        assertThat(unzipText(output.fileBytes(), "docProps/core.xml")).contains("<dc:title>合同</dc:title>");
    }

    @Test
    void renderXlsxEscapesXmlValues() throws Exception {
        BinaryTemplateRenderer renderer = new BinaryTemplateRenderer(engine, TemplateSourceFormat.XLSX);
        byte[] source = zipBytes(Map.of(
            "xl/sharedStrings.xml", "<t>{{company}}</t>",
            "xl/worksheets/sheet1.xml", "<v>1</v>"
        ));

        TemplateRenderOutput output = renderer.render(new TemplateRenderPayload(
            TemplateSourceFormat.XLSX,
            TemplateOutputFormat.XLSX,
            null,
            source,
            "report.xlsx",
            Map.of("company", "A&B<公司>")
        ));

        assertThat(output.fileName()).isEqualTo("report-rendered.xlsx");
        assertThat(unzipText(output.fileBytes(), "xl/sharedStrings.xml")).contains("A&amp;B&lt;公司&gt;");
    }

    @Test
    void extractVariablesReadsOoxmlXmlEntries() throws Exception {
        BinaryTemplateRenderer renderer = new BinaryTemplateRenderer(engine, TemplateSourceFormat.DOCX);
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("word/document.xml", "<w:t>{{customer.name}}</w:t>");
        entries.put("word/footer.xml", "<w:t>{{amount}}</w:t>");
        byte[] source = zipBytes(entries);

        assertThat(renderer.extractVariables(new TemplateRenderPayload(
            TemplateSourceFormat.DOCX,
            TemplateOutputFormat.DOCX,
            null,
            source,
            "contract.docx",
            Map.of()
        ))).containsExactly("customer.name", "amount");
    }

    private byte[] zipBytes(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zipOutput.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutput.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zipOutput.closeEntry();
            }
        }
        return output.toByteArray();
    }

    private String unzipText(byte[] bytes, String name) throws Exception {
        try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return new String(zipInput.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }
}
