package io.mango.infra.fileproc.render;

import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.command.RenderVariableDefinition;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.service.DocxRenderProvider;
import io.mango.infra.fileproc.render.service.PlaceholderRenderEngine;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DocxRenderProviderTest {

    private final DocxRenderProvider provider = new DocxRenderProvider(new PlaceholderRenderEngine());

    @Test
    void renderReplacesVariablesWithPoiTl() throws Exception {
        byte[] source = docx("客户：{{customer.name}}，金额：{{amount}}");

        RenderResultVO output = provider.render(command(source, "contract.docx",
                Map.of("customer", Map.of("name", "张三"), "amount", 128), List.of()));

        assertThat(output.fileName()).isEqualTo("contract-rendered.docx");
        assertThat(readDocxText(output.content())).contains("客户：张三，金额：128");
    }

    @Test
    void extractVariablesReadsDocxXmlEntries() throws Exception {
        byte[] source = docx("客户：{{customer.name}}，金额：{{amount}}");

        assertThat(provider.extractVariables(command(source, "contract.docx", Map.of(), List.of())))
                .containsExactly("customer.name", "amount");
    }

    @Test
    void renderLoopsTableRowsForArrayVariables() throws Exception {
        byte[] source = docxTable();
        RenderVariableDefinition items = RenderVariableDefinition.of("items", "ARRAY");

        RenderResultVO output = provider.render(command(source, "list.docx",
                Map.of("items", List.of(
                        Map.of("name", "身份证", "count", 1),
                        Map.of("name", "营业执照", "count", 2)
                )),
                List.of(items)));

        assertThat(readDocxText(output.content()))
                .contains("身份证")
                .contains("营业执照")
                .doesNotContain("{{items}}");
    }

    private RenderCommand command(byte[] source,
                                  String fileName,
                                  Map<String, Object> variables,
                                  List<RenderVariableDefinition> definitions) {
        return RenderCommand.builder()
                .sourceFormat(RenderFormat.DOCX)
                .targetFormat(RenderFormat.DOCX)
                .inputStream(new ByteArrayInputStream(source))
                .fileName(fileName)
                .variables(variables)
                .variableDefinitions(definitions)
                .build();
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
