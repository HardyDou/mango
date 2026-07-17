package io.mango.app.microservice.filepreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.FileDeleteCommand;
import io.mango.file.api.command.FileMergePdfCommand;
import io.mango.file.api.command.FilePackageCommand;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MangoFilePreviewAppFlowTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.file.enabled=false",
                "mango.kv.store.type=memory",
                "mango.authorization.resource-access.enabled=false",
                "office.plugin.enabled=false",
                "trust.host=127.0.0.1,localhost",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                        + "io.mango.infra.persistence.starter.PersistenceFlywayAutoConfiguration,"
                        + "io.mango.file.starter.remote.FileRemoteAutoConfiguration,"
                        + "com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure,"
                        + "org.redisson.spring.starter.RedissonAutoConfigurationV2"
        })
@Tag("flow")
@Tag("file-preview")
@DisplayName("Mango file preview app flow tests")
class MangoFilePreviewAppFlowTest {

    private static final Long FILE_ID = 10001L;
    private static final String FILE_NAME = "readme.txt";
    private static final String FILE_CONTENT = "Mango file preview E2E";

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("fileId preview should render online preview with source content")
    void fileIdPreviewShouldRenderOnlinePreviewWithSourceContent() throws Exception {
        ResponseEntity<String> linkResponse = restTemplate.getForEntity(baseUrl()
                + "/file-preview/files/preview-link?fileId=" + FILE_ID, String.class);
        assertThat(linkResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(linkResponse.getBody());
        assertThat(body.path("success").asBoolean()).isTrue();
        assertThat(body.at("/data/fileId").asText()).isEqualTo(FILE_ID.toString());
        String previewUrl = body.at("/data/previewUrl").asText();
        assertThat(previewUrl).startsWith("/file-preview/files/preview-entry?token=");

        ResponseEntity<String> previewResponse = restTemplate.getForEntity(baseUrl() + previewUrl, String.class);
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).contains(FILE_NAME);
        assertThat(previewResponse.getBody()).contains(Base64.getEncoder()
                .encodeToString((FILE_CONTENT + "\r\n").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("missing fileId should be rejected by API validation")
    void missingFileIdShouldBeRejectedByApiValidation() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl() + "/file-preview/files/preview-link", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("fileId");
    }

    @Test
    @DisplayName("blank preview tokens should be rejected by controller validation")
    void blankPreviewTokensShouldBeRejectedByControllerValidation() {
        ResponseEntity<String> entryResponse = restTemplate.getForEntity(
                baseUrl() + "/file-preview/files/preview-entry?token=", String.class);
        ResponseEntity<String> sourceResponse = restTemplate.getForEntity(
                baseUrl() + "/file-preview/sources?token=", String.class);

        assertThat(entryResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(sourceResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "io.mango.file.starter.FileAutoConfiguration",
            "io.mango.file.starter.remote.FileRemoteAutoConfiguration"
    })
    static class TestApp {

        @Bean
        @Primary
        FileApi fileApi() {
            return new StubFileApi();
        }

        @Bean
        @Primary
        IFileContentProvider fileContentProvider() {
            return new StubFileContentProvider();
        }
    }

    public static class StubFileApi implements FileApi {

        @Override
        public R<FileRecordVO> get(Long id) {
            if (!FILE_ID.equals(id)) {
                return R.fail("文件不存在");
            }
            FileRecordVO vo = new FileRecordVO();
            vo.setId(FILE_ID);
            vo.setFileName(FILE_NAME);
            vo.setFileExt("txt");
            vo.setFileSize((long) FILE_CONTENT.getBytes(StandardCharsets.UTF_8).length);
            vo.setContentType("text/plain;charset=UTF-8");
            return R.ok(vo);
        }

        @Override
        public R<PageResult<FileRecordVO>> page(FileRecordPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, 1, 10));
        }

        @Override
        public R<FilePreviewVO> preview(Long id) {
            return R.fail("文件不存在");
        }

        @Override
        public R<FileRecordVO> packageFiles(FilePackageCommand command) {
            return R.fail("文件不存在");
        }

        @Override
        public R<FileRecordVO> mergeToPdf(FileMergePdfCommand command) {
            return R.fail("文件不存在");
        }

        @Override
        public R<Boolean> archive(Long id, String reason) {
            return R.ok(false);
        }

        @Override
        public R<Boolean> delete(FileDeleteCommand command) {
            return R.ok(false);
        }
    }

    public static class StubFileContentProvider implements IFileContentProvider {

        @Override
        public FileRecordVO save(SaveFileCommand command) {
            FileRecordVO vo = new FileRecordVO();
            vo.setId(FILE_ID);
            vo.setFileName(command.getFileName());
            vo.setFileSize(command.getFileSize());
            vo.setContentType(command.getContentType());
            return vo;
        }

        @Override
        public FileDownloadVO download(Long id) {
            return downloadForService(id);
        }

        @Override
        public FileDownloadVO downloadForService(Long id) {
            byte[] content = FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
            return new FileDownloadVO(new ByteArrayInputStream(content), FILE_NAME,
                    "text/plain;charset=UTF-8", content.length);
        }
    }
}
