package io.mango.infra.excel.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelColumn;
import io.mango.infra.persistence.web.starter.excel.ExcelColumnConverter;
import io.mango.infra.persistence.web.starter.excel.ExcelDictionaryProvider;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelHeadGenerator;
import io.mango.infra.persistence.web.starter.excel.ExcelImportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportMode;
import io.mango.infra.persistence.web.starter.excel.ExcelLine;
import io.mango.infra.persistence.web.starter.excel.ExcelReadResult;
import io.mango.infra.persistence.web.starter.excel.FailureRowPolicy;
import io.mango.infra.persistence.web.starter.excel.ImportError;
import io.mango.infra.persistence.web.starter.excel.UnknownColumnPolicy;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoiExcelAdapterTest {

    private final AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

    @AfterEach
    void closeContext() {
        applicationContext.close();
    }

    @Test
    void mapsShuffledTitlesAndPreservesOriginalTextAndLine() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("协议号", "状态"), List.of("字段说明", "字段说明"),
                List.of(List.of("00123", "启用")));

        ExcelReadResult<TitleRow> result = adapter.readResult(file, context(2), TitleRow.class);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.agreementNo).isEqualTo("00123");
            assertThat(row.status).isEqualTo("启用");
            assertThat(row.line).isEqualTo(3);
        });
    }

    @Test
    void originalAndReorderedColumnsProduceSameDto() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile original = workbook("台账", List.of("状态", "协议号"), List.of(),
                List.of(List.of("启用", "00123")));
        MockMultipartFile reordered = workbook("台账", List.of("协议号", "状态"), List.of(),
                List.of(List.of("00123", "启用")));

        TitleRow originalRow = adapter.readResult(original, context(1), TitleRow.class).rows().getFirst();
        TitleRow reorderedRow = adapter.readResult(reordered, context(1), TitleRow.class).rows().getFirst();

        assertThat(reorderedRow.status).isEqualTo(originalRow.status);
        assertThat(reorderedRow.agreementNo).isEqualTo(originalRow.agreementNo);
    }

    @Test
    void mapsZeroBasedIndexWithoutTitleFallback() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("任意列", "另一个列"), List.of(),
                List.of(List.of("A", "B")));

        ExcelReadResult<IndexRow> result = adapter.readResult(file, context(1), IndexRow.class);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(row -> row.second).containsExactly("B");
    }

    @Test
    void customConverterWinsOverDictionaryMetadata() throws IOException {
        applicationContext.registerBean(UpperConverter.class);
        applicationContext.refresh();
        ExcelDictionaryProvider dictionary = (dictType, label, metadata, context) -> "DICT";
        PoiExcelAdapter adapter = adapter(dictionary);
        MockMultipartFile file = workbook("台账", List.of("状态"), List.of(), List.of(List.of("启用")));

        ExcelReadResult<ConverterRow> result = adapter.readResult(file, context(1), ConverterRow.class);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(row -> row.status).containsExactly("启用:TENDER_STATUS");
    }

    @Test
    void dictionaryConvertsLabelToTypedValue() throws IOException {
        ExcelDictionaryProvider dictionary = (dictType, label, metadata, context) ->
                "tender_status".equals(dictType) && "启用".equals(label) ? "1" : null;
        PoiExcelAdapter adapter = adapter(dictionary);
        MockMultipartFile file = workbook("台账", List.of("状态"), List.of(), List.of(List.of("启用")));

        ExcelReadResult<DictionaryRow> result = adapter.readResult(file, context(1), DictionaryRow.class);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(row -> row.status).containsExactly(1);
    }

    @Test
    void conversionErrorContainsLineTitleAndRawValue() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("数量"), List.of(), List.of(List.of("abc")));

        ExcelReadResult<NumberRow> result = adapter.readResult(file, context(1), NumberRow.class);

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.line()).isEqualTo(2);
            assertThat(error.title()).isEqualTo("数量");
            assertThat(error.rawValue()).isEqualTo("abc");
            assertThat(error.code()).isEqualTo("CELL_CONVERSION_FAILED");
        });
    }

    @Test
    void readsFormulaDateAmountMergedCellAndSkipsEmptyRow() throws IOException {
        PoiExcelAdapter adapter = adapter(null);

        ExcelReadResult<AdvancedRow> result = adapter.readResult(advancedWorkbook(), context(1), AdvancedRow.class);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.date).isEqualTo(LocalDate.of(2026, 7, 11));
            assertThat(row.amount).isEqualByComparingTo("123.45");
            assertThat(row.total).isEqualByComparingTo("246.9");
            assertThat(row.status).isEqualTo("启用");
        });
    }

    @Test
    void reportsDuplicateAndMissingRequiredTitlesWithoutIndexFallback() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("状态", "状态"), List.of(),
                List.of(List.of("启用", "禁用")));

        ExcelReadResult<TitleRow> result = adapter.readResult(file, context(1), TitleRow.class);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).extracting(ImportError::code)
                .contains("DUPLICATE_TITLE", "REQUIRED_TITLE_MISSING");
    }

    @Test
    void rejectsColumnConfiguredWithBothTitleAndIndex() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("状态"), List.of(), List.of(List.of("启用")));

        assertThatThrownBy(() -> adapter.readResult(file, context(1), InvalidRow.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须且只能配置 title 或 idx");
    }

    @Test
    void failureWorkbookKeepsOnlyFailedRowsAndAllReasons() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("协议号", "状态"), List.of("说明", "说明"),
                List.of(List.of("001", "启用"), List.of("002", "禁用")));
        List<ImportError> errors = List.of(
                ImportError.cell(4, "status", "状态", "禁用", "INVALID", "状态无效"),
                ImportError.cell(4, "agreementNo", "协议号", "002", "DUPLICATE", "协议号重复"),
                ImportError.cell(4, "agreementNo", "协议号", "002", "RELATED", "关联记录不存在"));

        byte[] content = adapter.createFailureWorkbook(file, context(2), errors);

        try (Workbook failed = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = failed.getSheet("台账");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("002");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue())
                    .isEqualTo("状态无效；协议号重复；关联记录不存在");
        }
    }

    @Test
    void failureWorkbookAppendsReasonAfterEveryExistingDataColumn() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("协议号"), List.of(),
                List.of(List.of("001", "原始扩展值", "原始备注")));

        byte[] content = adapter.createFailureWorkbook(file, context(1),
                List.of(ImportError.cell(2, "agreementNo", "协议号", "001", "INVALID", "协议号无效")));

        try (Workbook failed = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Row row = failed.getSheet("台账").getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("原始扩展值");
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("原始备注");
            assertThat(row.getCell(3).getStringCellValue()).isEqualTo("协议号无效");
        }
    }

    @Test
    void exportHonorsFieldSelectionNativeTypesAndCustomHead() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelExportContext exportContext = new ExcelExportContext("ledger.xlsx", "", "", "台账",
                List.of("amount", "enabled"), List.of("enabled"), TwoLevelHeadGenerator.class);

        adapter.write(response, exportContext, ExportRow.class,
                List.of(new ExportRow("启用", new BigDecimal("123.45"), true)));

        try (Workbook exported = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = exported.getSheet("台账");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("财务");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("金额");
            assertThat(sheet.getRow(2).getCell(0).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.NUMERIC);
            assertThat(sheet.getRow(2).getCell(0).getNumericCellValue()).isEqualTo(123.45D);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 1);
        }
    }

    @Test
    void duplicateConfiguredColumnShouldFailFast() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("任意列"), List.of(), List.of(List.of("A")));

        assertThatThrownBy(() -> adapter.readResult(file, context(1), DuplicateIndexRow.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复映射 Excel 列");
    }

    @Test
    void titleAndFixedIndexCannotResolveToSameWorkbookColumn() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockMultipartFile file = workbook("台账", List.of("状态"), List.of(), List.of(List.of("启用")));

        ExcelReadResult<ResolvedCollisionRow> result = adapter.readResult(file, context(1),
                ResolvedCollisionRow.class);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).extracting(ImportError::code).contains("DUPLICATE_COLUMN_MAPPING");
    }

    @Test
    void exportRejectsUnknownSelectionAndUnsupportedTemplateInsteadOfIgnoringConfiguration() {
        PoiExcelAdapter adapter = adapter(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelExportContext unknownField = new ExcelExportContext("ledger.xlsx", "", "", "台账",
                List.of("missing"), List.of(), ExcelHeadGenerator.class);
        ExcelExportContext unsupportedTemplate = new ExcelExportContext("ledger.xlsx", "template-key", "", "台账",
                List.of(), List.of(), ExcelHeadGenerator.class);

        assertThatThrownBy(() -> adapter.write(response, unknownField, ExportRow.class, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("include 包含未知字段");
        assertThatThrownBy(() -> adapter.write(response, unsupportedTemplate, ExportRow.class, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能静默忽略");
    }

    @Test
    void exportPreservesFixedColumnAndWritesUnsafeIntegerAsText() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelExportContext exportContext = new ExcelExportContext("fixed.xlsx", "", "", "固定列",
                List.of(), List.of(), ExcelHeadGenerator.class);

        adapter.write(response, exportContext, FixedExportRow.class,
                List.of(new FixedExportRow(123456789012345678L)));

        try (Workbook exported = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = exported.getSheet("固定列");
            assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("businessId");
            assertThat(sheet.getRow(1).getCell(2).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.STRING);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("123456789012345678");
        }
    }

    @Test
    void exportKeepsLegacyDefaultsForBlankDirectContext() throws IOException {
        PoiExcelAdapter adapter = adapter(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelExportContext blank = new ExcelExportContext(" ", "", "", " ", null, null, null);

        adapter.write(response, blank, ExportRow.class,
                List.of(new ExportRow("启用", new BigDecimal("1"), true)));

        assertThat(response.getHeader("Content-Disposition")).contains("export.xlsx");
        try (Workbook exported = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(exported.getSheet("sheet1")).isNotNull();
        }
    }

    @Test
    void classpathTemplateIsDownloadedByteForByte() throws IOException, URISyntaxException {
        PoiExcelAdapter adapter = adapter(null);
        byte[] original = richTemplate();
        Path classpathRoot = Path.of(PoiExcelAdapterTest.class.getClassLoader().getResource("").toURI());
        Path template = classpathRoot.resolve("templates/import-rich.xlsx");
        Files.createDirectories(template.getParent());
        Files.write(template, original);
        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelImportContext context = new ExcelImportContext("file", 2, true, ExcelImportMode.PARTIAL_SUCCESS,
                "台账", 0, UnknownColumnPolicy.IGNORE, "classpath:/templates/import-rich.xlsx",
                FailureRowPolicy.FAILED_ONLY);

        try {
            adapter.writeImportTemplate(response, context, TitleRow.class);
            assertThat(response.getContentAsByteArray()).isEqualTo(original);
        } finally {
            Files.deleteIfExists(template);
        }
    }

    private byte[] richTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("台账");
            sheet.createFreezePane(0, 2);
            sheet.setColumnWidth(0, 6000);
            sheet.createRow(0).createCell(0).setCellValue("协议号");
            sheet.createRow(1).createCell(0).setCellValue("字段说明");
            workbook.createSheet("字典").createRow(0).createCell(0).setCellValue("启用");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private MockMultipartFile advancedWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("台账");
            writeRow(sheet.createRow(0), List.of("日期", "金额", "合计", "状态", "状态说明"));
            Row data = sheet.createRow(1);
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
            data.createCell(0).setCellValue(LocalDate.of(2026, 7, 11));
            data.getCell(0).setCellStyle(dateStyle);
            data.createCell(1).setCellValue(123.45D);
            data.createCell(2).setCellFormula("B2*2");
            data.createCell(3).setCellValue("启用");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 3, 4));
            sheet.createRow(2);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(output);
            return new MockMultipartFile("file", "advanced.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private PoiExcelAdapter adapter(ExcelDictionaryProvider dictionaryProvider) {
        if (!applicationContext.isActive()) {
            if (dictionaryProvider != null) {
                applicationContext.registerBean(ExcelDictionaryProvider.class, () -> dictionaryProvider);
            }
            applicationContext.refresh();
        }
        return new PoiExcelAdapter(applicationContext,
                applicationContext.getBeanProvider(ExcelDictionaryProvider.class));
    }

    private ExcelImportContext context(int headRowNumber) {
        return new ExcelImportContext("file", headRowNumber, true, ExcelImportMode.PARTIAL_SUCCESS,
                "台账", 0, UnknownColumnPolicy.IGNORE, "", FailureRowPolicy.FAILED_ONLY);
    }

    private MockMultipartFile workbook(String sheetName, List<String> titles, List<String> descriptions,
                                       List<List<String>> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeRow(sheet.createRow(0), titles);
            int dataStart = 1;
            if (!descriptions.isEmpty()) {
                writeRow(sheet.createRow(1), descriptions);
                dataStart = 2;
            }
            for (int index = 0; index < rows.size(); index++) {
                writeRow(sheet.createRow(dataStart + index), rows.get(index));
            }
            workbook.write(output);
            return new MockMultipartFile("file", "fixture.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }

    private void writeRow(Row row, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            row.createCell(index).setCellValue(values.get(index));
        }
    }

    static class TitleRow {

        @ExcelLine
        private int line;

        @ExcelColumn(title = "状态", required = true)
        private String status;

        @ExcelColumn(title = "协议号", required = true)
        private String agreementNo;
    }

    static class IndexRow {

        @ExcelColumn(idx = 1)
        private String second;
    }

    static class ConverterRow {

        @ExcelColumn(title = "状态", dictType = "tender_status", converter = UpperConverter.class)
        private String status;
    }

    static class DictionaryRow {

        @ExcelColumn(title = "状态", dictType = "tender_status")
        private Integer status;
    }

    static class NumberRow {

        @ExcelColumn(title = "数量")
        private Integer quantity;
    }

    static class AdvancedRow {

        @ExcelColumn(title = "日期")
        private LocalDate date;

        @ExcelColumn(title = "金额")
        private BigDecimal amount;

        @ExcelColumn(title = "合计")
        private BigDecimal total;

        @ExcelColumn(title = "状态")
        private String status;
    }

    static class InvalidRow {

        @ExcelColumn(title = "状态", idx = 0)
        private String status;
    }

    static class DuplicateIndexRow {

        @ExcelColumn(idx = 0)
        private String first;

        @ExcelColumn(idx = 0)
        private String second;
    }

    static class ResolvedCollisionRow {

        @ExcelColumn(idx = 0)
        private String fixed;

        @ExcelColumn(title = "状态")
        private String status;
    }

    static class ExportRow {

        @ExcelColumn(title = "状态")
        private String status;

        @ExcelColumn(title = "金额")
        private BigDecimal amount;

        @ExcelColumn(title = "启用")
        private boolean enabled;

        ExportRow(String status, BigDecimal amount, boolean enabled) {
            this.status = status;
            this.amount = amount;
            this.enabled = enabled;
        }
    }

    static class FixedExportRow {

        @ExcelColumn(idx = 2)
        private long businessId;

        FixedExportRow(long businessId) {
            this.businessId = businessId;
        }
    }

    static class TwoLevelHeadGenerator implements ExcelHeadGenerator {

        @Override
        public List<List<String>> head(Class<?> rowType) {
            return List.of(List.of("财务", "金额"));
        }
    }

    static class UpperConverter implements ExcelColumnConverter<String> {

        @Override
        public String convert(io.mango.infra.persistence.web.starter.excel.ExcelCellValue value,
                              io.mango.infra.persistence.web.starter.excel.ExcelColumnMetadata metadata,
                              ExcelImportContext context) {
            return value.rawText() + ":" + metadata.dictType().toUpperCase();
        }
    }
}
