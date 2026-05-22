package io.mango.template.core.render;

import io.mango.template.api.command.TemplateVariableDefinition;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocxTemplateRendererTest {

    private final DocxTemplateRenderer renderer = new DocxTemplateRenderer(new PlaceholderTemplateEngine());

    @Test
    void renderReplacesVariablesWithPoiTl() throws Exception {
        byte[] source = docx("客户：{{customer.name}}，金额：{{amount}}");

        TemplateRenderOutput output = renderer.render(new TemplateRenderPayload(
                TemplateSourceFormat.DOCX,
                TemplateOutputFormat.DOCX,
                null,
                source,
                "contract.docx",
                Map.of("customer", Map.of("name", "张三"), "amount", 128),
                List.of()
        ));

        assertThat(output.fileName()).isEqualTo("contract-rendered.docx");
        assertThat(readDocxText(output.fileBytes())).contains("客户：张三，金额：128");
    }

    @Test
    void extractVariablesReadsDocxXmlEntries() throws Exception {
        byte[] source = docx("客户：{{customer.name}}，金额：{{amount}}");

        assertThat(renderer.extractVariables(new TemplateRenderPayload(
                TemplateSourceFormat.DOCX,
                TemplateOutputFormat.DOCX,
                null,
                source,
                "contract.docx",
                Map.of(),
                List.of()
        ))).containsExactly("customer.name", "amount");
    }

    @Test
    void renderLoopsTableRowsForArrayVariables() throws Exception {
        byte[] source = docxTable();
        TemplateVariableDefinition items = new TemplateVariableDefinition();
        items.setName("items");
        items.setType("ARRAY");

        TemplateRenderOutput output = renderer.render(new TemplateRenderPayload(
                TemplateSourceFormat.DOCX,
                TemplateOutputFormat.DOCX,
                null,
                source,
                "list.docx",
                Map.of("items", List.of(
                        Map.of("name", "身份证", "count", 1),
                        Map.of("name", "营业执照", "count", 2)
                )),
                List.of(items)
        ));

        assertThat(readDocxText(output.fileBytes()))
                .contains("身份证")
                .contains("营业执照")
                .doesNotContain("{{items}}");
    }

    private byte[] docx(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }

    private byte[] docxTable() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("资料名称");
            table.getRow(0).getCell(1).setText("数量");
            XWPFTableRow row = table.getRow(1);
            row.getCell(0).setText("{{items}} [name]");
            row.getCell(1).setText("[count]");
            document.write(output);
            return output.toByteArray();
        }
    }

    private String readDocxText(byte[] bytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String paragraphs = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", String::concat);
            String tables = document.getTables().stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .map(cell -> cell.getTextRecursively())
                    .reduce("", String::concat);
            return paragraphs + tables;
        }
    }
}
