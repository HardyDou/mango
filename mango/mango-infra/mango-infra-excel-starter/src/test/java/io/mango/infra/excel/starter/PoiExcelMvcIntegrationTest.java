package io.mango.infra.excel.starter;

import io.mango.common.result.R;
import io.mango.infra.persistence.web.starter.PersistenceWebAutoConfiguration;
import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelColumn;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelLine;
import io.mango.infra.persistence.web.starter.excel.ExcelHeadGenerator;
import io.mango.infra.persistence.web.starter.excel.RequestExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@SpringBootTest(classes = PoiExcelMvcIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.junit.jupiter.api.Tag("flow")
@org.junit.jupiter.api.Tag("infra-excel")
class PoiExcelMvcIntegrationTest {

    @LocalServerPort
    private int port;

    @jakarta.annotation.Resource
    private ExcelAdapter excelAdapter;

    @Test
    void defaultAdapterResolvesMultipartRequestExcelFromRealHttpEntry() throws Exception {
        MockMultipartFile file = workbook();
        String boundary = "mango-infra-excel-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"fixture.xlsx\"\r\n"
                + "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        body.write(file.getBytes());
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/excel/import"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("00123", "启用", "\"3\"");
        assertThat(excelAdapter).isInstanceOf(PoiExcelAdapter.class);
    }

    @Test
    void exportEntryDownloadsSelectedNativeTypedWorkbookOverRealHttp() throws Exception {
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/excel/export"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.headers().firstValue("Content-Disposition").orElse(""))
                .contains("ledger.xlsx");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.body()))) {
            Sheet sheet = workbook.getSheet("台账");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("金额");
            assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(org.apache.poi.ss.usermodel.CellType.NUMERIC);
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(123.45D);
            assertThat(sheet.getRow(0).getLastCellNum()).isEqualTo((short) 1);
        }
    }

    private MockMultipartFile workbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("投标模板");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("协议号");
            title.createCell(1).setCellValue("状态");
            Row description = sheet.createRow(1);
            description.createCell(0).setCellValue("协议唯一编号");
            description.createCell(1).setCellValue("业务状态");
            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("00123");
            data.createCell(1).setCellValue("启用");
            workbook.write(output);
            return new MockMultipartFile("file", "fixture.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    output.toByteArray());
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            PersistenceWebAutoConfiguration.class,
            ExcelAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        ExcelController excelController(ExcelAdapter adapter) {
            return new ExcelController(adapter);
        }
    }

    @RestController
    static class ExcelController {

        private final ExcelAdapter adapter;

        ExcelController(ExcelAdapter adapter) {
            this.adapter = adapter;
        }

        @PostMapping("/excel/import")
        R<List<String>> importRows(
                @RequestExcel(sheetName = "投标模板", headRowNumber = 2) List<HttpImportRow> rows) {
            HttpImportRow row = rows.getFirst();
            return R.ok(List.of(row.agreementNo, row.status, String.valueOf(row.line)));
        }

        @GetMapping("/excel/export")
        void export(HttpServletResponse response) {
            ExcelExportContext context = new ExcelExportContext("ledger.xlsx", "", "", "台账",
                    List.of("amount"), List.of(), ExcelHeadGenerator.class);
            adapter.write(response, context, HttpExportRow.class,
                    List.of(new HttpExportRow("启用", new java.math.BigDecimal("123.45"))));
        }
    }

    static class HttpImportRow {

        @ExcelColumn(title = "状态", required = true)
        private String status;

        @ExcelColumn(title = "协议号", required = true)
        private String agreementNo;

        @ExcelLine
        private int line;
    }

    static class HttpExportRow {

        @ExcelColumn(title = "状态")
        private String status;

        @ExcelColumn(title = "金额")
        private java.math.BigDecimal amount;

        HttpExportRow(String status, java.math.BigDecimal amount) {
            this.status = status;
            this.amount = amount;
        }
    }
}
