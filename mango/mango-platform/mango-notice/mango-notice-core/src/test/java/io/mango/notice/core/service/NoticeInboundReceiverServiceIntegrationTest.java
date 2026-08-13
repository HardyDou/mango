package io.mango.notice.core.service;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.file.api.IFileContentProvider;
import io.mango.file.api.command.SaveFileCommand;
import io.mango.file.api.vo.FileDownloadVO;
import io.mango.file.api.vo.FileRecordVO;
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
import io.mango.notice.api.enums.NoticeInboundMessageStatus;
import io.mango.notice.api.enums.NoticeInboundProtocol;
import io.mango.notice.api.query.NoticeInboundMessagePageQuery;
import io.mango.notice.api.vo.NoticeInboundMessageVO;
import io.mango.notice.core.entity.NoticeInboundAttachmentEntity;
import io.mango.notice.core.entity.NoticeInboundMessageEntity;
import io.mango.notice.core.entity.NoticeInboundReceiveCursorEntity;
import io.mango.notice.core.mapper.NoticeInboundAttachmentMapper;
import io.mango.notice.core.mapper.NoticeInboundMessageMapper;
import io.mango.notice.core.mapper.NoticeInboundReceiveCursorMapper;
import io.mango.notice.core.service.impl.NoticeInboundQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NoticeInboundReceiverServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_inbound;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class NoticeInboundReceiverServiceIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NoticeInboundReceiverService receiver;
    @Autowired private INoticeInboundQueryService inboundQueryService;
    @Autowired private NoticeInboundMessageMapper messageMapper;
    @Autowired private NoticeInboundAttachmentMapper attachmentMapper;
    @Autowired private NoticeInboundMailCursorService cursorService;
    @Autowired private NoticeInboundReceiveCursorMapper cursorMapper;
    @Autowired private RecordingFileProvider fileProvider;
    @Autowired private RecordingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("public-default"));
        resetSchema();
        fileProvider.clear();
        eventPublisher.clear();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void receivePersistsInboxStoresAttachmentAndBroadcastsStableFileId() {
        InboundReceiveResultResponse result = receiver.receive(message("IT_765_SOURCE", "payload.txt"));

        NoticeInboundMessageEntity stored = messageMapper.selectById(result.messageId());
        NoticeInboundAttachmentEntity attachment = attachmentMapper.selectList(null).getFirst();
        assertThat(stored.getTenantId()).isEqualTo("tenant-765");
        assertThat(stored.getStatus()).isEqualTo(NoticeInboundMessageStatus.BROADCASTED);
        assertThat(attachment.getTenantId()).isEqualTo("tenant-765");
        assertThat(attachment.getFileId()).isEqualTo(9001L);
        assertThat(fileProvider.commands).singleElement().satisfies(command -> {
            assertThat(command.getBizType()).isEqualTo("NOTICE_INBOUND_MESSAGE");
            assertThat(command.getAccessLevel()).isEqualTo("PRIVATE");
        });
        assertThat(eventPublisher.events).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo(NoticeInboundReceiverService.EVENT_TYPE);
            assertThat(event.getHeaders()).containsEntry("tenantId", "tenant-765");
            assertThat(event.getPayload()).containsEntry("fileIds", List.of(9001L));
            assertThat(event.getPayload().toString()).doesNotContain("url", "http");
        });
        assertThat(MangoContextHolder.tenantId()).isEqualTo("public-default");
    }

    @Test
    void duplicateSourceKeepsSingleInboxAndStableEventIdentity() {
        InboundReceiveResultResponse first = receiver.receive(message("IT_765_DUP", "first.txt"));
        InboundReceiveResultResponse second = receiver.receive(message("IT_765_DUP", "second.txt"));

        assertThat(second.duplicate()).isTrue();
        assertThat(second.messageId()).isEqualTo(first.messageId());
        assertThat(second.eventId()).isEqualTo(first.eventId());
        assertThat(messageMapper.selectCount(null)).isOne();
        assertThat(attachmentMapper.selectCount(null)).isOne();
        assertThat(eventPublisher.events).hasSize(1);
    }

    @Test
    void receivedMessageIsAvailableToAdministratorListAndDetail() {
        InboundReceiveResultResponse received = receiver.receive(message("IT_765_ADMIN_LIST", "admin-list.txt"));

        NoticeInboundMessagePageQuery query = new NoticeInboundMessagePageQuery();
        query.setChannelType(NoticeChannelType.EMAIL);
        query.setKeyword("Inbound test");
        var page = inboundQueryService.listInboundMessages(query);

        assertThat(page.getList()).singleElement().satisfies(summary -> {
            assertThat(summary.getId()).isEqualTo(received.messageId());
            assertThat(summary.getStatus()).isEqualTo(NoticeInboundMessageStatus.BROADCASTED);
            assertThat(summary.getBodyText()).isNull();
            assertThat(summary.getAttachments()).isNull();
        });

        NoticeInboundMessageVO detail = inboundQueryService.getInboundMessage(received.messageId());
        assertThat(detail.getBodyText()).isEqualTo("body");
        assertThat(detail.getAttachments()).singleElement().satisfies(attachment -> {
            assertThat(attachment.getFileId()).isEqualTo(9001L);
            assertThat(attachment.getFileName()).isEqualTo("admin-list.txt");
        });
    }

    @Test
    void successfulBroadcastClearsPreviousFailureState() {
        eventPublisher.failNext();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> receiver.receive(message("IT_765_RETRY", "retry.txt")))
                .isInstanceOf(NoticeInboundReceiverService.InboundReceiveRetryableException.class);
        NoticeInboundMessageEntity failed = messageMapper.selectOne(null);
        assertThat(failed.getFailureCode()).isEqualTo("NOTICE_INBOUND_BROADCAST_FAILED");
        assertThat(failed.getFailureReason()).isNotBlank();
        assertThat(failed.getNextRetryAt()).isNotNull();

        receiver.retryBroadcast("tenant-765", failed.getId());

        NoticeInboundMessageEntity broadcasted = messageMapper.selectById(failed.getId());
        assertThat(broadcasted.getStatus()).isEqualTo(NoticeInboundMessageStatus.BROADCASTED);
        assertThat(broadcasted.getFailureCode()).isNull();
        assertThat(broadcasted.getFailureReason()).isNull();
        assertThat(broadcasted.getNextRetryAt()).isNull();
    }

    @Test
    void successfulMailPollClearsPreviousCursorFailureState() {
        java.time.LocalDateTime nextPollAt = java.time.LocalDateTime.parse("2026-08-13T12:00:00");
        cursorService.recordFailure(new NoticeInboundMailCursorFailureCommand(765L, NoticeInboundProtocol.POP3,
                "InboundReceiveRetryableException", "入站消息广播暂未受理", nextPollAt));

        cursorService.advance(new NoticeInboundMailCursorAdvanceCommand(765L, NoticeInboundProtocol.POP3,
                "uidl-2222", "version-2222", nextPollAt.plusMinutes(1)));

        NoticeInboundReceiveCursorEntity cursor = cursorMapper.selectOne(null);
        assertThat(cursor.getCursorValue()).isEqualTo("uidl-2222");
        assertThat(cursor.getCursorVersion()).isEqualTo("version-2222");
        assertThat(cursor.getLastFailureCode()).isNull();
        assertThat(cursor.getLastFailureReason()).isNull();
    }

    private InboundNoticeMessageRequest message(String sourceKey, String fileName) {
        byte[] bytes = "IT_765_ATTACHMENT".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new InboundNoticeMessageRequest(
                "tenant-765", 765L, NoticeChannelType.EMAIL, "STANDARD_MAIL", NoticeInboundProtocol.IMAP,
                sourceKey, "message-765", "Inbound test", "sender@example.com",
                List.of("yunxinbaokeji@126.com"), "body", "", List.of(new InboundNoticeHeaderRequest("Message-ID", "message-765")),
                List.of(new InboundNoticeAttachmentRequest(
                        0, fileName, "text/plain", bytes.length, new ByteArrayInputStream(bytes))),
                Instant.parse("2026-08-13T04:00:00Z"));
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists notice_inbound_attachment");
        jdbcTemplate.execute("drop table if exists notice_inbound_message");
        jdbcTemplate.execute("drop table if exists notice_inbound_receive_cursor");
        jdbcTemplate.execute("""
                create table notice_inbound_message (
                    id bigint primary key, channel_config_id bigint not null, channel_type varchar(32) not null,
                    provider_code varchar(64), source_key varchar(255) not null, message_id varchar(255),
                    subject varchar(500), from_address varchar(500), to_addresses_json clob, body_text clob,
                    body_html clob, raw_headers_json clob, status varchar(32) not null, event_id varchar(64) not null,
                    failure_code varchar(128), failure_reason varchar(1000), attempt_count int default 0 not null,
                    next_retry_at timestamp, received_at timestamp not null, processed_at timestamp,
                    tenant_id varchar(64) not null, created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp,
                    constraint uk_inbound_source unique (tenant_id, channel_config_id, source_key),
                    constraint uk_inbound_event unique (tenant_id, event_id)
                )
                """);
        jdbcTemplate.execute("""
                create table notice_inbound_receive_cursor (
                    id bigint primary key, channel_config_id bigint not null, protocol varchar(16) not null,
                    cursor_value varchar(500), cursor_version varchar(255), last_polled_at timestamp,
                    next_poll_at timestamp, last_failure_code varchar(128), last_failure_reason varchar(1000),
                    tenant_id varchar(64) not null, created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp,
                    constraint uk_inbound_cursor unique (tenant_id, channel_config_id)
                )
                """);
        jdbcTemplate.execute("""
                create table notice_inbound_attachment (
                    id bigint primary key, message_id bigint not null, attachment_index int not null, file_id bigint,
                    file_name varchar(255) not null, content_type varchar(128), file_size bigint not null,
                    content_sha256 varchar(64), status varchar(32) not null, failure_code varchar(128),
                    failure_reason varchar(1000), attempt_count int default 0 not null, tenant_id varchar(64) not null,
                    created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp,
                    constraint uk_inbound_attachment unique (tenant_id, message_id, attachment_index)
                )
                """);
    }

    @Configuration
    @MapperScan(basePackageClasses = {
            NoticeInboundMessageMapper.class,
            NoticeInboundAttachmentMapper.class,
            NoticeInboundReceiveCursorMapper.class
    })
    @Import({NoticeInboundReceiverService.class, NoticeInboundMailCursorService.class,
            NoticeInboundQueryService.class})
    static class TestConfig {
        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean RecordingFileProvider fileProvider() {
            return new RecordingFileProvider();
        }

        @Bean RecordingEventPublisher eventPublisher() {
            return new RecordingEventPublisher();
        }

        @Bean PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    static class RecordingFileProvider implements IFileContentProvider {
        private final List<SaveFileCommand> commands = new ArrayList<>();

        @Override
        public FileRecordVO save(SaveFileCommand command) {
            commands.add(command);
            FileRecordVO result = new FileRecordVO();
            result.setId(9001L);
            return result;
        }

        @Override public FileDownloadVO download(Long id) {
            throw new AssertionError("not used");
        }

        @Override public FileDownloadVO downloadForService(Long id) {
            throw new AssertionError("not used");
        }

        void clear() {
            commands.clear();
        }
    }

    static class RecordingEventPublisher implements IDomainEventPublisher {
        private final List<DomainEvent> events = new ArrayList<>();
        private boolean failNext;

        @Override
        public void publish(DomainEvent event) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("event outbox unavailable");
            }
            events.add(event);
        }

        void failNext() {
            failNext = true;
        }

        void clear() {
            events.clear();
            failNext = false;
        }
    }
}
