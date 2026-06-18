package io.mango.infra.persistence.web.starter.controller;

import io.mango.common.po.PageQuery;
import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.MangoCrudService;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelExport;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImport;
import io.mango.infra.persistence.web.starter.excel.ExcelImportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelLine;
import io.mango.infra.persistence.web.starter.excel.ExportableService;
import io.mango.infra.persistence.web.starter.excel.ImportError;
import io.mango.infra.persistence.web.starter.excel.ImportResult;
import io.mango.infra.persistence.web.starter.excel.ImportableService;
import io.mango.infra.persistence.web.starter.excel.RequestExcel;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BaseCrudControllerTest.TestApplication.class)
@AutoConfigureMockMvc
class BaseCrudControllerTest {

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private RecordingCrudService recordingCrudService;

    @jakarta.annotation.Resource
    private RecordingExcelCrudService recordingExcelCrudService;

    @BeforeEach
    void setUp() {
        recordingExcelCrudService.reset();
    }

    @Test
    void defaultEndpoints_shouldExposeTypedContractsAndPassThemToLooseService() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","nickname":"Alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10001));

        assertThat(recordingCrudService.lastCreateCommand)
                .isEqualTo(new UserCreateCommand("alice", "Alice"));

        mockMvc.perform(post("/users/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":10001,"nickname":"Alice Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        assertThat(recordingCrudService.lastUpdateCommand)
                .isEqualTo(new UserUpdateCommand(10001L, "Alice Updated"));

        mockMvc.perform(post("/users/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":"10001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        assertThat(recordingCrudService.lastDeletedId).isEqualTo(10001L);

        mockMvc.perform(post("/users/batch-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":["10001","10002"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        assertThat(new ArrayList<>(recordingCrudService.lastBatchDeletedIds)).isEqualTo(List.of(10001L, 10002L));

        mockMvc.perform(get("/users/detail").param("id", "10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));

        assertThat(recordingCrudService.lastDetailId).isEqualTo(10001L);

        mockMvc.perform(get("/users/page")
                        .param("page", "2")
                        .param("size", "5")
                        .param("username", "ali")
                        .param("statuses", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("alice"));

        assertThat(recordingCrudService.lastQuery.getPage()).isEqualTo(2);
        assertThat(recordingCrudService.lastQuery.getSize()).isEqualTo(5);
        assertThat(recordingCrudService.lastQuery.getUsername()).isEqualTo("ali");
        assertThat(recordingCrudService.lastQuery.getStatuses()).containsExactly(1, 2);
    }

    @Test
    void excelEndpoints_shouldFailWhenServiceOrAdapterIsNotEnabled() throws Exception {
        mockMvc.perform(post("/users/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());

        MockMultipartFile file = new MockMultipartFile(
                "file", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/users/import").file(file))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void excelEndpoints_shouldPassMethodAnnotationMetadataAndPojoTypesToAdapter() throws Exception {
        mockMvc.perform(post("/excel-users/export")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"page":1,"size":10,"username":"ali"}
                                """))
                .andExpect(status().isOk());

        assertThat(recordingExcelCrudService.lastExportQuery.getUsername()).isEqualTo("ali");
        assertThat(recordingExcelCrudService.lastExportContext.fileName()).isEqualTo("users.xlsx");
        assertThat(recordingExcelCrudService.lastExportContext.templateKey()).isEqualTo("user-export");
        assertThat(recordingExcelCrudService.lastExportContext.templateLocation())
                .isEqualTo("classpath:/templates/export/users.xlsx");
        assertThat(recordingExcelCrudService.lastExportContext.include()).containsExactly("username", "nickname");
        assertThat(recordingExcelCrudService.lastExportRowType).isEqualTo(UserExportExcelRow.class);

        MockMultipartFile file = new MockMultipartFile(
                "upload", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/excel-users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(1));

        assertThat(recordingExcelCrudService.lastImportContext.fileName()).isEqualTo("upload");
        assertThat(recordingExcelCrudService.lastImportContext.headRowNumber()).isEqualTo(2);
        assertThat(recordingExcelCrudService.lastImportRowType).isEqualTo(UserImportExcelRow.class);

        mockMvc.perform(get("/excel-users/import-template"))
                .andExpect(status().isOk());

        assertThat(recordingExcelCrudService.templateRowType).isEqualTo(UserImportExcelRow.class);
    }

    @Test
    void importData_shouldReturnFailureDetailsWhenPojoValidationFails() throws Exception {
        recordingExcelCrudService.returnInvalidImportRows = true;

        MockMultipartFile file = new MockMultipartFile(
                "upload", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/excel-users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.success").value(0))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.errors[0].line").value(3))
                .andExpect(jsonPath("$.data.errors[0].field").value("username"))
                .andExpect(jsonPath("$.data.errors[0].message").value("用户名不能为空"));

        assertThat(recordingExcelCrudService.importRowsCalled).isFalse();
    }

    @Test
    void importData_shouldReturnFailureDetailsWhenBusinessValidationFails() throws Exception {
        recordingExcelCrudService.returnDuplicateImportRows = true;

        MockMultipartFile file = new MockMultipartFile(
                "upload", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/excel-users/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.success").value(1))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.errors[0].line").value(4))
                .andExpect(jsonPath("$.data.errors[0].field").value("username"))
                .andExpect(jsonPath("$.data.errors[0].message").value("用户名已存在"));

        assertThat(recordingExcelCrudService.importRowsCalled).isTrue();
        assertThat(recordingExcelCrudService.importedRows).extracting(UserImportExcelRow::username)
                .containsExactly("alice");
        assertThat(recordingExcelCrudService.importedRows).extracting(UserImportExcelRow::lineNum)
                .containsExactly(3L);
    }

    @Test
    void importData_shouldNotImportAnyRowsWhenRequestModeRequiresAllSuccess() throws Exception {
        recordingExcelCrudService.returnDuplicateImportRows = true;

        MockMultipartFile file = new MockMultipartFile(
                "upload", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/excel-users/import").file(file).param("importMode", "ALL_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.success").value(0))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.errors[0].line").value(4));

        assertThat(recordingExcelCrudService.importRowsCalled).isFalse();
    }

    @Test
    void requestExcel_shouldResolveTypedListAndFillLineNumber() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "upload", "users.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/excel-users/request-excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @SpringBootApplication
    static class TestApplication {

        @Bean
        @Primary
        RecordingCrudService recordingCrudService() {
            return new RecordingCrudService();
        }

        @Bean
        RecordingExcelCrudService recordingExcelCrudService() {
            return new RecordingExcelCrudService();
        }

        @Bean
        UserController userController(RecordingCrudService service) {
            return new UserController(service);
        }

        @Bean
        ExcelUserController excelUserController(RecordingExcelCrudService service) {
            return new ExcelUserController(service);
        }

        @Bean
        ExcelAdapter excelAdapter(RecordingExcelCrudService service) {
            return new RecordingExcelAdapter(service);
        }
    }

    @RestController
    @RequestMapping("/users")
    static class UserController extends BaseCrudController<RecordingCrudService, UserCreateCommand, UserUpdateCommand,
            UserQuery> {

        UserController(RecordingCrudService service) {
            super(service);
        }

        @Override
        protected Class<UserQuery> queryType() {
            return UserQuery.class;
        }
    }

    @RestController
    @RequestMapping("/excel-users")
    static class ExcelUserController extends BaseCrudController<RecordingExcelCrudService, UserCreateCommand,
            UserUpdateCommand, UserQuery> {

        ExcelUserController(RecordingExcelCrudService service) {
            super(service);
        }

        @Override
        protected Class<UserQuery> queryType() {
            return UserQuery.class;
        }

        @Override
        @ExcelExport(fileName = "users.xlsx", templateKey = "user-export",
                templateLocation = "classpath:/templates/export/users.xlsx",
                include = {"username", "nickname"})
        public void export(Map<String, Object> body, HttpServletResponse response) {
            super.export(body, response);
        }

        @Override
        @ExcelImport(fileName = "upload", headRowNumber = 2)
        public R<ImportResult> importData(MultipartHttpServletRequest request) {
            return super.importData(request);
        }

        @Override
        @ExcelImport(fileName = "upload", headRowNumber = 2)
        public void importTemplate(HttpServletResponse response) {
            super.importTemplate(response);
        }

        @PostMapping("/request-excel")
        public R<Long> requestExcel(@RequestExcel(fileName = "upload", headRowNumber = 2)
                                    List<UserImportExcelRow> rows) {
            return R.ok(rows.get(0).lineNum());
        }
    }

    @Service
    static class RecordingCrudService implements MangoCrudService<Object> {

        private UserCreateCommand lastCreateCommand;

        private UserUpdateCommand lastUpdateCommand;

        private Object lastDeletedId;

        private List<?> lastBatchDeletedIds = new ArrayList<>();

        private Object lastDetailId;

        private UserQuery lastQuery;

        @Override
        public Object createByCommand(Object command) {
            lastCreateCommand = (UserCreateCommand) command;
            return 10001L;
        }

        @Override
        public boolean updateByCommand(Object command) {
            lastUpdateCommand = (UserUpdateCommand) command;
            return true;
        }

        @Override
        public boolean deleteById(Object id) {
            lastDeletedId = (Long) id;
            return true;
        }

        @Override
        public boolean batchDeleteByIds(List<?> ids) {
            lastBatchDeletedIds = ids;
            return true;
        }

        @Override
        public Object detailById(Object id) {
            lastDetailId = id;
            return new UserVO(10001L, "alice", "Alice");
        }

        @Override
        public List<?> listByQuery(Object query) {
            lastQuery = (UserQuery) query;
            return List.of(new UserVO(10001L, "alice", "Alice"));
        }

        @Override
        public PersistencePageResult<?> pageByQuery(Object query) {
            lastQuery = (UserQuery) query;
            return PersistencePageResult.of(List.of(new UserVO(10001L, "alice", "Alice")), 1, 1, 10);
        }

    }

    static class RecordingExcelCrudService extends RecordingCrudService
            implements ExportableService<UserQuery, UserExportExcelRow>, ImportableService<UserImportExcelRow> {

        private UserQuery lastExportQuery;

        private ExcelExportContext lastExportContext;

        private Class<?> lastExportRowType;

        private ExcelImportContext lastImportContext;

        private Class<?> lastImportRowType;

        private Class<?> templateRowType;

        private boolean returnInvalidImportRows;

        private boolean returnDuplicateImportRows;

        private boolean importRowsCalled;

        private List<UserImportExcelRow> importedRows = new ArrayList<>();

        void reset() {
            returnInvalidImportRows = false;
            returnDuplicateImportRows = false;
            importRowsCalled = false;
            lastExportQuery = null;
            lastExportContext = null;
            lastExportRowType = null;
            lastImportContext = null;
            lastImportRowType = null;
            templateRowType = null;
            importedRows = new ArrayList<>();
        }

        @Override
        public Class<UserExportExcelRow> exportRowType() {
            return UserExportExcelRow.class;
        }

        @Override
        public List<UserExportExcelRow> exportRows(UserQuery query) {
            lastExportQuery = query;
            return List.of(new UserExportExcelRow("alice", "Alice"));
        }

        @Override
        public Class<UserImportExcelRow> importRowType() {
            return UserImportExcelRow.class;
        }

        @Override
        public List<ImportError> validateImportRows(List<UserImportExcelRow> rows, ExcelImportContext context) {
            if (!returnDuplicateImportRows) {
                return List.of();
            }
            return List.of(
                    ImportError.of(context.headRowNumber() + 2, "username", "用户名已存在")
            );
        }

        @Override
        public ImportResult importRows(List<UserImportExcelRow> rows) {
            importRowsCalled = true;
            importedRows = new ArrayList<>(rows);
            return ImportResult.success(rows.size());
        }
    }

    static class RecordingExcelAdapter implements ExcelAdapter {

        private final RecordingExcelCrudService service;

        RecordingExcelAdapter(RecordingExcelCrudService service) {
            this.service = service;
        }

        @Override
        public <ROW> List<ROW> read(MultipartFile file, ExcelImportContext context, Class<ROW> rowType) {
            service.lastImportContext = context;
            service.lastImportRowType = rowType;
            if (service.returnDuplicateImportRows) {
                return List.of(
                        rowType.cast(new UserImportExcelRow(null, "alice", "Alice")),
                        rowType.cast(new UserImportExcelRow(null, "alice", "Alice Duplicate"))
                );
            }
            UserImportExcelRow row = service.returnInvalidImportRows
                    ? new UserImportExcelRow(null, "", "Alice")
                    : new UserImportExcelRow(null, "alice", "Alice");
            return List.of(rowType.cast(row));
        }

        @Override
        public <ROW> void write(HttpServletResponse response, ExcelExportContext context, Class<ROW> rowType,
                                List<ROW> rows) {
            service.lastExportContext = context;
            service.lastExportRowType = rowType;
        }

        @Override
        public <ROW> void writeImportTemplate(HttpServletResponse response, ExcelImportContext context,
                                               Class<ROW> rowType) {
            service.lastImportContext = context;
            service.templateRowType = rowType;
        }
    }

    record UserCreateCommand(String username, String nickname) {
    }

    record UserUpdateCommand(Long id, String nickname) {
    }

    static class UserQuery extends PageQuery {

        private String username;

        private List<Integer> statuses;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public List<Integer> getStatuses() {
            return statuses;
        }

        public void setStatuses(List<Integer> statuses) {
            this.statuses = statuses;
        }
    }

    record UserVO(Long id, String username, String nickname) {
    }

    record UserExportExcelRow(String username, String nickname) {
    }

    static class UserImportExcelRow {

        @ExcelLine
        private Long lineNum;

        @NotBlank(message = "用户名不能为空")
        private String username;

        private String nickname;

        UserImportExcelRow(Long lineNum, String username, String nickname) {
            this.lineNum = lineNum;
            this.username = username;
            this.nickname = nickname;
        }

        public Long lineNum() {
            return lineNum;
        }

        public String username() {
            return username;
        }

        public String nickname() {
            return nickname;
        }
    }
}
