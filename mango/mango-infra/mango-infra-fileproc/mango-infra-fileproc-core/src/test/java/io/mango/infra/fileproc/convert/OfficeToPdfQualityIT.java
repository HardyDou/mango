package io.mango.infra.fileproc.convert;

import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.convert.ConvertOptionKeys;
import io.mango.infra.fileproc.convert.convert.OfficeManagerHolder;
import io.mango.infra.fileproc.convert.convert.OfficeToPdfConvertProvider;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class OfficeToPdfQualityIT {

    private static final String[] WORD_MARKERS = {
            "Mango Complex Word",
            "客户：华东合同中心",
            "金额合计",
            "质量校验标记-WORD-2026"
    };

    private static final String[] EXCEL_MARKERS = {
            "Mango Complex Excel",
            "华东大区",
            "质量校验标记-EXCEL-2026"
    };

    @Test
    @EnabledIf("officeAvailable")
    void convert_complexWordToPdf_preservesKeyTextAndRenderablePageQuality() throws Exception {
        ConvertResultVO result = provider().convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.DOCX)
                .targetFormat(ConvertFormat.PDF)
                .fileName("complex-word.docx")
                .inputStream(new ByteArrayInputStream(complexWord()))
                .option(ConvertOptionKeys.EXPORT_BOOKMARKS, true)
                .option(ConvertOptionKeys.EXPORT_NOTES, true)
                .option(ConvertOptionKeys.QUALITY, 95)
                .build());

        assertPdfQuality(result, WORD_MARKERS, 1);
    }

    @Test
    @EnabledIf("officeAvailable")
    void convert_complexExcelToPdf_preservesSheetTextAndRenderablePageQuality() throws Exception {
        ConvertResultVO result = provider().convert(ConvertCommand.builder()
                .sourceFormat(ConvertFormat.XLSX)
                .targetFormat(ConvertFormat.PDF)
                .fileName("complex-excel.xlsx")
                .inputStream(new ByteArrayInputStream(complexExcel()))
                .option(ConvertOptionKeys.EXPORT_BOOKMARKS, true)
                .option(ConvertOptionKeys.QUALITY, 95)
                .build());

        assertPdfQuality(result, EXCEL_MARKERS, 1);
    }

    private OfficeToPdfConvertProvider provider() {
        return new OfficeToPdfConvertProvider(new OfficeManagerHolder(officeHome(), new int[]{2091, 2092}, 300000L));
    }

    private void assertPdfQuality(ConvertResultVO result, String[] markers, int minPages) throws Exception {
        assertThat(result.format()).isEqualTo(ConvertFormat.PDF);
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).startsWith("%PDF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        try (PDDocument document = Loader.loadPDF(result.content())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(minPages);

            String text = new PDFTextStripper().getText(document);
            for (String marker : markers) {
                assertThat(text).contains(marker);
            }

            BufferedImage firstPage = new PDFRenderer(document).renderImageWithDPI(0, 72, ImageType.RGB);
            assertThat(nonWhitePixelRatio(firstPage)).isGreaterThan(0.02D);
            assertThat(firstPage.getWidth()).isGreaterThan(400);
            assertThat(firstPage.getHeight()).isGreaterThan(500);
        }
    }

    private double nonWhitePixelRatio(BufferedImage image) {
        int step = Math.max(1, Math.min(image.getWidth(), image.getHeight()) / 120);
        int total = 0;
        int nonWhite = 0;
        for (int y = 0; y < image.getHeight(); y += step) {
            for (int x = 0; x < image.getWidth(); x += step) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                total++;
                if (rgb < 0xF5F5F5) {
                    nonWhite++;
                }
            }
        }
        return (double) nonWhite / total;
    }

    private byte[] complexWord() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Mango Complex Word");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            XWPFParagraph summary = document.createParagraph();
            XWPFRun summaryRun = summary.createRun();
            summaryRun.setText("客户：华东合同中心；金额合计：128900.50；质量校验标记-WORD-2026");
            summaryRun.setColor("1F4E79");

            XWPFTable table = document.createTable(4, 4);
            fillCell(table.getRow(0).getCell(0), "项目");
            fillCell(table.getRow(0).getCell(1), "数量");
            fillCell(table.getRow(0).getCell(2), "单价");
            fillCell(table.getRow(0).getCell(3), "金额合计");
            for (int rowIndex = 1; rowIndex < 4; rowIndex++) {
                XWPFTableRow row = table.getRow(rowIndex);
                fillCell(row.getCell(0), "复杂格式-" + rowIndex);
                fillCell(row.getCell(1), String.valueOf(rowIndex * 3));
                fillCell(row.getCell(2), String.valueOf(rowIndex * 1200));
                fillCell(row.getCell(3), String.valueOf(rowIndex * 3600));
            }

            XWPFParagraph tail = document.createParagraph();
            XWPFRun tailRun = tail.createRun();
            tailRun.setText("包含标题、中文、表格、颜色与多段文本，用于转换质量回归。");
            tailRun.setItalic(true);

            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void fillCell(XWPFTableCell cell, String value) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(value);
    }

    private byte[] complexExcel() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("质量对比");
            sheet.setColumnWidth(0, 22 * 256);
            sheet.setColumnWidth(1, 18 * 256);
            sheet.setColumnWidth(2, 18 * 256);
            sheet.setColumnWidth(3, 20 * 256);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Mango Complex Excel");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

            Row header = sheet.createRow(2);
            String[] headers = {"区域", "收入", "增长率", "质量标记"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[] regions = {"华东大区", "华南大区", "华北大区"};
            for (int i = 0; i < regions.length; i++) {
                Row row = sheet.createRow(i + 3);
                row.createCell(0).setCellValue(regions[i]);
                row.createCell(1).setCellValue(56000 + i * 12000);
                row.createCell(2).setCellValue((0.12 + i * 0.03));
                row.createCell(3).setCellValue(i == 0 ? "质量校验标记-EXCEL-2026" : "通过");
            }

            Row formula = sheet.createRow(7);
            formula.createCell(0).setCellValue("金额合计");
            formula.createCell(1).setCellFormula("SUM(B4:B6)");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @SuppressWarnings("unused")
    private static boolean officeAvailable() {
        return officeHome() != null;
    }

    private static File officeHome() {
        File macOffice = new File("/Applications/LibreOffice.app/Contents");
        if (macOffice.exists()) {
            return macOffice;
        }
        String soffice = System.getenv("SOFFICE_HOME");
        if (soffice != null && !soffice.isBlank()) {
            File configured = new File(soffice);
            if (configured.exists()) {
                return configured;
            }
        }
        return null;
    }
}
