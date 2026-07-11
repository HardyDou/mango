package io.mango.infra.persistence.web.starter.controller;

import io.mango.infra.persistence.api.crud.MangoCrudService;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportMode;
import io.mango.infra.persistence.web.starter.excel.ImportError;
import io.mango.infra.persistence.web.starter.excel.ImportResult;
import io.mango.infra.persistence.web.starter.excel.ImportableService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ExcelImportTransactionIntegrationTest.TestApplication.class,
        properties = "mango.persistence.schema-validation.enabled=false")
@AutoConfigureMockMvc
class ExcelImportTransactionIntegrationTest {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTable() {
        jdbcTemplate.update("delete from excel_import_tx");
    }

    @Test
    void allSuccessRuntimeFailureRollsBackWholeDatabaseTransaction() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rows.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/tx/import").file(file).param("importMode", "ALL_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.batchErrors[0].code").value("IMPORT_BATCH_FAILED"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from excel_import_tx", Integer.class)).isZero();
    }

    @Test
    void partialSuccessPersistsOnlyRowsWithoutValidationErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "rows.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/tx/import").file(file).param("importMode", "PARTIAL_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIAL_SUCCESS"))
                .andExpect(jsonPath("$.data.success").value(1))
                .andExpect(jsonPath("$.data.failed").value(1));

        assertThat(jdbcTemplate.queryForList("select code from excel_import_tx", String.class))
                .containsExactly("A");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
                    .addScript("classpath:excel-import-transaction-schema.sql").build();
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionalImportService transactionalImportService(JdbcTemplate jdbcTemplate) {
            return new TransactionalImportService(jdbcTemplate);
        }

        @Bean
        TransactionController transactionController(TransactionalImportService service) {
            return new TransactionController(service);
        }

        @Bean
        ExcelAdapter excelAdapter() {
            return new TransactionExcelAdapter();
        }
    }

    @RestController
    @RequestMapping("/tx")
    static class TransactionController extends BaseCrudController<TransactionalImportService, Object, Object, Object> {

        TransactionController(TransactionalImportService service) {
            super(service);
        }

        @Override
        protected Class<Object> queryType() {
            return Object.class;
        }
    }

    static class TransactionalImportService implements MangoCrudService<Object>, ImportableService<TransactionRow> {

        private final JdbcTemplate jdbcTemplate;

        TransactionalImportService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public Class<TransactionRow> importRowType() {
            return TransactionRow.class;
        }

        @Override
        public List<ImportError> validateImportRows(List<TransactionRow> rows, ExcelImportContext context) {
            if (ExcelImportMode.PARTIAL_SUCCESS.equals(context.mode())) {
                return List.of(ImportError.of(context.headRowNumber() + 2, "code", "B 已存在"));
            }
            return List.of();
        }

        @Override
        public ImportResult importRows(List<TransactionRow> rows) {
            return importRows(rows, new ExcelImportContext("file", 1, true, ExcelImportMode.PARTIAL_SUCCESS));
        }

        @Override
        public ImportResult importRows(List<TransactionRow> rows, ExcelImportContext context) {
            for (TransactionRow row : rows) {
                jdbcTemplate.update("insert into excel_import_tx(code) values (?)", row.code);
            }
            if (ExcelImportMode.ALL_SUCCESS.equals(context.mode())) {
                throw new IllegalStateException("模拟第二阶段批量写入失败");
            }
            return ImportResult.success(rows.size());
        }

        @Override
        public Object createByCommand(Object command) {
            return null;
        }

        @Override
        public boolean updateByCommand(Object command) {
            return false;
        }

        @Override
        public boolean deleteById(Object id) {
            return false;
        }

        @Override
        public boolean batchDeleteByIds(List<?> ids) {
            return false;
        }

        @Override
        public Object detailById(Object id) {
            return null;
        }

        @Override
        public List<?> listByQuery(Object query) {
            return List.of();
        }

        @Override
        public PersistencePageResult<?> pageByQuery(Object query) {
            return PersistencePageResult.of(List.of(), 0, 1, 10);
        }
    }

    static class TransactionExcelAdapter implements ExcelAdapter {

        @Override
        public <ROW> List<ROW> read(MultipartFile file, ExcelImportContext context, Class<ROW> rowType) {
            return List.of(rowType.cast(new TransactionRow("A")), rowType.cast(new TransactionRow("B")));
        }

        @Override
        public <ROW> void write(HttpServletResponse response, ExcelExportContext context, Class<ROW> rowType,
                                List<ROW> rows) {
            throw new IllegalStateException("事务测试不执行导出");
        }
    }

    static class TransactionRow {

        private final String code;

        TransactionRow(String code) {
            this.code = code;
        }
    }
}
