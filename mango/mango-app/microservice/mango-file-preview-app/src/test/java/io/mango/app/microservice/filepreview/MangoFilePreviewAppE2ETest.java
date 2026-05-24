package io.mango.app.microservice.filepreview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.file.api.FileApi;
import io.mango.file.api.command.SaveGeneratedFileCommand;
import io.mango.file.api.command.FileArchiveCommand;
import io.mango.file.api.query.FileRecordPageQuery;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FilePreviewVO;
import io.mango.file.api.vo.FileRecordVO;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MangoFilePreviewAppE2ETest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "mango.file.enabled=false",
                "mango.kv.store.type=memory",
                "mango.authorization.resource-access.enabled=false",
                "office.plugin.enabled=false",
                "trust.host=127.0.0.1,localhost",
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
@DisplayName("Mango file preview app E2E tests")
class MangoFilePreviewAppE2ETest {

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
        assertThat(previewUrl).isEqualTo("/file-preview/files/preview?fileId=" + FILE_ID);

        ResponseEntity<String> previewResponse = restTemplate.getForEntity(baseUrl()
                + "/file-preview/files/preview?fileId=" + FILE_ID, String.class);
        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).contains(FILE_NAME);
        assertThat(previewResponse.getBody()).contains(Base64.getEncoder()
                .encodeToString((FILE_CONTENT + "\r\n").getBytes(StandardCharsets.UTF_8)));
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
    }

    private static class StubFileApi implements FileApi {

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
        public FileDownloadVO download(Long id) {
            byte[] content = FILE_CONTENT.getBytes(StandardCharsets.UTF_8);
            return new FileDownloadVO(new ByteArrayInputStream(content), FILE_NAME, "text/plain;charset=UTF-8", content.length);
        }

        @Override
        public R<PageResult<FileRecordVO>> page(FileRecordPageQuery query) {
            throw unsupported();
        }

        @Override
        public R<FilePreviewVO> preview(Long id) {
            throw unsupported();
        }

        @Override
        public R<FileRecordVO> saveGenerated(SaveGeneratedFileCommand command) {
            throw unsupported();
        }

        @Override
        public R<Boolean> archive(FileArchiveCommand command) {
            throw unsupported();
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Not used by file preview E2E");
        }
    }
}
