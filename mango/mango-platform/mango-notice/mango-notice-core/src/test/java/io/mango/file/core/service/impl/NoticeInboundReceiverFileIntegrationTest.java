package io.mango.file.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.file.api.enums.FileAccessLevel;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.entity.FileStorageConfigEntity;
import io.mango.file.core.mapper.FileDirectoryMapper;
import io.mango.file.core.mapper.FileHashMappingMapper;
import io.mango.file.core.mapper.FileObjectMapper;
import io.mango.file.core.mapper.FileRecordMapper;
import io.mango.file.core.mapper.FileUploadPartMapper;
import io.mango.file.core.mapper.FileUploadSessionMapper;
import io.mango.file.core.service.IFileDirectoryService;
import io.mango.file.core.service.IFileSettingsService;
import io.mango.file.core.service.IFileStorageConfigService;
import io.mango.file.core.service.model.EnabledFileStorageKey;
import io.mango.file.core.storage.FileStorageRouter;
import io.mango.file.core.storage.LocalFileStorage;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.api.InboundNoticeAttachmentRequest;
import io.mango.notice.api.InboundNoticeMessageRequest;
import io.mango.notice.api.InboundNoticeHeaderRequest;
import io.mango.notice.api.InboundReceiveResultResponse;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import io.mango.notice.core.entity.NoticeInboundAttachmentEntity;
import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import io.mango.notice.core.mapper.NoticeInboundAttachmentMapper;
import io.mango.notice.core.mapper.NoticeInboundMessageMapper;
import io.mango.notice.core.service.NoticeInboundReceiverService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NoticeInboundReceiverFileIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_inbound_file;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class NoticeInboundReceiverFileIntegrationTest {

    private static final long TENANT_ID = 765L;

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NoticeInboundReceiverService receiver;
    @Autowired private NoticeInboundMessageMapper messageMapper;
    @Autowired private NoticeInboundAttachmentMapper attachmentMapper;
    @Autowired private FileRecordMapper fileRecordMapper;
    @Autowired private FileService fileService;
    @Autowired private FileProperties fileProperties;
    @Autowired private RecordingEventPublisher eventPublisher;

    @BeforeEach
    void setUp(@TempDir Path storageRoot) {
        fileProperties.getLocal().setRootPath(storageRoot.toString());
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("public-default"));
        resetSchema();
        eventPublisher.events.clear();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void receivePersistsNoticeAndRealFileRecordThenReadsAttachmentFromLocalStorage() throws Exception {
        byte[] content = "IT_765_REAL_FILE_ATTACHMENT".getBytes(StandardCharsets.UTF_8);
        InboundReceiveResultResponse result = receiver.receive(message(content));

        NoticeInboundMessageEntity notice = messageMapper.selectById(result.messageId());
        NoticeInboundAttachmentEntity attachment = attachmentMapper.selectList(null).getFirst();
        FileRecordVO file = fileRecordMapper.selectById(attachment.getFileId()) == null
                ? null : toFileRecord(fileRecordMapper.selectById(attachment.getFileId()));

        assertThat(notice.getTenantId()).isEqualTo(String.valueOf(TENANT_ID));
        assertThat(notice.getStatus()).isEqualTo(NoticeInboundMessageStatus.BROADCASTED);
        assertThat(attachment.getTenantId()).isEqualTo(String.valueOf(TENANT_ID));
        assertThat(attachment.getFileId()).isNotNull();
        assertThat(file).isNotNull();
        assertThat(file.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(file.getBizType()).isEqualTo("NOTICE_INBOUND_MESSAGE");
        assertThat(file.getBizId()).isEqualTo(String.valueOf(result.messageId()));
        assertThat(file.getAccessLevel()).isEqualTo(FileAccessLevel.PRIVATE.name());

        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId(String.valueOf(TENANT_ID)));
        try {
            FileDownloadVO download = fileService.downloadForService(attachment.getFileId());
            try (var input = download.inputStream()) {
                assertThat(input.readAllBytes()).containsExactly(content);
            }
        } finally {
            MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("public-default"));
        }
        assertThat(Files.walk(Path.of(fileProperties.getLocal().getRootPath()))
                .filter(Files::isRegularFile)
                .findAny()).isPresent();
        assertThat(eventPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.getPayload()).containsEntry("fileIds", List.of(attachment.getFileId()));
            assertThat(event.getPayload().toString()).doesNotContain("url", "http");
        });
    }

    private FileRecordVO toFileRecord(io.mango.file.core.entity.FileRecordEntity entity) {
        FileRecordVO result = new FileRecordVO();
        result.setId(entity.getId());
        result.setTenantId(entity.getTenantIdAsLong());
        result.setBizType(entity.getBizType());
        result.setBizId(entity.getBizId());
        result.setAccessLevel(entity.getAccessLevel());
        return result;
    }

    private InboundNoticeMessageRequest message(byte[] content) {
        return new InboundNoticeMessageRequest(
                String.valueOf(TENANT_ID), 765L, NoticeChannelType.EMAIL, "STANDARD_MAIL", NoticeInboundProtocol.IMAP,
                "IT_765_REAL_FILE", "mail-765", "Real file integration", "yunxinbaokeji@126.com",
                List.of("yunxinbaokeji@126.com"), "body", "", List.of(new InboundNoticeHeaderRequest("Message-ID", "mail-765")),
                List.of(new InboundNoticeAttachmentRequest(0, "IT_765_attachment.txt", "text/plain", content.length,
                        new ByteArrayInputStream(content))), Instant.parse("2026-08-13T04:00:00Z"));
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists notice_inbound_attachment");
        jdbcTemplate.execute("drop table if exists notice_inbound_message");
        jdbcTemplate.execute("drop table if exists file_record");
        jdbcTemplate.execute("drop table if exists file_hash_mapping");
        jdbcTemplate.execute("drop table if exists file_object");
        jdbcTemplate.execute("create table notice_inbound_message (id bigint primary key, channel_config_id bigint not null, channel_type varchar(32) not null, provider_code varchar(64), source_key varchar(255) not null, message_id varchar(255), subject varchar(500), from_address varchar(500), to_addresses_json clob, body_text clob, body_html clob, raw_headers_json clob, status varchar(32) not null, event_id varchar(64) not null, failure_code varchar(128), failure_reason varchar(1000), attempt_count int default 0 not null, next_retry_at timestamp, received_at timestamp not null, processed_at timestamp, tenant_id varchar(64) not null, created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp, unique(tenant_id, channel_config_id, source_key), unique(tenant_id, event_id))");
        jdbcTemplate.execute("create table notice_inbound_attachment (id bigint primary key, message_id bigint not null, attachment_index int not null, file_id bigint, file_name varchar(255) not null, content_type varchar(128), file_size bigint not null, content_sha256 varchar(64), status varchar(32) not null, failure_code varchar(128), failure_reason varchar(1000), attempt_count int default 0 not null, tenant_id varchar(64) not null, created_by bigint, created_at timestamp default current_timestamp, updated_by bigint, updated_at timestamp default current_timestamp, unique(tenant_id, message_id, attachment_index))");
        jdbcTemplate.execute("create table file_object (id bigint primary key, tenant_id varchar(64) not null, org_id bigint, storage_config_id bigint, storage_type varchar(32) not null, bucket_name varchar(128) not null, object_name varchar(500) not null, file_hash varchar(128) not null, file_size bigint not null, content_type varchar(128), status int not null, ref_count bigint not null, created_by bigint, created_time timestamp not null, created_at timestamp default current_timestamp, updated_by bigint, updated_time timestamp not null, updated_at timestamp default current_timestamp, unique(storage_config_id, bucket_name, file_hash, file_size))");
        jdbcTemplate.execute("create table file_hash_mapping (id bigint primary key, scope_type varchar(32) not null, tenant_id bigint not null, org_id bigint, storage_config_id bigint, file_hash varchar(128) not null, file_size bigint not null, object_id bigint not null, status int not null, created_by bigint, created_time timestamp not null, created_at timestamp default current_timestamp, updated_by bigint, updated_time timestamp not null, updated_at timestamp default current_timestamp, unique(scope_type, tenant_id, storage_config_id, file_hash, file_size))");
        jdbcTemplate.execute("create table file_record (id bigint primary key, tenant_id varchar(64) not null, org_id bigint, biz_type varchar(64), biz_id varchar(128), purpose varchar(64), biz_meta varchar(4000), directory_id bigint not null, access_level varchar(32) not null, object_id bigint not null, storage_type varchar(32) not null, storage_config_id bigint, bucket_name varchar(128) not null, object_name varchar(500) not null, file_name varchar(255) not null, file_ext varchar(32), file_size bigint not null, content_type varchar(128), file_hash varchar(128) not null, status int not null, archived int not null, created_by bigint, created_time timestamp not null, created_at timestamp default current_timestamp, updated_by bigint, updated_time timestamp not null, updated_at timestamp default current_timestamp)");
    }

    @Configuration
    @MapperScan(basePackageClasses = {NoticeInboundMessageMapper.class, NoticeInboundAttachmentMapper.class,
            FileObjectMapper.class, FileHashMappingMapper.class, FileRecordMapper.class,
            FileUploadSessionMapper.class, FileUploadPartMapper.class, FileDirectoryMapper.class})
    @Import(NoticeInboundReceiverService.class)
    static class TestConfig {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean FileProperties fileProperties() { return new FileProperties(); }
        @Bean FileStorageRouter fileStorageRouter(FileProperties properties) {
            return new FileStorageRouter(List.of(new LocalFileStorage(properties)));
        }
        @Bean IFileStorageConfigService storageConfigService() {
            IFileStorageConfigService service = mock(IFileStorageConfigService.class);
            FileStorageConfigEntity config = new FileStorageConfigEntity();
            config.setId(1L); config.setTenantId(TENANT_ID); config.setStorageType("LOCAL");
            config.setBucketName("IT_765_NOTICE"); config.setStoragePath("notice"); config.setStatus(1);
            when(service.activeConfig()).thenReturn(config);
            when(service.getEnabledConfig(any(EnabledFileStorageKey.class))).thenReturn(config);
            return service;
        }
        @Bean IFileSettingsService settingsService() {
            IFileSettingsService service = mock(IFileSettingsService.class);
            io.mango.file.api.vo.FileSettingsVO settings = new io.mango.file.api.vo.FileSettingsVO();
            settings.setMaxSize(1024L * 1024L); settings.setDefaultAccessLevel("PRIVATE");
            settings.setDuplicateNameStrategy("ALLOW"); settings.setDuplicateCheckDirectoryScoped(true);
            settings.setObjectNameStrategy("DATE_UUID"); settings.setInstantUploadEnabled(true);
            settings.setInstantUploadScope("TENANT"); settings.setContentTypeCheckEnabled(false);
            settings.setAccessMode("PROXY");
            when(service.current()).thenReturn(settings);
            return service;
        }
        @Bean IFileDirectoryService directoryService() { return mock(IFileDirectoryService.class); }
        @Bean FileService fileService(FileStorageRouter router, IFileStorageConfigService storage,
                                      IFileSettingsService settings, IFileDirectoryService directory,
                                      FileRecordMapper record, FileObjectMapper object, FileHashMappingMapper hash,
                                      FileUploadSessionMapper session, FileUploadPartMapper part,
                                      FileDirectoryMapper directories, FileProperties properties) {
            return new FileService(router, storage, settings, directory, record, object, hash, session, part,
                    directories, new ObjectMapper(), new FileAccessUrlAssembler(properties), List.of(), List.of(),
                    List.of(), new FilePackageSizeControlProcessor(List.of()));
        }
        @Bean RecordingEventPublisher eventPublisher() { return new RecordingEventPublisher(); }
        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
        @Bean IDomainEventPublisher domainEventPublisher(RecordingEventPublisher publisher) { return publisher; }
    }

    static class RecordingEventPublisher implements IDomainEventPublisher {
        private final java.util.ArrayList<DomainEvent> events = new java.util.ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
