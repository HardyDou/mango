package io.mango.infra.excel.starter;

import io.mango.common.result.R;
import io.mango.infra.persistence.web.starter.PersistenceWebAutoConfiguration;
import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelColumn;
import io.mango.infra.persistence.web.starter.excel.ExcelLine;
import io.mango.infra.persistence.web.starter.excel.RequestExcel;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = PoiExcelMvcIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
class PoiExcelMvcIntegrationTest {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private ExcelAdapter excelAdapter;

    @Test
    void defaultAdapterResolvesMultipartRequestExcelFromRealHttpEntry() throws Exception {
        MockMultipartFile file = workbook();

        mockMvc.perform(multipart("/excel/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("00123"))
                .andExpect(jsonPath("$.data[1]").value("启用"))
                .andExpect(jsonPath("$.data[2]").value("3"));

        assertThat(excelAdapter).isInstanceOf(PoiExcelAdapter.class);
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
            WebMvcAutoConfiguration.class,
            PersistenceWebAutoConfiguration.class,
            ExcelAutoConfiguration.class
    })
    static class TestApplication {

        @Bean
        ExcelController excelController() {
            return new ExcelController();
        }
    }

    @RestController
    static class ExcelController {

        @PostMapping("/excel/import")
        R<List<String>> importRows(
                @RequestExcel(sheetName = "投标模板", headRowNumber = 2) List<HttpImportRow> rows) {
            HttpImportRow row = rows.getFirst();
            return R.ok(List.of(row.agreementNo, row.status, String.valueOf(row.line)));
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
}
