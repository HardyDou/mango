package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.common.vo.PageResult;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.api.command.NoticeAnnouncementTargetCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.PublishNoticeAnnouncementCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeAnnouncementConfirmStatus;
import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.query.MyNoticeAnnouncementPageQuery;
import io.mango.notice.api.vo.NoticeAnnouncementVO;
import io.mango.notice.core.entity.NoticeAnnouncementEntity;
import io.mango.notice.core.entity.NoticeAnnouncementRecipientEntity;
import io.mango.notice.core.mapper.NoticeAnnouncementMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementRecipientMapper;
import io.mango.notice.core.mapper.NoticeAnnouncementTargetMapper;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.core.service.NoticeRecipientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.mango.notice.core.service.impl.NoticeAnnouncementService.ANNOUNCEMENT_PUBLISHED_BIZ_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NoticeAnnouncementServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_announcement_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class NoticeAnnouncementServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NoticeAnnouncementMapper announcementMapper;

    @Autowired
    private NoticeAnnouncementRecipientMapper recipientMapper;

    @Autowired
    private TestNoticeRecipientResolver recipientResolver;

    @Autowired
    private INoticeService noticeService;

    @Autowired
    private NoticeAnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        resetSchema();
        recipientResolver.clear();
        reset(noticeService);
    }

    @Test
    void publishAnnouncementFansOutAllUsersOnceAndSendsSiteReminderThroughRealMappers() {
        insertDraftAnnouncement(100L, true, true);
        recipientResolver.allEnabledUsers = List.of(
                recipient(1L, "张三"),
                recipient(1L, "张三重复"),
                recipient(2L, "李四"));
        PublishNoticeAnnouncementCommand command = new PublishNoticeAnnouncementCommand();
        command.setTargets(List.of(target(NoticeAnnouncementTargetType.ALL, null, "全员")));

        assertThat(announcementService.publishAnnouncement(100L, command)).isTrue();

        assertThat(targetTypes(100L)).containsExactly("ALL");
        List<NoticeAnnouncementRecipientEntity> savedRecipients = recipients(100L);
        assertThat(savedRecipients).hasSize(2);
        assertThat(savedRecipients.stream().map(NoticeAnnouncementRecipientEntity::getUserId))
                .containsExactly(1L, 2L);
        assertThat(savedRecipients).allSatisfy(recipient -> {
            assertThat(recipient.getReadStatus()).isEqualTo(NoticeReadStatus.UNREAD);
            assertThat(recipient.getConfirmStatus()).isEqualTo(NoticeAnnouncementConfirmStatus.PENDING);
        });

        NoticeAnnouncementEntity published = announcementMapper.selectById(100L);
        assertThat(published.getStatus()).isEqualTo(NoticeAnnouncementStatus.PUBLISHED);
        assertThat(published.getPublishTime()).isNotNull();

        ArgumentCaptor<SendNoticeCommand> sendCaptor = ArgumentCaptor.forClass(SendNoticeCommand.class);
        verify(noticeService).send(sendCaptor.capture());
        SendNoticeCommand reminder = sendCaptor.getValue();
        assertThat(reminder.getBizType()).isEqualTo(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE);
        assertThat(reminder.getBizId()).isEqualTo("100");
        assertThat(reminder.getChannelTypes()).containsExactly(NoticeChannelType.SITE);
        assertThat(reminder.getIdempotentKey()).isEqualTo(ANNOUNCEMENT_PUBLISHED_BIZ_TYPE + ":100");
        assertThat(reminder.getRecipients().stream().map(NoticeRecipientCommand::getUserId))
                .containsExactly(1L, 2L);
    }

    @Test
    void publishAnnouncementRejectsMixedAllTargetWithoutPersistingRecipients() {
        insertDraftAnnouncement(100L, false, true);
        PublishNoticeAnnouncementCommand command = new PublishNoticeAnnouncementCommand();
        command.setTargets(List.of(
                target(NoticeAnnouncementTargetType.ALL, null, "全员"),
                target(NoticeAnnouncementTargetType.ROLE, 10L, "管理员")));

        assertThatThrownBy(() -> announcementService.publishAnnouncement(100L, command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("全员发布不能和组织、角色、用户混用");

        assertThat(recipientResolver.allUsersCalled).isFalse();
        assertThat(countRows("notice_announcement_recipient")).isZero();
        assertThat(announcementMapper.selectById(100L).getStatus()).isEqualTo(NoticeAnnouncementStatus.DRAFT);
    }

    @Test
    void getMyAnnouncementChecksVisibilityBeforeMarkingReadThroughRealMappers() {
        insertPublishedAnnouncement(100L, false);
        jdbcTemplate.update("update notice_announcement set status = 'OFFLINE' where id = 100");
        insertRecipient(1001L, 100L, 8L, NoticeReadStatus.UNREAD, NoticeAnnouncementConfirmStatus.NOT_REQUIRED);

        assertThatThrownBy(() -> announcementService.getMyAnnouncement(100L, 8L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("公告不存在或不可访问");

        assertThat(recipientMapper.selectById(1001L).getReadStatus()).isEqualTo(NoticeReadStatus.UNREAD);
        assertThat(recipientMapper.selectById(1001L).getReadTime()).isNull();
    }

    @Test
    void getMyAnnouncementMarksUnreadRecipientAsReadThroughRealMappers() {
        insertPublishedAnnouncement(100L, false);
        insertRecipient(1001L, 100L, 8L, NoticeReadStatus.UNREAD, NoticeAnnouncementConfirmStatus.NOT_REQUIRED);

        NoticeAnnouncementVO vo = announcementService.getMyAnnouncement(100L, 8L);

        NoticeAnnouncementRecipientEntity persisted = recipientMapper.selectById(1001L);
        assertThat(vo.getReadStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(vo.getReadTime()).isNotNull();
        assertThat(persisted.getReadStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(persisted.getReadTime()).isNotNull();
    }

    @Test
    void confirmMyAnnouncementMarksReadAndConfirmedThroughRealMappers() {
        insertPublishedAnnouncement(100L, true);
        insertRecipient(1001L, 100L, 8L, NoticeReadStatus.UNREAD, NoticeAnnouncementConfirmStatus.PENDING);

        assertThat(announcementService.confirmMyAnnouncement(100L, 8L)).isTrue();

        NoticeAnnouncementRecipientEntity persisted = recipientMapper.selectById(1001L);
        assertThat(persisted.getReadStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(persisted.getReadTime()).isNotNull();
        assertThat(persisted.getConfirmStatus()).isEqualTo(NoticeAnnouncementConfirmStatus.CONFIRMED);
        assertThat(persisted.getConfirmTime()).isNotNull();
    }

    @Test
    void pageMyAnnouncementsUsesJoinedMapperFiltersAndKeepsTotalFromDatabase() {
        insertPublishedAnnouncement(100L, true);
        insertPublishedAnnouncement(101L, true);
        jdbcTemplate.update("update notice_announcement set title = '普通公告', content = '普通内容' where id = 101");
        insertRecipient(1001L, 100L, 8L, NoticeReadStatus.UNREAD, NoticeAnnouncementConfirmStatus.PENDING);
        insertRecipient(1002L, 101L, 8L, NoticeReadStatus.READ, NoticeAnnouncementConfirmStatus.CONFIRMED);
        MyNoticeAnnouncementPageQuery query = new MyNoticeAnnouncementPageQuery();
        query.setUnreadOnly(true);
        query.setPendingConfirmOnly(true);
        query.setKeyword("升级");

        PageResult<NoticeAnnouncementVO> result = announcementService.pageMyAnnouncements(8L, query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getId()).isEqualTo(100L);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists notice_announcement_recipient");
        jdbcTemplate.execute("drop table if exists notice_announcement_target");
        jdbcTemplate.execute("drop table if exists notice_announcement");
        jdbcTemplate.execute("""
                create table notice_announcement (
                    id bigint primary key,
                    title varchar(200) not null,
                    content clob,
                    status varchar(32) not null,
                    publish_time timestamp,
                    valid_start_time timestamp,
                    valid_end_time timestamp,
                    pinned boolean not null default false,
                    confirm_required boolean not null default false,
                    sync_message_enabled boolean not null default true,
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table notice_announcement_target (
                    id bigint primary key,
                    announcement_id bigint not null,
                    target_type varchar(32) not null,
                    target_id bigint,
                    target_name varchar(128),
                    include_children boolean not null default false,
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table notice_announcement_recipient (
                    id bigint primary key,
                    announcement_id bigint not null,
                    user_id bigint not null,
                    read_status varchar(32) not null,
                    read_time timestamp,
                    confirm_status varchar(32) not null,
                    confirm_time timestamp,
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void insertDraftAnnouncement(Long id, boolean confirmRequired, boolean syncMessageEnabled) {
        jdbcTemplate.update("""
                        insert into notice_announcement
                        (id, title, content, status, pinned, confirm_required, sync_message_enabled, created_at)
                        values (?, '系统升级公告', '今晚升级', 'DRAFT', false, ?, ?, current_timestamp)
                        """,
                id, confirmRequired, syncMessageEnabled);
    }

    private void insertPublishedAnnouncement(Long id, boolean confirmRequired) {
        jdbcTemplate.update("""
                        insert into notice_announcement
                        (id, title, content, status, publish_time, pinned, confirm_required, sync_message_enabled,
                         created_at)
                        values (?, '系统升级公告', '今晚升级', 'PUBLISHED', ?, false, ?, true, current_timestamp)
                        """,
                id, LocalDateTime.now().minusHours(1), confirmRequired);
    }

    private void insertRecipient(Long id, Long announcementId, Long userId, NoticeReadStatus readStatus,
                                 NoticeAnnouncementConfirmStatus confirmStatus) {
        jdbcTemplate.update("""
                        insert into notice_announcement_recipient
                        (id, announcement_id, user_id, read_status, confirm_status, created_at)
                        values (?, ?, ?, ?, ?, current_timestamp)
                        """,
                id, announcementId, userId, readStatus.name(), confirmStatus.name());
    }

    private List<String> targetTypes(Long announcementId) {
        return jdbcTemplate.queryForList(
                "select target_type from notice_announcement_target where announcement_id = ? order by id",
                String.class,
                announcementId);
    }

    private List<NoticeAnnouncementRecipientEntity> recipients(Long announcementId) {
        return recipientMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoticeAnnouncementRecipientEntity>()
                .eq(NoticeAnnouncementRecipientEntity::getAnnouncementId, announcementId)
                .orderByAsc(NoticeAnnouncementRecipientEntity::getUserId));
    }

    private Long countRows(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private NoticeAnnouncementTargetCommand target(NoticeAnnouncementTargetType type, Long targetId, String targetName) {
        NoticeAnnouncementTargetCommand command = new NoticeAnnouncementTargetCommand();
        command.setTargetType(type);
        command.setTargetId(targetId);
        command.setTargetName(targetName);
        return command;
    }

    private NoticeRecipientCommand recipient(Long userId, String name) {
        NoticeRecipientCommand command = new NoticeRecipientCommand();
        command.setUserId(userId);
        command.setRecipientName(name);
        return command;
    }

    @Configuration
    @MapperScan(basePackageClasses = NoticeAnnouncementMapper.class)
    @Import(NoticeAnnouncementService.class)
    static class TestConfig {

        @Bean
        TestNoticeRecipientResolver noticeRecipientResolver() {
            return new TestNoticeRecipientResolver();
        }

        @Bean
        INoticeService noticeService() {
            return mock(INoticeService.class);
        }
    }

    static class TestNoticeRecipientResolver extends NoticeRecipientResolver {

        private List<NoticeRecipientCommand> allEnabledUsers = List.of();

        private boolean allUsersCalled;

        TestNoticeRecipientResolver() {
            super(null);
        }

        @Override
        public List<NoticeRecipientCommand> listAllEnabledUsers() {
            allUsersCalled = true;
            return allEnabledUsers;
        }

        void clear() {
            allEnabledUsers = List.of();
            allUsersCalled = false;
        }
    }

}
