package io.mango.notice.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.SendContactCaptchaCommand;
import io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.UpdateCurrentUserContactCommand;
import io.mango.identity.api.command.UpdateCurrentUserProfileCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.IDomainEventPublisher;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxMessageQuery;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.notice.api.command.CompleteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.ExecuteNoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelRouteMode;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionStatus;
import io.mango.notice.api.enums.NoticeSiteMessageCategory;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.channel.wecom.WecomDepartment;
import io.mango.notice.channel.wecom.WecomDirectoryClient;
import io.mango.notice.channel.wecom.WecomDirectoryUser;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeSiteMessageActionEntity;
import io.mango.notice.core.entity.NoticeSiteMessageActionRequestEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.integration.NoticeIdentityGateway;
import io.mango.notice.core.integration.NoticeOrgGateway;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeRecipientMapper;
import io.mango.notice.core.mapper.NoticeSendRecordMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageActionMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageActionRequestMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.outbox.NoticeOutboxMessageFactory;
import io.mango.notice.core.service.DefaultNoticeChannelSecretResolver;
import io.mango.notice.core.service.NoticeChannelSecretMaterializer;
import io.mango.notice.core.service.NoticeRecipientResolver;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.NoticeChannelSender;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateSysOrgCommand;
import io.mango.org.api.command.UpdateOrgMemberCommand;
import io.mango.org.api.command.UpdateSysOrgCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.OrgMemberVO;
import io.mango.org.api.vo.SysOrgVO;

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
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest(
        classes = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            NoticeServiceIntegrationTest.TestConfig.class
        })
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:notice_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "mango.persistence.mybatis-plus.tenant.enabled=false"
        })
class NoticeServiceIntegrationTest {
    private static final String BIZ_TYPE = "job.instance.failed";

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private NoticeService noticeService;

    @Autowired private NoticeBusinessTypeMapper businessTypeMapper;

    @Autowired private NoticeBusinessConfigVersionMapper businessConfigVersionMapper;

    @Autowired private NoticeChannelConfigMapper channelConfigMapper;

    @Autowired private NoticeTaskMapper taskMapper;

    @Autowired private NoticeRecipientMapper recipientMapper;

    @Autowired private NoticeSendRecordMapper sendRecordMapper;

    @Autowired private NoticeSiteMessageMapper siteMessageMapper;

    @Autowired private NoticeSiteMessageActionMapper siteMessageActionMapper;

    @Autowired private NoticeSiteMessageActionRequestMapper siteMessageActionRequestMapper;

    @Autowired private TestOutboxStore outboxStore;

    @Autowired private TestRealtimeApi realtimeApi;

    @Autowired private TestDomainEventPublisher domainEventPublisher;

    @Autowired private TestIdentityUserApi identityUserApi;

    @Autowired private TestEmailChannelSender emailChannelSender;

    @BeforeEach
    void setUp() {
        MangoContextHolder.set(
                MangoContextSnapshot.empty()
                        .withSecurity(
                                1L, "default", "notice-test", null, null, null, null, "test"));
        resetSchema();
        outboxStore.clear();
        realtimeApi.clear();
        domainEventPublisher.clear();
        identityUserApi.clear();
        emailChannelSender.clear();
        identityUserApi.addUser(1L, "张三", "zhangsan@example.com", "13800000001");
        identityUserApi.addUser(2L, "李四", "lisi@example.com", "13800000002");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void sendPersistsTaskRecipientsRecordsAndOutboxThroughRealMappers() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        seedSiteChannelConfig(20L);
        SendNoticeCommand command = sendCommand();

        noticeService.send(command);

        NoticeTaskEntity task = singleTask();
        assertThat(task.getBizType()).isEqualTo(BIZ_TYPE);
        assertThat(task.getBizId()).isEqualTo("job-1001");
        assertThat(task.getChannelTypes()).isEqualTo("SITE");
        assertThat(task.getStatus()).isEqualTo(NoticeTaskStatus.SENDING);
        assertThat(task.getTotalCount()).isEqualTo(2);

        assertThat(recipientMapper.selectCount(null)).isEqualTo(2);
        assertThat(sendRecordMapper.selectList(null))
                .hasSize(2)
                .allSatisfy(
                        record -> {
                            assertThat(record.getTaskId()).isEqualTo(task.getId());
                            assertThat(record.getBusinessChannelTemplateId()).isEqualTo(10L);
                            assertThat(record.getChannelType()).isEqualTo(NoticeChannelType.SITE);
                            assertThat(record.getStatus()).isEqualTo(NoticeSendStatus.PENDING);
                            assertThat(record.getRenderedTitle()).isEqualTo("作业 ETL-01 失败");
                            assertThat(record.getRenderedContent()).isEqualTo("错误码 E500");
                        });

        assertThat(outboxStore.messages).hasSize(1);
        OutboxMessage outbox = outboxStore.messages.get(0);
        assertThat(outbox.getEventType()).isEqualTo(NoticeOutboxMessageFactory.EVENT_TYPE);
        assertThat(outbox.getPayload()).containsEntry("taskId", task.getId());
    }

    @Test
    void sendRejectsMissingRecipientsWithoutLeavingPartialRows() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        SendNoticeCommand command = sendCommand();
        command.setUserIds(List.of());

        assertThatThrownBy(() -> noticeService.send(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("接收用户不能为空");

        assertThat(taskMapper.selectCount(null)).isZero();
        assertThat(recipientMapper.selectCount(null)).isZero();
        assertThat(sendRecordMapper.selectCount(null)).isZero();
        assertThat(outboxStore.messages).isEmpty();
    }

    @Test
    void executeTaskSendsPendingSiteRecordsAndUpdatesTaskThroughRealMappers() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        seedSiteChannelConfig(20L);
        noticeService.send(sendCommand());
        Long taskId = singleTask().getId();

        int successCount = noticeService.executeTask(taskId);

        assertThat(successCount).isEqualTo(2);
        NoticeTaskEntity task = taskMapper.selectById(taskId);
        assertThat(task.getStatus()).isEqualTo(NoticeTaskStatus.SUCCESS);
        assertThat(task.getSuccessCount()).isEqualTo(2);
        assertThat(task.getFailCount()).isZero();
        assertThat(sendRecordMapper.selectList(null))
                .allSatisfy(
                        record -> {
                            assertThat(record.getStatus()).isEqualTo(NoticeSendStatus.SUCCESS);
                            assertThat(record.getSentAt()).isNotNull();
                            assertThat(record.getChannelConfigId()).isEqualTo(20L);
                        });
        assertThat(siteMessageMapper.selectList(null))
                .hasSize(2)
                .allSatisfy(
                        message -> {
                            assertThat(message.getTitle()).isEqualTo("作业 ETL-01 失败");
                            assertThat(message.getContent()).isEqualTo("错误码 E500");
                            assertThat(message.getReadStatus()).isEqualTo(NoticeReadStatus.UNREAD);
                            assertThat(message.getDeleteStatus())
                                    .isEqualTo(NoticeDeleteStatus.NORMAL);
                        });

        NoticeChannelConfigEntity channelConfig = channelConfigMapper.selectById(20L);
        assertThat(channelConfig.getLastSendStatus())
                .isEqualTo(NoticeChannelSendHealthStatus.SUCCESS);
        assertThat(channelConfig.getLastSendTime()).isNotNull();
    }

    @Test
    void executeTaskPersistsStructuredMessageActionsThroughRealMappers() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        seedSiteChannelConfig(20L);
        noticeService.send(interactiveSendCommand());

        noticeService.executeTask(singleTask().getId());

        NoticeTaskEntity task = singleTask();
        assertThat(task.getMessageScene()).isEqualTo("workflow.todo");
        assertThat(task.getMessageTargetKey()).isEqualTo("workflowTaskDetail");
        NoticeSiteMessageEntity message =
                siteMessageMapper
                        .selectList(
                                new LambdaQueryWrapper<NoticeSiteMessageEntity>()
                                        .eq(NoticeSiteMessageEntity::getUserId, 1L))
                        .get(0);
        assertThat(message.getMessageScene()).isEqualTo("workflow.todo");
        assertThat(message.getTargetKey()).isEqualTo("workflowTaskDetail");
        assertThat(message.getTargetParamsJson()).contains("taskId");
        assertThat(
                        siteMessageActionMapper.selectList(
                                new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                                        .eq(
                                                NoticeSiteMessageActionEntity::getMessageId,
                                                message.getId())))
                .hasSize(1)
                .allSatisfy(
                        action -> {
                            assertThat(action.getActionCode()).isEqualTo("approve");
                            assertThat(action.getInteractionType())
                                    .isEqualTo(NoticeSiteMessageActionInteractionType.EVENT);
                            assertThat(action.getEventType())
                                    .isEqualTo("workflow.task.approve.requested");
                            assertThat(action.getStatus())
                                    .isEqualTo(NoticeSiteMessageActionStatus.AVAILABLE);
                        });
    }

    @Test
    void executeSiteMessageActionPublishesDomainEventAndIsIdempotent() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        seedSiteChannelConfig(20L);
        noticeService.send(interactiveSendCommand());
        noticeService.executeTask(singleTask().getId());
        NoticeSiteMessageEntity message =
                siteMessageMapper
                        .selectList(
                                new LambdaQueryWrapper<NoticeSiteMessageEntity>()
                                        .eq(NoticeSiteMessageEntity::getUserId, 1L))
                        .get(0);

        ExecuteNoticeSiteMessageActionCommand executeCommand =
                new ExecuteNoticeSiteMessageActionCommand();
        executeCommand.setMessageId(message.getId());
        executeCommand.setActionCode("approve");
        var first = noticeService.executeSiteMessageAction(executeCommand);
        var second = noticeService.executeSiteMessageAction(executeCommand);

        assertThat(first.getRequestId()).isEqualTo(second.getRequestId());
        assertThat(first.getStatus()).isEqualTo(NoticeSiteMessageActionRequestStatus.REQUESTED);
        assertThat(domainEventPublisher.events).hasSize(1);
        DomainEvent event = domainEventPublisher.events.get(0);
        assertThat(event.getEventType()).isEqualTo("workflow.task.approve.requested");
        assertThat(event.getHeaders()).containsEntry("requestId", first.getRequestId());
        assertThat(event.getPayload()).containsEntry("actionCode", "approve");
        NoticeSiteMessageActionEntity action =
                siteMessageActionMapper.selectOne(
                        new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                                .eq(NoticeSiteMessageActionEntity::getMessageId, message.getId())
                                .eq(NoticeSiteMessageActionEntity::getActionCode, "approve"));
        assertThat(action.getStatus()).isEqualTo(NoticeSiteMessageActionStatus.PROCESSING);
    }

    @Test
    void completeSiteMessageActionUpdatesRequestAndActionStatus() {
        seedBusinessType();
        seedActiveSiteTemplate(10L);
        seedSiteChannelConfig(20L);
        noticeService.send(interactiveSendCommand());
        noticeService.executeTask(singleTask().getId());
        NoticeSiteMessageEntity message =
                siteMessageMapper
                        .selectList(
                                new LambdaQueryWrapper<NoticeSiteMessageEntity>()
                                        .eq(NoticeSiteMessageEntity::getUserId, 1L))
                        .get(0);
        ExecuteNoticeSiteMessageActionCommand executeCommand =
                new ExecuteNoticeSiteMessageActionCommand();
        executeCommand.setMessageId(message.getId());
        executeCommand.setActionCode("approve");
        var request = noticeService.executeSiteMessageAction(executeCommand);
        CompleteNoticeSiteMessageActionCommand command =
                new CompleteNoticeSiteMessageActionCommand();
        command.setRequestId(request.getRequestId());
        command.setStatus(NoticeSiteMessageActionRequestStatus.SUCCEEDED);
        command.setResult(NoticeJsonRequest.of(Map.of("approved", true)));

        var result = noticeService.completeSiteMessageAction(command);

        assertThat(result.getStatus()).isEqualTo(NoticeSiteMessageActionRequestStatus.SUCCEEDED);
        assertThat(result.getResult().toMap()).containsEntry("approved", true);
        NoticeSiteMessageActionRequestEntity persistedRequest =
                siteMessageActionRequestMapper.selectOne(
                        new LambdaQueryWrapper<NoticeSiteMessageActionRequestEntity>()
                                .eq(
                                        NoticeSiteMessageActionRequestEntity::getRequestId,
                                        request.getRequestId()));
        assertThat(persistedRequest.getFinishedAt()).isNotNull();
        NoticeSiteMessageActionEntity action =
                siteMessageActionMapper.selectOne(
                        new LambdaQueryWrapper<NoticeSiteMessageActionEntity>()
                                .eq(NoticeSiteMessageActionEntity::getMessageId, message.getId())
                                .eq(NoticeSiteMessageActionEntity::getActionCode, "approve"));
        assertThat(action.getStatus()).isEqualTo(NoticeSiteMessageActionStatus.SUCCEEDED);
    }

    @Test
    void siteMessageReadAndDeleteOnlyAffectCurrentUsersVisibleRows() {
        MangoContextHolder.set(
                MangoContextSnapshot.empty()
                        .withSecurity(
                                8L, "default", "notice-test", null, null, null, null, "test"));
        insertSiteMessage(100L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL);
        insertSiteMessage(101L, 9L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL);

        assertThat(noticeService.markSiteMessageRead(100L)).isTrue();
        assertThat(noticeService.deleteSiteMessage(100L)).isTrue();
        assertThat(noticeService.markSiteMessageRead(101L)).isFalse();

        NoticeSiteMessageEntity own = siteMessageMapper.selectById(100L);
        NoticeSiteMessageEntity other = siteMessageMapper.selectById(101L);
        assertThat(own.getReadStatus()).isEqualTo(NoticeReadStatus.READ);
        assertThat(own.getReadTime()).isNotNull();
        assertThat(own.getDeleteStatus()).isEqualTo(NoticeDeleteStatus.DELETED);
        assertThat(other.getReadStatus()).isEqualTo(NoticeReadStatus.UNREAD);
        assertThat(other.getDeleteStatus()).isEqualTo(NoticeDeleteStatus.NORMAL);
        assertThat(realtimeApi.messages).hasSize(1);
        assertThat(realtimeApi.messages.get(0).content()).contains("\"unreadCount\":0");
    }

    @Test
    void unreadCategoryStatsAndPageFilterUseTheSameVisibleMessageRules() {
        MangoContextHolder.set(
                MangoContextSnapshot.empty()
                        .withSecurity(
                                8L, "default", "notice-test", null, null, null, null, "test"));
        insertBusinessType(11L, "workflow.task.assigned", "WORKFLOW");
        insertBusinessType(12L, "identity.password.expired", "IDENTITY");
        insertBusinessType(13L, "order.paid", "ORDER");
        insertSiteMessage(110L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL,
                "workflow.task.assigned");
        insertSiteMessage(111L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL,
                "identity.password.expired");
        insertSiteMessage(112L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL,
                "order.paid");
        insertSiteMessage(113L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL, null);
        insertSiteMessage(114L, 8L, NoticeReadStatus.READ, NoticeDeleteStatus.NORMAL,
                "workflow.task.assigned");
        insertSiteMessage(115L, 8L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.DELETED,
                "identity.password.expired");
        insertSiteMessage(116L, 9L, NoticeReadStatus.UNREAD, NoticeDeleteStatus.NORMAL,
                "workflow.task.assigned");

        var stats = noticeService.unreadCategoryStats();

        assertThat(stats.getTotal()).isEqualTo(4L);
        assertThat(stats.getCategories())
                .extracting(item -> item.getCategory() + "=" + item.getCount())
                .containsExactly("APPROVAL=1", "SYSTEM=1", "BUSINESS=2");

        NoticeSiteMessagePageQuery query = new NoticeSiteMessagePageQuery();
        query.setUnreadOnly(true);
        query.setCategory(NoticeSiteMessageCategory.BUSINESS);
        query.setPageSize(10);
        var page = noticeService.listSiteMessages(query);

        assertThat(page.getTotal()).isEqualTo(2L);
        assertThat(page.getList())
                .extracting(item -> item.getId())
                .containsExactlyInAnyOrder(112L, 113L);
    }

    @Test
    void publishBusinessConfigDraftMovesActiveToHistoryAndUpdatesBusinessTypeThroughRealMappers() {
        seedBusinessType();
        insertBusinessConfigVersion(
                100L,
                1,
                NoticeTemplateVersionStatus.ACTIVE,
                "{\"old\":true}",
                NoticePriority.NORMAL,
                "old-key");
        SaveNoticeBusinessConfigCommand command = new SaveNoticeBusinessConfigCommand();
        command.setParamsSchema("{\"required\":[\"jobName\"]}");
        command.setDefaultPriority(NoticePriority.HIGH);
        command.setIdempotentStrategy("biz-id");
        noticeService.saveBusinessConfigDraft(1L, command);

        assertThat(noticeService.publishBusinessConfigDraft(1L)).isTrue();

        NoticeBusinessTypeEntity businessType = businessTypeMapper.selectById(1L);
        assertThat(businessType.getParamsSchema()).isEqualTo("{\"required\":[\"jobName\"]}");
        assertThat(businessType.getDefaultPriority()).isEqualTo(NoticePriority.HIGH);
        assertThat(businessType.getIdempotentStrategy()).isEqualTo("biz-id");
        assertThat(configVersionsByStatus(NoticeTemplateVersionStatus.ACTIVE))
                .singleElement()
                .satisfies(
                        active -> {
                            assertThat(active.getVersion()).isEqualTo(2);
                            assertThat(active.getPublishTime()).isNotNull();
                        });
        assertThat(configVersionsByStatus(NoticeTemplateVersionStatus.HISTORY))
                .singleElement()
                .extracting(NoticeBusinessConfigVersionEntity::getVersion)
                .isEqualTo(1);
    }

    @Test
    void saveChannelConfigKeepsOriginalSecretWhenMaskedValueIsSubmitted() {
        insertEmailChannelConfig(
                30L,
                "{\"host\":\"smtp.example.com\",\"username\":\"mango\","
                        + "\"smtpPassword\":\"old-secret\",\"from\":\"notice@example.com\"}");
        SaveNoticeChannelConfigCommand command = new SaveNoticeChannelConfigCommand();
        command.setId(30L);
        command.setChannelType(NoticeChannelType.EMAIL);
        command.setProviderCode("SMTP");
        command.setConfigName("SMTP");
        command.setConfigJson(
                "{\"host\":\"smtp.example.com\",\"username\":\"mango\","
                        + "\"smtpPassword\":\"***\",\"from\":\"notice@example.com\"}");
        command.setEnabled(true);

        noticeService.saveChannelConfig(command);

        NoticeChannelConfigEntity persisted = channelConfigMapper.selectById(30L);
        assertThat(persisted.getConfigStatus()).isEqualTo(NoticeChannelConfigStatus.COMPLETE);
        assertThat(persisted.getConfigJson()).doesNotContain("smtpPassword", "old-secret", "***");
        assertThat(persisted.getSecretConfigJson()).contains("\"smtpPassword\":\"old-secret\"");
        assertThat(persisted.getSecretConfigJson()).doesNotContain("\"smtpPassword\":\"***\"");
    }

    @Test
    void tagRouteWithoutCandidateFailsWithoutFallingBackToAuto() {
        seedBusinessType();
        seedSiteChannelConfig(20L);
        seedActiveEmailTemplate(40L, NoticeChannelRouteMode.TAG, null, "PRIMARY");
        insertEmailChannelConfig(
                41L,
                "{\"host\":\"smtp.example.com\",\"username\":\"mango\","
                        + "\"password\":\"secret\",\"from\":\"notice@example.com\"}");
        jdbcTemplate.update(
                "insert into notice_channel_route_tag (id, channel_type, tag_code, tag_name) "
                        + "values (50, 'EMAIL', 'PRIMARY', '主通道')");
        SendNoticeCommand command = sendCommand();
        command.setUserIds(List.of(1L));
        command.setChannelTypes(List.of(NoticeChannelType.EMAIL));

        noticeService.send(command);
        noticeService.executeTask(singleTask().getId());

        assertThat(emailRecord().getFailCode()).isEqualTo("CHANNEL_ROUTE_TAG_UNAVAILABLE");
        assertThat(emailRecord().getChannelConfigId()).isNull();
        assertThat(emailChannelSender.configIds).isEmpty();
    }

    @Test
    void autoRouteRetriesThenUsesNextPriorityAccount() {
        seedBusinessType();
        seedSiteChannelConfig(20L);
        seedActiveEmailTemplate(40L, NoticeChannelRouteMode.AUTO, null, null);
        insertEmailChannelConfig(
                41L,
                "{\"host\":\"smtp-1.example.com\",\"username\":\"mango\","
                        + "\"password\":\"secret\",\"from\":\"notice@example.com\"}");
        insertEmailChannelConfig(
                42L,
                "{\"host\":\"smtp-2.example.com\",\"username\":\"mango\","
                        + "\"password\":\"secret\",\"from\":\"notice@example.com\"}");
        jdbcTemplate.update("update notice_channel_config set priority = 0 where id = 41");
        jdbcTemplate.update("update notice_channel_config set priority = 1 where id = 42");
        emailChannelSender.results.put(
                41L, ChannelSendResult.failed("SMTP_TEMPORARY", "temporary", true));
        SendNoticeCommand command = sendCommand();
        command.setUserIds(List.of(1L));
        command.setChannelTypes(List.of(NoticeChannelType.EMAIL));

        noticeService.send(command);
        noticeService.executeTask(singleTask().getId());

        assertThat(emailChannelSender.configIds).containsExactly(41L, 41L, 41L, 42L);
        assertThat(emailRecord().getStatus()).isEqualTo(NoticeSendStatus.SUCCESS);
        assertThat(emailRecord().getChannelConfigId()).isEqualTo(42L);
    }

    @Test
    void nonRetryableRouteFailureDoesNotSwitchIdentity() {
        seedBusinessType();
        seedSiteChannelConfig(20L);
        seedActiveEmailTemplate(40L, NoticeChannelRouteMode.AUTO, null, null);
        insertEmailChannelConfig(
                41L,
                "{\"host\":\"smtp-1.example.com\",\"username\":\"mango\","
                        + "\"password\":\"secret\",\"from\":\"notice@example.com\"}");
        insertEmailChannelConfig(
                42L,
                "{\"host\":\"smtp-2.example.com\",\"username\":\"mango\","
                        + "\"password\":\"secret\",\"from\":\"notice@example.com\"}");
        jdbcTemplate.update("update notice_channel_config set priority = 0 where id = 41");
        jdbcTemplate.update("update notice_channel_config set priority = 1 where id = 42");
        emailChannelSender.results.put(
                41L, ChannelSendResult.failed("CHANNEL_CONFIG_INVALID", "invalid", false));
        SendNoticeCommand command = sendCommand();
        command.setUserIds(List.of(1L));
        command.setChannelTypes(List.of(NoticeChannelType.EMAIL));

        noticeService.send(command);
        noticeService.executeTask(singleTask().getId());

        assertThat(emailChannelSender.configIds).containsExactly(41L);
        assertThat(emailRecord().getStatus()).isEqualTo(NoticeSendStatus.FAILED);
        assertThat(emailRecord().getChannelConfigId()).isEqualTo(41L);
    }

    private SendNoticeCommand sendCommand() {
        SendNoticeCommand command = new SendNoticeCommand();
        command.setBizType(BIZ_TYPE);
        command.setBizId("job-1001");
        command.setUserIds(List.of(1L, 2L));
        command.setChannelTypes(List.of(NoticeChannelType.SITE));
        command.setParams(NoticeJsonRequest.of(Map.of("jobName", "ETL-01", "errorCode", "E500")));
        return command;
    }

    private SendNoticeCommand interactiveSendCommand() {
        SendNoticeCommand command = sendCommand();
        command.setUserIds(List.of(1L));
        command.setMessageScene("workflow.todo");
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType("workflowTask");
        subject.setSubjectId("task-1001");
        subject.setSubjectName("审批任务");
        command.setMessageSubject(subject);
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        target.setTargetKey("workflowTaskDetail");
        target.setParams(NoticeJsonRequest.of(Map.of("taskId", "task-1001")));
        command.setMessageTarget(target);
        command.setMessageData(NoticeJsonRequest.of(Map.of("processInstanceId", "pi-1001")));
        NoticeSiteMessageActionCommand action = new NoticeSiteMessageActionCommand();
        action.setActionCode("approve");
        action.setActionLabel("同意");
        action.setInteractionType(NoticeSiteMessageActionInteractionType.EVENT);
        action.setEventType("workflow.task.approve.requested");
        action.setConfirmRequired(true);
        command.setMessageActions(List.of(action));
        return command;
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists notice_site_message_action_request");
        jdbcTemplate.execute("drop table if exists notice_site_message_action");
        jdbcTemplate.execute("drop table if exists notice_site_message");
        jdbcTemplate.execute("drop table if exists notice_send_record");
        jdbcTemplate.execute("drop table if exists notice_recipient");
        jdbcTemplate.execute("drop table if exists notice_task");
        jdbcTemplate.execute("drop table if exists notice_receive_preference");
        jdbcTemplate.execute("drop table if exists notice_recipient_account");
        jdbcTemplate.execute("drop table if exists notice_channel_config_route_tag");
        jdbcTemplate.execute("drop table if exists notice_channel_route_tag");
        jdbcTemplate.execute("drop table if exists notice_channel_config");
        jdbcTemplate.execute("drop table if exists notice_business_channel_template");
        jdbcTemplate.execute("drop table if exists notice_business_config_version");
        jdbcTemplate.execute("drop table if exists notice_business_type");
        jdbcTemplate.execute("drop table if exists notice_setting");
        jdbcTemplate.execute("drop table if exists notice_wecom_sync_mapping");
        createBusinessTypeTable();
        createBusinessConfigVersionTable();
        createBusinessChannelTemplateTable();
        createChannelConfigTable();
        createChannelRouteTagTable();
        createChannelConfigRouteTagTable();
        createRecipientAccountTable();
        createReceivePreferenceTable();
        createTaskTable();
        createRecipientTable();
        createSendRecordTable();
        createSiteMessageTable();
        createSiteMessageActionTable();
        createSiteMessageActionRequestTable();
        createSettingTable();
        createWecomSyncMappingTable();
    }

    private void createBusinessTypeTable() {
        jdbcTemplate.execute(
                """
                create table notice_business_type (
                    id bigint generated by default as identity primary key,
                    biz_type varchar(128),
                    biz_name varchar(128),
                    biz_group varchar(128),
                    domain_code varchar(128),
                    description varchar(512),
                    params_schema clob,
                    enabled boolean,
                    default_priority varchar(32),
                    idempotent_strategy varchar(128),
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createBusinessConfigVersionTable() {
        jdbcTemplate.execute(
                """
                create table notice_business_config_version (
                    id bigint generated by default as identity primary key,
                    business_type_id bigint,
                    biz_type varchar(128),
                    params_schema clob,
                    default_priority varchar(32),
                    idempotent_strategy varchar(128),
                    version int,
                    version_status varchar(32),
                    publish_time timestamp,
                    publish_by bigint,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createBusinessChannelTemplateTable() {
        jdbcTemplate.execute(
                """
                create table notice_business_channel_template (
                    id bigint generated by default as identity primary key,
                    business_type_id bigint,
                    biz_type varchar(128),
                    channel_type varchar(32),
                    template_name varchar(128),
                    title_template varchar(512),
                    content_template clob,
                    channel_template_id varchar(128),
                    variable_mapping clob,
                    version int,
                    version_status varchar(32),
                    enabled boolean,
                    channel_config_id bigint,
                    route_mode varchar(16),
                    route_tag_code varchar(64),
                    publish_time timestamp,
                    publish_by bigint,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createChannelConfigTable() {
        jdbcTemplate.execute(
                """
                create table notice_channel_config (
                    id bigint generated by default as identity primary key,
                    config_code varchar(128),
                    channel_type varchar(32),
                    provider_code varchar(64),
                    config_name varchar(128),
                    config_json clob,
                    secret_refs_json clob,
                    secret_config_json clob,
                    resource_id varchar(128),
                    resource_version bigint,
                    resource_module_code varchar(128),
                    resource_source varchar(32),
                    managed_fields_json clob,
                    secret_status varchar(32),
                    enabled boolean,
                    priority int,
                    weight int,
                    config_status varchar(32),
                    last_send_status varchar(32),
                    last_send_time timestamp,
                    last_failure_code varchar(128),
                    last_failure_reason varchar(512),
                    rate_limit_config clob,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createChannelRouteTagTable() {
        jdbcTemplate.execute(
                """
                create table notice_channel_route_tag (
                    id bigint generated by default as identity primary key,
                    channel_type varchar(32),
                    tag_code varchar(64),
                    tag_name varchar(128),
                    description varchar(512),
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createChannelConfigRouteTagTable() {
        jdbcTemplate.execute(
                """
                create table notice_channel_config_route_tag (
                    id bigint generated by default as identity primary key,
                    channel_config_id bigint,
                    route_tag_id bigint,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createRecipientAccountTable() {
        jdbcTemplate.execute(
                """
                create table notice_recipient_account (
                    id bigint generated by default as identity primary key,
                    user_id bigint,
                    account_type varchar(32),
                    account_value varchar(256),
                    display_name varchar(128),
                    verified_status varchar(32),
                    default_account boolean,
                    enabled boolean,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createReceivePreferenceTable() {
        jdbcTemplate.execute(
                """
                create table notice_receive_preference (
                    id bigint generated by default as identity primary key,
                    user_id bigint,
                    scope_type varchar(32),
                    scope_value varchar(128),
                    channel_type varchar(32),
                    enabled boolean,
                    account_id bigint,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createTaskTable() {
        jdbcTemplate.execute(
                """
                create table notice_task (
                    id bigint generated by default as identity primary key,
                    task_code varchar(64),
                    biz_type varchar(128),
                    biz_id varchar(128),
                    idempotent_key varchar(128),
                    params_snapshot clob,
                    recipient_targets_snapshot clob,
                    channel_types varchar(256),
                    message_scene varchar(64),
                    message_subject_type varchar(64),
                    message_subject_id varchar(128),
                    message_subject_name varchar(128),
                    message_target_type varchar(32),
                    message_target_key varchar(128),
                    message_target_params_json clob,
                    message_target_open_mode varchar(32),
                    message_data_json clob,
                    message_actions_json clob,
                    message_expire_time timestamp,
                    send_mode varchar(32),
                    scheduled_time timestamp,
                    status varchar(32),
                    total_count int,
                    success_count int,
                    fail_count int,
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createRecipientTable() {
        jdbcTemplate.execute(
                """
                create table notice_recipient (
                    id bigint generated by default as identity primary key,
                    task_id bigint,
                    user_id bigint,
                    recipient_name varchar(128),
                    mobile varchar(64),
                    email varchar(256),
                    wechat_openid varchar(128),
                    wecom_user_id varchar(128),
                    dingtalk_user_id varchar(128),
                    external_id varchar(128),
                    tenant_id varchar(64),
                    created_at timestamp default current_timestamp
                )
                """);
    }

    private void createSendRecordTable() {
        jdbcTemplate.execute(
                """
                create table notice_send_record (
                    id bigint generated by default as identity primary key,
                    task_id bigint,
                    recipient_id bigint,
                    biz_type varchar(128),
                    biz_id varchar(128),
                    business_channel_template_id bigint,
                    template_version int,
                    channel_type varchar(32),
                    channel_config_id bigint,
                    request_id varchar(64),
                    provider_message_id varchar(128),
                    status varchar(32),
                    rendered_title varchar(512),
                    rendered_content clob,
                    request_snapshot clob,
                    response_snapshot clob,
                    fail_code varchar(128),
                    fail_reason varchar(512),
                    retry_count int,
                    next_retry_time timestamp,
                    sent_at timestamp,
                    tenant_id varchar(64),
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createSiteMessageTable() {
        jdbcTemplate.execute(
                """
                create table notice_site_message (
                    id bigint generated by default as identity primary key,
                    task_id bigint,
                    send_record_id bigint,
                    user_id bigint,
                    title varchar(512),
                    content clob,
                    message_scene varchar(64),
                    subject_type varchar(64),
                    subject_id varchar(128),
                    subject_name varchar(128),
                    target_type varchar(32),
                    target_key varchar(128),
                    target_params_json clob,
                    target_open_mode varchar(32),
                    data_json clob,
                    expire_time timestamp,
                    priority varchar(32),
                    read_status varchar(32),
                    read_time timestamp,
                    delete_status varchar(32),
                    revoke_status boolean,
                    top_status boolean,
                    biz_type varchar(128),
                    biz_id varchar(128),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp,
                    tenant_id varchar(64)
                )
                """);
    }

    private void createSiteMessageActionTable() {
        jdbcTemplate.execute(
                """
                create table notice_site_message_action (
                    id bigint generated by default as identity primary key,
                    message_id bigint,
                    action_code varchar(64),
                    action_label varchar(64),
                    interaction_type varchar(32),
                    event_type varchar(128),
                    target_type varchar(32),
                    target_key varchar(128),
                    target_params_json clob,
                    target_open_mode varchar(32),
                    confirm_required boolean,
                    input_schema clob,
                    status varchar(32),
                    failure_reason varchar(512),
                    sort_order int,
                    expire_time timestamp,
                    tenant_id varchar(64),
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createSiteMessageActionRequestTable() {
        jdbcTemplate.execute(
                """
                create table notice_site_message_action_request (
                    id bigint generated by default as identity primary key,
                    message_id bigint,
                    action_id bigint,
                    action_code varchar(64),
                    actor_user_id bigint,
                    request_id varchar(160),
                    input_json clob,
                    status varchar(32),
                    fail_code varchar(64),
                    fail_reason varchar(512),
                    result_json clob,
                    event_id varchar(64),
                    tenant_id varchar(64),
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp,
                    finished_at timestamp
                )
                """);
    }

    private void createSettingTable() {
        jdbcTemplate.execute(
                """
                create table notice_setting (
                    id bigint generated by default as identity primary key,
                    setting_key varchar(128),
                    setting_value varchar(512),
                    tenant_id varchar(64),
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void createWecomSyncMappingTable() {
        jdbcTemplate.execute(
                """
                create table notice_wecom_sync_mapping (
                    id bigint generated by default as identity primary key,
                    sync_type varchar(32),
                    external_id varchar(128),
                    local_id bigint,
                    data_hash varchar(128),
                    display_name varchar(128),
                    tenant_id varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    private void seedBusinessType() {
        jdbcTemplate.update(
                """
                insert into notice_business_type
                (id, biz_type, biz_name, biz_group, domain_code, enabled, default_priority, created_at, updated_at)
                values (1, ?, '作业失败', '运维', 'ops', true, 'NORMAL', current_timestamp, current_timestamp)
                """,
                BIZ_TYPE);
    }

    private void insertBusinessType(Long id, String bizType, String bizGroup) {
        jdbcTemplate.update(
                """
                insert into notice_business_type
                (id, biz_type, biz_name, biz_group, domain_code, enabled, default_priority, created_at, updated_at)
                values (?, ?, ?, ?, 'test', true, 'NORMAL', current_timestamp, current_timestamp)
                """,
                id,
                bizType,
                bizType,
                bizGroup);
    }

    private void seedActiveSiteTemplate(Long id) {
        jdbcTemplate.update(
                """
                insert into notice_business_channel_template
                (id, business_type_id, biz_type, channel_type, template_name, title_template, content_template,
                 version, version_status, enabled, channel_config_id, publish_time, created_at, updated_at)
                values (?, 1, ?, 'SITE', '站内信模板', '作业 {{ jobName }} 失败', '错误码 {{errorCode}}',
                 1, 'ACTIVE', true, 20, current_timestamp, current_timestamp, current_timestamp)
                """,
                id,
                BIZ_TYPE);
    }

    private void seedActiveEmailTemplate(
            Long id, NoticeChannelRouteMode routeMode, Long configId, String routeTagCode) {
        jdbcTemplate.update(
                """
                insert into notice_business_channel_template
                (id, business_type_id, biz_type, channel_type, template_name, title_template, content_template,
                 version, version_status, enabled, channel_config_id, route_mode, route_tag_code,
                 publish_time, created_at, updated_at)
                values (?, 1, ?, 'EMAIL', '邮件模板', '作业 {{ jobName }} 失败', '错误码 {{errorCode}}',
                 1, 'ACTIVE', true, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                id,
                BIZ_TYPE,
                configId,
                routeMode.name(),
                routeTagCode);
    }

    private void seedSiteChannelConfig(Long id) {
        jdbcTemplate.update(
                """
                insert into notice_channel_config
                (id, config_code, channel_type, provider_code, config_name, config_json, resource_source,
                 secret_status, enabled, priority, weight, config_status, last_send_status, created_at, updated_at)
                values (?, 'SITE_INTERNAL', 'SITE', 'INTERNAL', '站内信', '{}', 'MANUAL',
                 'NOT_REQUIRED', true, 1, 100, 'COMPLETE', 'NONE', current_timestamp, current_timestamp)
                """,
                id);
    }

    private void insertBusinessConfigVersion(
            Long id,
            Integer version,
            NoticeTemplateVersionStatus status,
            String paramsSchema,
            NoticePriority priority,
            String idempotentStrategy) {
        jdbcTemplate.update(
                """
                insert into notice_business_config_version
                (id, business_type_id, biz_type, params_schema, default_priority, idempotent_strategy,
                 version, version_status, publish_time, created_at, updated_at)
                values (?, 1, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, current_timestamp)
                """,
                id,
                BIZ_TYPE,
                paramsSchema,
                priority.name(),
                idempotentStrategy,
                version,
                status.name());
    }

    private void insertEmailChannelConfig(Long id, String configJson) {
        jdbcTemplate.update(
                """
                insert into notice_channel_config
                (id, config_code, channel_type, provider_code, config_name, config_json, resource_source,
                 secret_status, enabled, priority, weight, config_status, last_send_status, created_at, updated_at)
                values (?, concat('EMAIL_', ?), 'EMAIL', 'SMTP', 'SMTP', ?, 'MANUAL',
                 'COMPLETE', true, 1, 100, 'COMPLETE', 'NONE', current_timestamp, current_timestamp)
                """,
                id,
                id,
                configJson);
    }

    private void insertSiteMessage(
            Long id, Long userId, NoticeReadStatus readStatus, NoticeDeleteStatus deleteStatus) {
        insertSiteMessage(id, userId, readStatus, deleteStatus, BIZ_TYPE);
    }

    private void insertSiteMessage(
            Long id,
            Long userId,
            NoticeReadStatus readStatus,
            NoticeDeleteStatus deleteStatus,
            String bizType) {
        jdbcTemplate.update(
                """
                insert into notice_site_message
                (id, user_id, title, content, priority, read_status, delete_status, revoke_status, top_status,
                 biz_type, biz_id, created_at, updated_at)
                values (?, ?, '标题', '内容', 'NORMAL', ?, ?, false, false, ?, 'biz-1',
                 current_timestamp, current_timestamp)
                """,
                id,
                userId,
                readStatus.name(),
                deleteStatus.name(),
                bizType);
    }

    private NoticeTaskEntity singleTask() {
        return taskMapper
                .selectList(
                        new LambdaQueryWrapper<NoticeTaskEntity>()
                                .orderByAsc(NoticeTaskEntity::getId))
                .get(0);
    }

    private io.mango.notice.core.entity.NoticeSendRecordEntity emailRecord() {
        return sendRecordMapper.selectOne(
                new LambdaQueryWrapper<io.mango.notice.core.entity.NoticeSendRecordEntity>()
                        .eq(
                                io.mango.notice.core.entity.NoticeSendRecordEntity::getChannelType,
                                NoticeChannelType.EMAIL)
                        .last("limit 1"));
    }

    private List<NoticeBusinessConfigVersionEntity> configVersionsByStatus(
            NoticeTemplateVersionStatus status) {
        return businessConfigVersionMapper.selectList(
                new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
                        .eq(NoticeBusinessConfigVersionEntity::getBizType, BIZ_TYPE)
                        .eq(NoticeBusinessConfigVersionEntity::getVersionStatus, status));
    }

    @Configuration
    @MapperScan(basePackageClasses = NoticeBusinessChannelTemplateMapper.class)
    @Import({
        NoticeService.class,
        NoticeDeliveryService.class,
        NoticeConfigurationService.class,
        NoticeChannelSecretMaterializer.class,
        DefaultNoticeChannelSecretResolver.class,
        NoticeRecordOperationService.class,
        NoticeRecipientSettingService.class,
        NoticeSiteMessageService.class,
        NoticeWecomSyncService.class,
        NoticeSiteMessageWriterImpl.class,
        NoticeIdentityGateway.class,
        NoticeOrgGateway.class
    })
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        TestOutboxStore outboxStore() {
            return new TestOutboxStore();
        }

        @Bean
        TestRealtimeApi realtimeApi() {
            return new TestRealtimeApi();
        }

        @Bean
        TestDomainEventPublisher domainEventPublisher() {
            return new TestDomainEventPublisher();
        }

        @Bean
        TestIdentityUserApi identityUserApi() {
            return new TestIdentityUserApi();
        }

        @Bean
        NoticeRecipientResolver noticeRecipientResolver(IdentityUserApi identityUserApi) {
            return new NoticeRecipientResolver(identityUserApi);
        }

        @Bean
        SysOrgApi sysOrgApi() {
            return new NoopSysOrgApi();
        }

        @Bean
        WecomDirectoryClient wecomDirectoryClient() {
            return new NoopWecomDirectoryClient();
        }

        @Bean
        NoticeChannelSender siteChannelSender(NoticeSiteMessageWriterImpl messageWriter) {
            return new TestSiteChannelSender(messageWriter);
        }

        @Bean
        TestEmailChannelSender emailChannelSender() {
            return new TestEmailChannelSender();
        }
    }

    static class TestSiteChannelSender implements NoticeChannelSender {
        private final NoticeSiteMessageWriterImpl messageWriter;

        TestSiteChannelSender(NoticeSiteMessageWriterImpl messageWriter) {
            this.messageWriter = messageWriter;
        }

        @Override
        public NoticeChannelType channelType() {
            return NoticeChannelType.SITE;
        }

        @Override
        public ChannelSendResult send(NoticeChannelMessage command) {
            return ChannelSendResult.success(messageWriter.write(command).messageId());
        }
    }

    static class TestEmailChannelSender implements NoticeChannelSender {
        private final List<Long> configIds = new ArrayList<>();
        private final Map<Long, ChannelSendResult> results = new HashMap<>();

        @Override
        public NoticeChannelType channelType() {
            return NoticeChannelType.EMAIL;
        }

        @Override
        public ChannelSendResult send(NoticeChannelMessage command) {
            configIds.add(command.getChannelConfigId());
            return results.getOrDefault(
                    command.getChannelConfigId(),
                    ChannelSendResult.providerSuccess("email-message", "{\"status\":\"SENT\"}"));
        }

        void clear() {
            configIds.clear();
            results.clear();
        }
    }

    static class TestRealtimeApi implements RealtimeApi {
        private final List<RealtimeOutboundMessage> messages = new ArrayList<>();

        @Override
        public void publish(RealtimeOutboundMessage realtimeOutboundMessage) {
            messages.add(realtimeOutboundMessage);
        }

        void clear() {
            messages.clear();
        }
    }

    static class TestDomainEventPublisher implements IDomainEventPublisher {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }

        void clear() {
            events.clear();
        }
    }

    static class TestOutboxStore implements IOutboxStore {
        private final List<OutboxMessage> messages = new ArrayList<>();

        @Override
        public void enqueue(OutboxMessage message) {
            messages.add(message);
        }

        @Override
        public List<OutboxMessage> claim(String workerId, int batchSize, Instant now) {
            return List.of();
        }

        @Override
        public List<OutboxMessage> claimByTopic(
                String workerId, String topic, int batchSize, Instant now) {
            return List.of();
        }

        @Override
        public void ack(String messageId, String workerId, Instant now) {}

        @Override
        public void nack(
                String messageId,
                String workerId,
                String errorMessage,
                Instant nextAttemptAt,
                Instant now) {}

        @Override
        public List<OutboxMessage> query(OutboxMessageQuery query) {
            return List.copyOf(messages);
        }

        void clear() {
            messages.clear();
        }
    }

    static class TestIdentityUserApi implements IdentityUserApi {
        private final Map<Long, IdentityUserInfoVO> users = new HashMap<>();

        @Override
        public R<CurrentUserProfileVO> currentProfile() {
            return R.ok(null);
        }

        @Override
        public R<CurrentUserProfileVO> updateCurrentProfile(
                UpdateCurrentUserProfileCommand command) {
            return R.ok(null);
        }

        @Override
        public R<ContactCaptchaTicketVO> sendCurrentContactCaptcha(
                SendContactCaptchaCommand command) {
            return R.ok(null);
        }

        @Override
        public R<CurrentUserProfileVO> updateCurrentContact(
                UpdateCurrentUserContactCommand command) {
            return R.ok(null);
        }

        void addUser(Long id, String nickname, String email, String phone) {
            IdentityUserInfoVO info = new IdentityUserInfoVO();
            info.setUserId(id);
            info.setUsername("user" + id);
            info.setNickname(nickname);
            info.setEmail(email);
            info.setPhone(phone);
            info.setStatus(1);
            users.put(id, info);
        }

        void clear() {
            users.clear();
        }

        @Override
        public R<PageResult<IdentityUserVO>> page(IdentityUserPageQuery query) {
            return R.ok(PageResult.of(List.of(), 0, query.getPage(), query.getSize()));
        }

        @Override
        public R<IdentityUserVO> detail(Long userId) {
            return R.ok(null);
        }

        @Override
        public R<Long> create(CreateIdentityUserCommand command) {
            return R.ok(1L);
        }

        @Override
        public R<Boolean> update(UpdateIdentityUserCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> delete(Long userId) {
            return R.ok(true);
        }

        @Override
        public R<Integer> deleteBatch(BatchDeleteIdentityUserCommand command) {
            return R.ok(0);
        }

        @Override
        public R<Boolean> updateStatus(UpdateIdentityUserStatusCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> resetPassword(ResetIdentityUserPasswordCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> unlock(UnlockIdentityUserCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> requirePasswordReset(RequireIdentityUserPasswordResetCommand command) {
            return R.ok(true);
        }

        @Override
        public R<IdentityUserInfoVO> getUserInfo(String username) {
            return R.ok(null);
        }

        @Override
        public R<IdentityUserInfoVO> getUserInfoById(Long userId) {
            return R.ok(users.get(userId));
        }

        @Override
        public R<List<IdentityUserInfoVO>> listUserInfosByTarget(IdentityUserTargetQuery query) {
            return R.ok(List.of());
        }

        @Override
        public R<ExternalIdentityBindingVO> bindExternalIdentity(
                BindExternalIdentityCommand command) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> unbindExternalIdentity(UnbindExternalIdentityCommand command) {
            return R.ok(true);
        }

        @Override
        public R<ExternalIdentityBindingVO> findExternalIdentity(ExternalIdentityQuery query) {
            return R.ok(null);
        }

        @Override
        public R<List<ExternalIdentityBindingVO>> listExternalIdentities(Long userId) {
            return R.ok(List.of());
        }

        @Override
        public R<List<ExternalIdentityBindingVO>> listCurrentExternalIdentities() {
            return R.ok(List.of());
        }

        @Override
        public R<Boolean> unbindCurrentExternalIdentity(
                UnbindCurrentExternalIdentityCommand command) {
            return R.ok(true);
        }
    }

    static class NoopSysOrgApi implements SysOrgApi {
        @Override
        public R<List<SysOrgVO>> tree(SysOrgTreeQuery query) {
            return R.ok(List.of());
        }

        @Override
        public R<List<SysOrgVO>> children(Long parentId) {
            return R.ok(List.of());
        }

        @Override
        public R<SysOrgVO> getById(Long id) {
            return R.ok(null);
        }

        @Override
        public R<Long> create(CreateSysOrgCommand command) {
            return R.ok(1L);
        }

        @Override
        public R<Boolean> update(UpdateSysOrgCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> delete(Long id) {
            return R.ok(true);
        }

        @Override
        public R<List<OrgMemberVO>> members(Long orgId) {
            return R.ok(List.of());
        }

        @Override
        public R<Boolean> addMember(AddOrgMemberCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> updateMember(UpdateOrgMemberCommand command) {
            return R.ok(true);
        }

        @Override
        public R<Boolean> removeMember(Long relationId) {
            return R.ok(true);
        }

        @Override
        public R<List<Long>> leaderUserIds(Long orgId) {
            return R.ok(List.of());
        }
    }

    static class NoopWecomDirectoryClient implements WecomDirectoryClient {
        @Override
        public List<WecomDirectoryUser> listUsers(String corpId, String secret) {
            return List.of();
        }

        @Override
        public List<WecomDirectoryUser> listUsers(
                String corpId, String secret, Long departmentId, boolean fetchChild) {
            return List.of();
        }

        @Override
        public List<WecomDepartment> listDepartments(
                String corpId, String secret, Long departmentId) {
            return List.of();
        }
    }
}
