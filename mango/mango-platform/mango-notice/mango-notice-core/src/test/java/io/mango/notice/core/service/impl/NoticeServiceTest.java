package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.R;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.mango.notice.api.enums.NoticeRecipientTargetType;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeSendCancelCode;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeReceivePreferenceEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
import io.mango.notice.core.entity.NoticeSendRecordEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeReceivePreferenceMapper;
import io.mango.notice.core.mapper.NoticeRecipientAccountMapper;
import io.mango.notice.core.mapper.NoticeRecipientMapper;
import io.mango.notice.core.mapper.NoticeSendRecordMapper;
import io.mango.notice.core.mapper.NoticeSettingMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.mapper.NoticeWecomSyncMappingMapper;
import io.mango.notice.channel.wecom.WecomDirectoryClient;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import io.mango.org.api.SysOrgApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.mango.notice.api.enums.NoticeChannelType.SITE;
import static io.mango.notice.api.enums.NoticeChannelType.SMS;
import static io.mango.notice.api.enums.NoticeChannelType.EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class NoticeServiceTest {

 private NoticeSiteMessageMapper messageMapper;
 private NoticeService noticeService;
 private NoticeTaskMapper taskMapper;
 private NoticeRecipientMapper recipientMapper;
 private NoticeSendRecordMapper sendRecordMapper;
 private NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private NoticeBusinessTypeMapper businessTypeMapper;
 private NoticeBusinessConfigVersionMapper businessConfigVersionMapper;
 private NoticeChannelConfigMapper channelConfigMapper;
 private NoticeRecipientAccountMapper recipientAccountMapper;
 private NoticeReceivePreferenceMapper receivePreferenceMapper;
 private NoticeWecomSyncMappingMapper wecomSyncMappingMapper;
 private IdentityUserApi identityUserApi;
 private SysOrgApi sysOrgApi;
 private WecomDirectoryClient wecomDirectoryClient;
 private IOutboxStore outboxStore;
 private List<NoticeTaskStatus> taskStatusUpdates;
 private List<NoticeSendStatus> sendRecordStatusUpdates;

 @BeforeEach
 void setUp() {
 messageMapper = mock(NoticeSiteMessageMapper.class);
 NoticeChannelSender sender = new NoticeChannelSender() {
 @Override
 public io.mango.notice.api.enums.NoticeChannelType channelType() {
 return SITE;
 }

 @Override
 public ChannelSendResult send(ChannelSendCommand command) {
 return ChannelSendResult.success(1L);
 }
 };
 taskMapper = mock(NoticeTaskMapper.class);
 recipientMapper = mock(NoticeRecipientMapper.class);
 sendRecordMapper = mock(NoticeSendRecordMapper.class);
 channelTemplateMapper = mock(NoticeBusinessChannelTemplateMapper.class);
 channelConfigMapper = mock(NoticeChannelConfigMapper.class);
 recipientAccountMapper = mock(NoticeRecipientAccountMapper.class);
 receivePreferenceMapper = mock(NoticeReceivePreferenceMapper.class);
 wecomSyncMappingMapper = mock(NoticeWecomSyncMappingMapper.class);
 businessTypeMapper = mock(NoticeBusinessTypeMapper.class);
 businessConfigVersionMapper = mock(NoticeBusinessConfigVersionMapper.class);
 identityUserApi = mock(IdentityUserApi.class);
 sysOrgApi = mock(SysOrgApi.class);
 wecomDirectoryClient = mock(WecomDirectoryClient.class);
 outboxStore = mock(IOutboxStore.class);
 taskStatusUpdates = new ArrayList<>();
 sendRecordStatusUpdates = new ArrayList<>();
 when(taskMapper.selectById(any())).thenAnswer(invocation -> {
 NoticeTaskEntity task = new NoticeTaskEntity();
 task.setId(invocation.getArgument(0));
 task.setBizType("TEST_NOTICE");
 task.setSuccessCount(0);
 task.setFailCount(0);
 return task;
 });
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record(10L,1L,100L), record(11L,2L,101L)));
 when(recipientMapper.selectById(100L)).thenReturn(recipient(1L));
 when(recipientMapper.selectById(101L)).thenReturn(recipient(2L));
 when(channelTemplateMapper.selectById(any())).thenReturn(template());
 when(channelConfigMapper.selectList(any())).thenReturn(List.of(channelConfig(1L, SITE)));
 doAnswer(invocation -> {
 NoticeTaskEntity task = invocation.getArgument(0);
 task.setId(1L);
 return 1;
 }).when(taskMapper).insert(any(NoticeTaskEntity.class));
 doAnswer(invocation -> {
 NoticeRecipientEntity recipient = invocation.getArgument(0);
 recipient.setId(recipient.getUserId() +99);
 return 1;
 }).when(recipientMapper).insert(any(NoticeRecipientEntity.class));
 doAnswer(invocation -> {
 NoticeSendRecordEntity record = invocation.getArgument(0);
 record.setId(record.getRecipientId() -90);
 return 1;
 }).when(sendRecordMapper).insert(any(NoticeSendRecordEntity.class));
 doAnswer(invocation -> {
 NoticeTaskEntity task = invocation.getArgument(0);
 taskStatusUpdates.add(task.getStatus());
 return 1;
 }).when(taskMapper).updateById(any(NoticeTaskEntity.class));
 doAnswer(invocation -> {
 NoticeSendRecordEntity record = invocation.getArgument(0);
 sendRecordStatusUpdates.add(record.getStatus());
 return 1;
 }).when(sendRecordMapper).updateById(any(NoticeSendRecordEntity.class));
 doAnswer(invocation -> {
 NoticeSendRecordEntity record = invocation.getArgument(0);
 sendRecordStatusUpdates.add(record.getStatus());
 return 1;
 }).when(sendRecordMapper).update(any(NoticeSendRecordEntity.class), any(LambdaQueryWrapper.class));
 noticeService = new NoticeService(
 messageMapper,
 businessTypeMapper,
 businessConfigVersionMapper,
 channelTemplateMapper,
 channelConfigMapper,
	 taskMapper,
	 recipientMapper,
	 recipientAccountMapper,
 receivePreferenceMapper,
	 sendRecordMapper,
 mock(NoticeSettingMapper.class),
 wecomSyncMappingMapper,
 List.of(sender),
 new ObjectMapper(),
 outboxStore,
 identityUserApi,
 sysOrgApi,
 wecomDirectoryClient);
 }

 @Test
 void updateBusinessType_updatesBaseInfoWithoutSavingConfigDraft() {
 NoticeBusinessTypeEntity businessType = businessType();
 businessType.setBizGroup("基础");
 businessType.setDescription("旧说明");
 businessType.setParamsSchema("{\"type\":\"object\",\"properties\":{\"username\":{\"type\":\"string\"}}}");
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 UpdateNoticeBusinessTypeCommand command = new UpdateNoticeBusinessTypeCommand();
 command.setBizName("登录提醒");
 command.setBizGroup("系统");
 command.setDescription("新说明");

 var result = noticeService.updateBusinessType(1L, command);

 assertEquals("登录提醒", result.getBizName());
 assertEquals("系统", result.getBizGroup());
 assertEquals("新说明", result.getDescription());
 verify(businessTypeMapper).updateById(businessType);
 verify(businessConfigVersionMapper, never()).insert(any(NoticeBusinessConfigVersionEntity.class));
 verify(businessConfigVersionMapper, never()).updateById(any(NoticeBusinessConfigVersionEntity.class));
 }

 @Test
 void saveBusinessConfigDraft_existingDraft_updatesDraftOnly() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessConfigVersionEntity draft = businessConfigVersion(10L, 2, NoticeTemplateVersionStatus.DRAFT);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(businessConfigVersionMapper.selectOne(any())).thenReturn(draft);
 SaveNoticeBusinessConfigCommand command = new SaveNoticeBusinessConfigCommand();
 command.setParamsSchema("{\"type\":\"object\",\"properties\":{}}");
 command.setDefaultPriority(NoticePriority.HIGH);
 command.setIdempotentStrategy("bizId");

 var result = noticeService.saveBusinessConfigDraft(1L, command);

 assertEquals(10L, result.getId());
 assertEquals(2, result.getVersion());
 assertEquals(NoticeTemplateVersionStatus.DRAFT, result.getVersionStatus());
 assertEquals(NoticePriority.HIGH, result.getDefaultPriority());
 verify(businessConfigVersionMapper).updateById(draft);
 verify(businessConfigVersionMapper, never()).insert(any(NoticeBusinessConfigVersionEntity.class));
 }

 @Test
 void publishBusinessConfigDraft_movesActiveToHistoryAndUpdatesBusinessSnapshot() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessConfigVersionEntity draft = businessConfigVersion(11L, 2, NoticeTemplateVersionStatus.DRAFT);
 draft.setParamsSchema("{\"type\":\"object\",\"properties\":{\"orderNo\":{\"type\":\"string\"}}}");
 draft.setDefaultPriority(NoticePriority.URGENT);
 draft.setIdempotentStrategy("bizId");
 NoticeBusinessConfigVersionEntity active = businessConfigVersion(10L, 1, NoticeTemplateVersionStatus.ACTIVE);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(businessConfigVersionMapper.selectOne(any())).thenReturn(draft);
 when(businessConfigVersionMapper.selectList(any())).thenReturn(List.of(active));
 when(businessTypeMapper.updateById(any(NoticeBusinessTypeEntity.class))).thenReturn(1);

 boolean result = noticeService.publishBusinessConfigDraft(1L);

 assertTrue(result);
 assertEquals(NoticeTemplateVersionStatus.HISTORY, active.getVersionStatus());
 assertEquals(NoticeTemplateVersionStatus.ACTIVE, draft.getVersionStatus());
 assertEquals(NoticePriority.URGENT, businessType.getDefaultPriority());
 assertEquals("bizId", businessType.getIdempotentStrategy());
 verify(businessConfigVersionMapper).updateById(active);
 verify(businessConfigVersionMapper).updateById(draft);
 verify(businessTypeMapper).updateById(businessType);
 }

 @Test
 void saveChannelTemplate_existingDraft_updatesDraftOnly() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity draft = template();
 draft.setVersionStatus(NoticeTemplateVersionStatus.DRAFT);
 draft.setVersion(2);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(channelTemplateMapper.selectOne(any())).thenReturn(draft);
 SaveNoticeChannelTemplateCommand command = new SaveNoticeChannelTemplateCommand();
 command.setTemplateName("系统消息草稿");
 command.setTitleTemplate("标题 {{orderNo}}");
 command.setContentTemplate("内容 {{orderNo}}");
 command.setEnabled(Boolean.FALSE);

 var result = noticeService.saveChannelTemplate(1L, SITE, command);

 assertEquals(2, result.getVersion());
 assertEquals(NoticeTemplateVersionStatus.DRAFT, result.getVersionStatus());
 assertEquals(Boolean.FALSE, result.getEnabled());
 verify(channelTemplateMapper).updateById(draft);
 verify(channelTemplateMapper, never()).insert(any(NoticeBusinessChannelTemplateEntity.class));
 }

 @Test
 void saveChannelConfig_existingMaskedSecret_keepsOriginalSecret() {
 NoticeChannelConfigEntity existing = channelConfig(20L, EMAIL);
 existing.setConfigJson("{\"host\":\"smtp.old.com\",\"username\":\"notice@example.com\",\"password\":\"origin-secret\",\"from\":\"notice@example.com\",\"ssl\":true}");
 when(channelConfigMapper.selectById(20L)).thenReturn(existing);
 SaveNoticeChannelConfigCommand command = new SaveNoticeChannelConfigCommand();
 command.setId(20L);
 command.setChannelType(EMAIL);
 command.setProviderCode("CUSTOM_SMTP");
 command.setConfigName("默认邮件账号");
 command.setEnabled(Boolean.TRUE);
 command.setWeight(100);
 command.setPriority(0);
 command.setConfigJson("{\"host\":\"smtp.new.com\",\"username\":\"notice@example.com\",\"password\":\"***\",\"from\":\"notice@example.com\",\"ssl\":true}");

 noticeService.saveChannelConfig(command);

 ArgumentCaptor<NoticeChannelConfigEntity> captor = ArgumentCaptor.forClass(NoticeChannelConfigEntity.class);
 verify(channelConfigMapper).updateById(captor.capture());
 assertTrue(captor.getValue().getConfigJson().contains("\"password\":\"origin-secret\""));
 assertTrue(captor.getValue().getConfigJson().contains("\"host\":\"smtp.new.com\""));
 assertEquals(NoticeChannelConfigStatus.COMPLETE, captor.getValue().getConfigStatus());
 }

 @Test
 void saveChannelConfig_builtinSiteConfig_marksComplete() {
 SaveNoticeChannelConfigCommand command = new SaveNoticeChannelConfigCommand();
 command.setChannelType(SITE);
 command.setProviderCode("INTERNAL");
 command.setConfigName("默认系统消息通道");
 command.setEnabled(Boolean.TRUE);
 command.setWeight(100);
 command.setPriority(0);
 command.setConfigJson("{\"senderName\":\"系统通知\",\"retentionDays\":180,\"realtimeEnabled\":true}");

 noticeService.saveChannelConfig(command);

 ArgumentCaptor<NoticeChannelConfigEntity> captor = ArgumentCaptor.forClass(NoticeChannelConfigEntity.class);
 verify(channelConfigMapper).insert(captor.capture());
 assertEquals(NoticeChannelConfigStatus.COMPLETE, captor.getValue().getConfigStatus());
 }

 @Test
 void saveChannelConfig_aliyunEmailApiConfig_marksComplete() {
 SaveNoticeChannelConfigCommand command = new SaveNoticeChannelConfigCommand();
 command.setChannelType(EMAIL);
 command.setProviderCode("ALIYUN_DM");
 command.setConfigName("阿里云邮件推送");
 command.setEnabled(Boolean.TRUE);
 command.setWeight(100);
 command.setPriority(0);
 command.setConfigJson("{\"accessKeyId\":\"ak\",\"accessKeySecret\":\"sk\",\"regionId\":\"cn-hangzhou\",\"endpoint\":\"dm.aliyuncs.com\",\"accountName\":\"notice@example.com\"}");

 noticeService.saveChannelConfig(command);

 ArgumentCaptor<NoticeChannelConfigEntity> captor = ArgumentCaptor.forClass(NoticeChannelConfigEntity.class);
 verify(channelConfigMapper).insert(captor.capture());
 assertEquals(NoticeChannelConfigStatus.COMPLETE, captor.getValue().getConfigStatus());
 }

 @Test
 void deleteBusinessType_withoutRunningTask_deletesDefinitionVersionsAndTemplates() {
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType());
 when(taskMapper.selectCount(any())).thenReturn(0L);
 when(channelTemplateMapper.delete(any())).thenReturn(2);
 when(businessConfigVersionMapper.delete(any())).thenReturn(1);
 when(businessTypeMapper.deleteById(1L)).thenReturn(1);

 boolean result = noticeService.deleteBusinessType(1L);

 assertTrue(result);
 verify(channelTemplateMapper).delete(any());
 verify(businessConfigVersionMapper).delete(any());
 verify(businessTypeMapper).deleteById(1L);
 }

 @Test
 void deleteBusinessType_withRunningTask_throwsException() {
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType());
 when(taskMapper.selectCount(any())).thenReturn(1L);

 assertThrows(RuntimeException.class, () -> noticeService.deleteBusinessType(1L));

 verify(channelTemplateMapper, never()).delete(any());
 verify(businessConfigVersionMapper, never()).delete(any());
 verify(businessTypeMapper, never()).deleteById(anyLong());
 }

 @Test
 void deleteChannelConfig_regularConfig_deletesWhenUnused() {
 when(channelConfigMapper.selectById(30L)).thenReturn(channelConfig(30L, SMS));
 when(channelTemplateMapper.selectCount(any())).thenReturn(0L);
 when(channelConfigMapper.deleteById(30L)).thenReturn(1);

 boolean result = noticeService.deleteChannelConfig(30L);

 assertTrue(result);
 verify(channelTemplateMapper).selectCount(any());
 verify(channelConfigMapper).deleteById(30L);
 }

 @Test
 void deleteChannelConfig_builtinSiteConfig_throwsException() {
 NoticeChannelConfigEntity entity = channelConfig(31L, SITE);
 entity.setProviderCode("INTERNAL");
 when(channelConfigMapper.selectById(31L)).thenReturn(entity);

 assertThrows(RuntimeException.class, () -> noticeService.deleteChannelConfig(31L));

 verify(channelTemplateMapper, never()).selectCount(any());
 verify(channelConfigMapper, never()).deleteById(anyLong());
 }

 @Test
 void deleteChannelConfig_referencedConfig_throwsException() {
 when(channelConfigMapper.selectById(32L)).thenReturn(channelConfig(32L, EMAIL));
 when(channelTemplateMapper.selectCount(any())).thenReturn(1L);

 assertThrows(RuntimeException.class, () -> noticeService.deleteChannelConfig(32L));

 verify(channelConfigMapper, never()).deleteById(anyLong());
 }

 @Test
 void saveChannelTemplate_withoutDraft_createsNextDraftVersion() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity active = template(10L, SMS, "旧标题", "旧内容");
 active.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 active.setVersion(1);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(channelTemplateMapper.selectOne(any())).thenReturn(null);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(active));
 SaveNoticeChannelTemplateCommand command = new SaveNoticeChannelTemplateCommand();
 command.setTemplateName("短信新草稿");
 command.setTitleTemplate("新标题 {{orderNo}}");
 command.setContentTemplate("新内容 {{orderNo}}");
 command.setChannelTemplateId("tpl-2");
 command.setVariableMapping("{\"orderNo\":\"order_no\"}");

 var result = noticeService.saveChannelTemplate(1L, SMS, command);

 assertEquals(2, result.getVersion());
 assertEquals(NoticeTemplateVersionStatus.DRAFT, result.getVersionStatus());
 assertEquals("短信新草稿", result.getTemplateName());
 ArgumentCaptor<NoticeBusinessChannelTemplateEntity> captor = ArgumentCaptor.forClass(NoticeBusinessChannelTemplateEntity.class);
 verify(channelTemplateMapper).insert(captor.capture());
 NoticeBusinessChannelTemplateEntity draft = captor.getValue();
 assertEquals(1L, draft.getBusinessTypeId());
 assertEquals("TEST_NOTICE", draft.getBizType());
 assertEquals(SMS, draft.getChannelType());
 assertEquals(2, draft.getVersion());
 assertEquals(NoticeTemplateVersionStatus.DRAFT, draft.getVersionStatus());
 assertEquals("tpl-2", draft.getChannelTemplateId());
 assertEquals("{\"orderNo\":\"order_no\"}", draft.getVariableMapping());
 }

 @Test
 void publishChannelTemplate_draftBecomesActiveAndOldActiveBecomesHistory() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity draft = template(11L, SMS, "新标题", "新内容");
 draft.setVersion(2);
 draft.setVersionStatus(NoticeTemplateVersionStatus.DRAFT);
 NoticeBusinessChannelTemplateEntity active = template(10L, SMS, "旧标题", "旧内容");
 active.setVersion(1);
 active.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(channelTemplateMapper.selectOne(any())).thenReturn(draft);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(active));
 when(channelTemplateMapper.updateById(any(NoticeBusinessChannelTemplateEntity.class))).thenReturn(1);

 boolean result = noticeService.publishChannelTemplate(1L, SMS);

 assertTrue(result);
 assertEquals(NoticeTemplateVersionStatus.HISTORY, active.getVersionStatus());
 assertEquals(NoticeTemplateVersionStatus.ACTIVE, draft.getVersionStatus());
 assertNotNull(draft.getPublishTime());
 verify(channelTemplateMapper).updateById(active);
 verify(channelTemplateMapper).updateById(draft);
 }

 @Test
 void saveChannelTemplate_afterPublish_createsNewDraftWithoutChangingPublishedVersions() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity history = template(9L, SMS, "历史标题", "历史内容");
 history.setVersion(1);
 history.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 NoticeBusinessChannelTemplateEntity active = template(10L, SMS, "当前标题", "当前内容");
 active.setVersion(2);
 active.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 when(businessTypeMapper.selectById(1L)).thenReturn(businessType);
 when(channelTemplateMapper.selectOne(any())).thenReturn(null);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(history, active));
 SaveNoticeChannelTemplateCommand command = new SaveNoticeChannelTemplateCommand();
 command.setTemplateName("再次修改草稿");
 command.setTitleTemplate("V3 标题");
 command.setContentTemplate("V3 内容");

 var result = noticeService.saveChannelTemplate(1L, SMS, command);

 assertEquals(3, result.getVersion());
 assertEquals(NoticeTemplateVersionStatus.DRAFT, result.getVersionStatus());
 assertEquals(NoticeTemplateVersionStatus.HISTORY, history.getVersionStatus());
 assertEquals(NoticeTemplateVersionStatus.ACTIVE, active.getVersionStatus());
 ArgumentCaptor<NoticeBusinessChannelTemplateEntity> captor = ArgumentCaptor.forClass(NoticeBusinessChannelTemplateEntity.class);
 verify(channelTemplateMapper).insert(captor.capture());
 assertEquals(3, captor.getValue().getVersion());
 verify(channelTemplateMapper, never()).updateById(active);
 verify(channelTemplateMapper, never()).updateById(history);
 }

 @Test
 void send_activeChannelTemplates_rendersSharedParamsForEachChannel() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity siteTemplate = template(10L, SITE,
 "系统消息 {{orderNo}}", "订单 {{orderNo}} 已由 {{carrier}} 发货");
 NoticeBusinessChannelTemplateEntity smsTemplate = template(11L, SMS,
 "短信 {{orderNo}}", "短信订单 {{orderNo}} 已由 {{carrier}} 发货");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(siteTemplate, smsTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setBizId("SO-1001");
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(1L);
 recipient.setMobile("13800000000");
 command.setRecipients(List.of(recipient));
 command.setParams(Map.of("orderNo", "SO-1001", "carrier", "顺丰"));

 noticeService.send(command);

 verify(outboxStore).enqueue(any(OutboxMessage.class));
 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper, times(2)).insert(captor.capture());
 List<NoticeSendRecordEntity> records = captor.getAllValues();
 NoticeSendRecordEntity siteRecord = records.stream()
 .filter(record -> record.getChannelType() == SITE)
 .findFirst()
 .orElseThrow();
 NoticeSendRecordEntity smsRecord = records.stream()
 .filter(record -> record.getChannelType() == SMS)
 .findFirst()
 .orElseThrow();
 assertEquals("系统消息 SO-1001", siteRecord.getRenderedTitle());
 assertEquals("订单 SO-1001 已由 顺丰 发货", siteRecord.getRenderedContent());
 assertEquals("TEST_NOTICE", siteRecord.getBizType());
 assertEquals("SO-1001", siteRecord.getBizId());
 assertEquals("短信 SO-1001", smsRecord.getRenderedTitle());
 assertEquals("短信订单 SO-1001 已由 顺丰 发货", smsRecord.getRenderedContent());
 assertEquals("TEST_NOTICE", smsRecord.getBizType());
 assertEquals("SO-1001", smsRecord.getBizId());
 assertEquals(1, siteRecord.getTemplateVersion());
 assertEquals(1, smsRecord.getTemplateVersion());
 assertTrue(siteRecord.getRequestSnapshot().contains("\"channelType\":\"SITE\""));
 assertTrue(siteRecord.getRequestSnapshot().contains("\"bizId\":\"SO-1001\""));
 assertTrue(siteRecord.getRequestSnapshot().contains("\"recipientId\":100"));
 assertTaskTotalCount(2, "SITE,SMS");
 }

 @Test
 void send_activeChannelTemplates_supportsDollarVariableSyntax() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity siteTemplate = template(10L, SITE,
 "验证码 ${code}", "验证码 ${code}，${expireMinutes} 分钟内有效");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(siteTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("code", "839216", "expireMinutes", "5"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper).insert(captor.capture());
 NoticeSendRecordEntity record = captor.getValue();
 assertEquals("验证码 839216", record.getRenderedTitle());
 assertEquals("验证码 839216，5 分钟内有效", record.getRenderedContent());
 }

 @Test
 void send_activeChannelTemplates_rendersDatetimeParamBySchemaName() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity siteTemplate = template(10L, SITE,
 "申请 {{applyNo}}", "申请 {{applyNo}} 于 ${occurredAt} 发生");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(siteTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("applyNo", "APPLY-20260527", "occurredAt", "2026-05-27 10:30:00"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper).insert(captor.capture());
 NoticeSendRecordEntity record = captor.getValue();
 assertEquals("申请 APPLY-20260527", record.getRenderedTitle());
 assertEquals("申请 APPLY-20260527 于 2026-05-27 10:30:00 发生", record.getRenderedContent());
 assertTrue(record.getRequestSnapshot().contains("\"occurredAt\":\"2026-05-27 10:30:00\""));
 }

 @Test
 void send_activeChannelTemplates_rendersChineseParamName() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity siteTemplate = template(10L, SITE,
 "地区 {{地区}}", "系统消息地区：${地区}");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(siteTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("地区", "华东"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper).insert(captor.capture());
 NoticeSendRecordEntity record = captor.getValue();
 assertEquals("地区 华东", record.getRenderedTitle());
 assertEquals("系统消息地区：华东", record.getRenderedContent());
 }

 @Test
 void send_attachmentParams_rendersRecordsFromPersistedTaskParams() {
 NoticeBusinessTypeEntity businessType = businessType();
 NoticeBusinessChannelTemplateEntity siteTemplate = template(10L, SITE,
 "订单 {{orderNo}}", "订单 {{orderNo}} 附件 {{attachments}}");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType);
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(siteTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("orderNo", "SO-1001"));
 command.setAttachmentFileIds(List.of(10L, 20L));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper).insert(captor.capture());
 NoticeSendRecordEntity record = captor.getValue();
 assertEquals("订单 SO-1001", record.getRenderedTitle());
 assertEquals("订单 SO-1001 附件 [10, 20]", record.getRenderedContent());
 assertTrue(record.getRequestSnapshot().contains("\"attachments\":[10,20]"));
 }

 @Test
 void send_recipientTargets_resolvesUsersAndDeduplicatesRecipients() {
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template(10L, SITE, "标题", "内容")));
 when(identityUserApi.listUserInfosByTarget(argThat(query ->
 query != null && query.getTargetType().name().equals("ORG") && Long.valueOf(200L).equals(query.getTargetId()))))
 .thenReturn(R.ok(List.of(identityUser(1L, "admin"), identityUser(2L, "operator"))));
 when(identityUserApi.listUserInfosByTarget(argThat(query ->
 query != null && query.getTargetType().name().equals("ROLE") && Long.valueOf(300L).equals(query.getTargetId()))))
 .thenReturn(R.ok(List.of(identityUser(2L, "operator"))));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 NoticeRecipientTargetCommand orgTarget = new NoticeRecipientTargetCommand();
 orgTarget.setTargetType(NoticeRecipientTargetType.ORG);
 orgTarget.setTargetId(200L);
 NoticeRecipientTargetCommand roleTarget = new NoticeRecipientTargetCommand();
 roleTarget.setTargetType(NoticeRecipientTargetType.ROLE);
 roleTarget.setTargetId(300L);
 command.setRecipientTargets(List.of(orgTarget, roleTarget));

 noticeService.send(command);

 ArgumentCaptor<NoticeRecipientEntity> captor = ArgumentCaptor.forClass(NoticeRecipientEntity.class);
 verify(recipientMapper, times(2)).insert(captor.capture());
 assertEquals(List.of(1L, 2L), captor.getAllValues().stream().map(NoticeRecipientEntity::getUserId).toList());
 ArgumentCaptor<NoticeTaskEntity> taskCaptor = ArgumentCaptor.forClass(NoticeTaskEntity.class);
 verify(taskMapper).insert(taskCaptor.capture());
 assertTrue(taskCaptor.getValue().getRecipientTargetsSnapshot().contains("\"targetType\":\"ORG\""));
 assertTrue(taskCaptor.getValue().getRecipientTargetsSnapshot().contains("\"targetType\":\"ROLE\""));
 assertTaskTotalCount(2, "SITE");
 }

 @Test
 void send_immediateTask_enqueuesOutboxWithoutInlineExecution() {
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template(10L, SITE, "标题", "内容")));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);

 var result = noticeService.send(command);

 assertEquals(0, result.getSuccessCount());
 assertEquals(0, result.getFailCount());
 verify(outboxStore).enqueue(any(OutboxMessage.class));
 verify(sendRecordMapper, never()).selectList(any());
 assertTaskTotalCount(1, "SITE");
 }

 @Test
 void send_scheduledTask_enqueuesOutboxWithScheduledAttemptTime() {
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template(10L, SITE, "标题", "内容")));
 LocalDateTime scheduledTime = LocalDateTime.of(2026, 5, 26, 10, 30);
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setSendMode(NoticeSendMode.SCHEDULED);
 command.setScheduledTime(scheduledTime);

 noticeService.send(command);

 ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
 verify(outboxStore).enqueue(captor.capture());
 assertEquals("notice.send", captor.getValue().getEventType());
 assertEquals(1L, captor.getValue().getPayload().get("taskId"));
 assertEquals(scheduledTime.atZone(ZoneId.systemDefault()).toInstant(),
 captor.getValue().getNextAttemptAt());
 verify(sendRecordMapper, never()).selectList(any());
 assertTaskTotalCount(1, "SITE");
 }

 @Test
 void listSendRecords_returnsBizContextInRecordVO() {
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setBizType("TEST_NOTICE");
 record.setBizId("BIZ-1");
 record.setBusinessChannelTemplateId(30L);
 record.setTemplateVersion(2);
 record.setChannelConfigId(40L);
 Page<NoticeSendRecordEntity> page = new Page<>(1, 10);
 page.setRecords(List.of(record));
 page.setTotal(1);
 when(sendRecordMapper.selectPage(any(), any())).thenReturn(page);
 NoticeBusinessTypeEntity businessType = businessType();
 businessType.setBizGroup("基础");
 when(businessTypeMapper.selectList(any())).thenReturn(List.of(businessType));
 NoticeRecipientEntity recipient = recipient(1L);
 recipient.setRecipientName("管理员");
 when(recipientMapper.selectList(any())).thenReturn(List.of(recipient));
 NoticeBusinessChannelTemplateEntity template = template(30L, SITE, "标题", "内容");
 template.setTemplateName("登录提醒系统消息");
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template));
 NoticeChannelConfigEntity channelConfig = channelConfig(40L, SITE);
 channelConfig.setConfigName("系统消息内置通道");
 when(channelConfigMapper.selectList(any())).thenReturn(List.of(channelConfig));
 NoticeSendRecordPageQuery query = new NoticeSendRecordPageQuery();
 query.setBizType("TEST_NOTICE");
 query.setBizId("BIZ-1");

 var result = noticeService.listSendRecords(query);

 assertEquals(1, result.getTotal());
 assertEquals("TEST_NOTICE", result.getList().get(0).getBizType());
 assertEquals("BIZ-1", result.getList().get(0).getBizId());
 assertEquals("基础", result.getList().get(0).getBizGroup());
 assertEquals("测试通知", result.getList().get(0).getMessageName());
 assertEquals("管理员", result.getList().get(0).getRecipientName());
 assertNull(result.getList().get(0).getRecipientAccount());
 assertEquals(30L, result.getList().get(0).getBusinessChannelTemplateId());
 assertEquals("登录提醒系统消息", result.getList().get(0).getBusinessChannelTemplateName());
 assertEquals(2, result.getList().get(0).getTemplateVersion());
 assertEquals(40L, result.getList().get(0).getChannelConfigId());
 assertEquals("系统消息内置通道", result.getList().get(0).getChannelConfigName());
 }

 @Test
 void send_disabledChannelTemplate_isNotSelected() {
 NoticeBusinessChannelTemplateEntity disabledSite = template(10L, SITE,
 "系统消息 {{orderNo}}", "订单 {{orderNo}}");
 disabledSite.setEnabled(false);
 NoticeBusinessChannelTemplateEntity activeSms = template(11L, SMS,
 "短信 {{orderNo}}", "短信订单 {{orderNo}}");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(disabledSite, activeSms));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(1L);
 recipient.setMobile("13800000000");
 command.setRecipients(List.of(recipient));
 command.setParams(Map.of("orderNo", "SO-1001"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper, times(2)).insert(captor.capture());
 List<NoticeSendRecordEntity> records = captor.getAllValues();
 NoticeSendRecordEntity smsRecord = records.stream()
 .filter(record -> record.getChannelType() == SMS)
 .findFirst()
 .orElseThrow();
 assertEquals("短信 SO-1001", smsRecord.getRenderedTitle());
 assertTrue(records.stream().anyMatch(record -> record.getChannelType() == SITE));
 assertTaskTotalCount(2, "SMS,SITE");
 }

 @Test
 void send_configuredSmsWithoutMobile_createsCanceledSmsRecordAndSiteFallback() {
 NoticeBusinessChannelTemplateEntity smsTemplate = template(11L, SMS,
 "短信 {{orderNo}}", "短信订单 {{orderNo}}");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(smsTemplate));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("orderNo", "SO-1001"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper, times(2)).insert(captor.capture());
 List<NoticeSendRecordEntity> records = captor.getAllValues();
 NoticeSendRecordEntity smsRecord = records.stream()
 .filter(record -> record.getChannelType() == SMS)
 .findFirst()
 .orElseThrow();
 assertEquals(NoticeSendStatus.CANCELED, smsRecord.getStatus());
 assertEquals(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING.name(), smsRecord.getFailCode());
 assertTrue(records.stream().anyMatch(record -> record.getChannelType() == SITE
 && record.getStatus() == NoticeSendStatus.PENDING));
 assertTaskTotalCount(2, "SMS,SITE");
 }

 @Test
 void send_userDisabledSmsChannel_createsCanceledRecord() {
 NoticeBusinessChannelTemplateEntity smsTemplate = template(11L, SMS,
 "短信 {{orderNo}}", "短信订单 {{orderNo}}");
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(smsTemplate));
 NoticeReceivePreferenceEntity preference = new NoticeReceivePreferenceEntity();
 preference.setUserId(1L);
 preference.setScopeType(NoticeReceivePreferenceScopeType.BIZ_TYPE);
 preference.setScopeValue("TEST_NOTICE");
 preference.setChannelType(SMS);
 preference.setEnabled(false);
 when(receivePreferenceMapper.selectOne(any())).thenReturn(preference);
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setParams(Map.of("orderNo", "SO-1001"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper, times(2)).insert(captor.capture());
 NoticeSendRecordEntity smsRecord = captor.getAllValues().stream()
 .filter(record -> record.getChannelType() == SMS)
 .findFirst()
 .orElseThrow();
 assertEquals(NoticeSendStatus.CANCELED, smsRecord.getStatus());
 assertEquals(NoticeSendCancelCode.USER_CHANNEL_DISABLED.name(), smsRecord.getFailCode());
 assertEquals("用户关闭该消息渠道", smsRecord.getFailReason());
 }

 @Test
 void syncWecomUsers_groupTargetReturnsBusinessMessageWithoutCallingWecom() {
 SyncWecomUsersCommand command = new SyncWecomUsersCommand();
 command.setTargetOrgId(1L);
 command.setTargetOrgType(1);
 command.setSyncUsers(true);

 var result = noticeService.syncWecomUsers(command);

 assertEquals(1, result.getFailedCount());
 assertEquals("集团节点不支持同步企业微信用户，请选择二级公司或已映射部门", result.getMessages().get(0));
 verifyNoInteractions(wecomDirectoryClient);
 }

 @Test
 void send_withoutReceiver_throwsReceiverRequired() {
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template(10L, SITE, "标题", "内容")));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");

 RuntimeException error = assertThrows(RuntimeException.class, () -> noticeService.send(command));

 assertEquals("接收用户不能为空", error.getMessage());
 verify(taskMapper, never()).insert(any(NoticeTaskEntity.class));
 verify(sendRecordMapper, never()).insert(any(NoticeSendRecordEntity.class));
 }

 @Test
 void send_noEnabledTemplateWithBusinessType_sendsSiteFallback() {
 NoticeBusinessChannelTemplateEntity disabledSite = template(10L, SITE,
 "系统消息 {{orderNo}}", "订单 {{orderNo}}");
 disabledSite.setEnabled(false);
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(disabledSite));
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setChannelTypes(List.of(SITE));
 command.setUserId(1L);
 command.setParams(Map.of("orderNo", "SO-1001"));

 noticeService.send(command);

 ArgumentCaptor<NoticeSendRecordEntity> captor = ArgumentCaptor.forClass(NoticeSendRecordEntity.class);
 verify(sendRecordMapper).insert(captor.capture());
 assertEquals(SITE, captor.getValue().getChannelType());
 assertEquals("测试通知", captor.getValue().getRenderedTitle());
 assertTaskTotalCount(1, "SITE");
 }

 @Test
 void executeTask_emailRecordPassesHtmlContentAndAttachmentFileIdsToSender() {
 AtomicReference<ChannelSendCommand> sentCommand = new AtomicReference<>();
 NoticeChannelConfigMapper emailChannelConfigMapper = mock(NoticeChannelConfigMapper.class);
 when(emailChannelConfigMapper.selectList(any())).thenReturn(List.of(channelConfig(2L, EMAIL)));
 NoticeChannelSender emailSender = new NoticeChannelSender() {
 @Override
 public NoticeChannelType channelType() {
 return EMAIL;
 }

 @Override
 public ChannelSendResult send(ChannelSendCommand command) {
 sentCommand.set(command);
 return ChannelSendResult.providerSuccess("email-20", "{\"status\":\"SENT\"}");
 }
 };
 noticeService = new NoticeService(
 messageMapper,
 businessTypeMapper,
 businessConfigVersionMapper,
 channelTemplateMapper,
 emailChannelConfigMapper,
	 taskMapper,
	 recipientMapper,
	 recipientAccountMapper,
	 receivePreferenceMapper,
	 sendRecordMapper,
 mock(NoticeSettingMapper.class),
 wecomSyncMappingMapper,
 List.of(emailSender),
 new ObjectMapper(),
 outboxStore,
 identityUserApi,
 sysOrgApi,
 mock(WecomDirectoryClient.class));
 NoticeTaskEntity task = new NoticeTaskEntity();
 task.setId(1L);
 task.setBizType("TEST_NOTICE");
 task.setParamsSnapshot("{\"attachments\":[1001,1002]}");
 when(taskMapper.selectById(1L)).thenReturn(task);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setChannelType(EMAIL);
 record.setRenderedTitle("发货通知");
 record.setRenderedContent("<p>订单 SO-1001 已发货</p>");
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));
 when(recipientMapper.selectById(100L)).thenReturn(recipient(1L));
 NoticeBusinessChannelTemplateEntity template = template(20L, EMAIL, "发货通知", "<p>订单 {{orderNo}} 已发货</p>");
 when(channelTemplateMapper.selectById(1L)).thenReturn(template);

 int result = noticeService.executeTask(1L);

 assertEquals(1, result);
 assertEquals("发货通知", sentCommand.get().getTitle());
 assertEquals("<p>订单 SO-1001 已发货</p>", sentCommand.get().getContent());
 assertEquals(List.of(1001L, 1002L), sentCommand.get().getAttachmentFileIds());
 assertEquals("{\"status\":\"SENT\"}", record.getResponseSnapshot());
 }

 @Test
 void executeTask_whenTaskAlreadySent_doesNotSendAgain() {
 NoticeTaskEntity task = task(1L);
 task.setSuccessCount(2);
 task.setFailCount(0);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of());

 int result = noticeService.executeTask(1L);

 assertEquals(0, result);
 assertTaskFinalStatus(NoticeTaskStatus.SUCCESS, 2, 0);
 verify(recipientMapper, never()).selectById(anyLong());
 }

 @Test
 void executeTask_whenRecordAlreadyClaimed_doesNotSendAgain() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));
 when(sendRecordMapper.update(any(NoticeSendRecordEntity.class), any(LambdaQueryWrapper.class))).thenReturn(0);

 int result = noticeService.executeTask(1L);

 assertEquals(0, result);
 verify(taskMapper, times(1)).updateById(task);
 verify(recipientMapper, never()).selectById(anyLong());
 }

 @Test
 void executeTask_canceledTask_returnsZeroWithoutSending() {
 NoticeTaskEntity task = task(2L);
 task.setStatus(NoticeTaskStatus.CANCELED);
 when(taskMapper.selectById(2L)).thenReturn(task);

 int result = noticeService.executeTask(2L);

 assertEquals(0, result);
 verify(sendRecordMapper, never()).selectList(any());
 verify(sendRecordMapper, never()).updateById(any(NoticeSendRecordEntity.class));
 verify(taskMapper, never()).updateById(task);
 }

 @Test
 void executeTask_allRecordsSuccess_marksTaskSuccess() {
 NoticeTaskEntity task = task(1L);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record(20L, 1L, 100L), record(21L, 2L, 101L)));

 int result = noticeService.executeTask(1L);

 assertEquals(2, result);
 assertTaskFinalStatus(NoticeTaskStatus.SUCCESS, 2, 0);
 assertSendRecordStatusUpdates(NoticeSendStatus.SENDING, NoticeSendStatus.SUCCESS,
 NoticeSendStatus.SENDING, NoticeSendStatus.SUCCESS);
 }

 @Test
 void executeTask_successAndFailure_marksTaskPartialSuccess() {
 noticeService = serviceWithSender(command -> command.getSendRecordId().equals(20L)
 ? ChannelSendResult.success(1L)
 : ChannelSendResult.failed("SEND_FAILED", "发送失败", false));
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity successRecord = record(20L, 1L, 100L);
 NoticeSendRecordEntity failRecord = record(21L, 2L, 101L);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(successRecord, failRecord));

 int result = noticeService.executeTask(1L);

 assertEquals(1, result);
 assertTaskFinalStatus(NoticeTaskStatus.PARTIAL_SUCCESS, 1, 1);
 assertSendRecordStatusUpdates(NoticeSendStatus.SENDING, NoticeSendStatus.SUCCESS,
 NoticeSendStatus.SENDING, NoticeSendStatus.FAILED);
 assertEquals(NoticeSendStatus.FAILED, failRecord.getStatus());
 assertEquals("SEND_FAILED", failRecord.getFailCode());
 assertEquals("发送失败", failRecord.getFailReason());
 }

 @Test
 void executeTask_retryableFailure_marksRecordRetryWaiting() {
 noticeService = serviceWithSender(command -> ChannelSendResult.failed("SEND_FAILED", "临时失败", true));
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));

 int result = noticeService.executeTask(1L);

 assertEquals(0, result);
 assertTaskFinalStatus(NoticeTaskStatus.FAILED, 0, 1);
 assertSendRecordStatusUpdates(NoticeSendStatus.SENDING, NoticeSendStatus.RETRY_WAITING);
 assertEquals(NoticeSendStatus.RETRY_WAITING, record.getStatus());
 assertEquals(1, record.getRetryCount());
 assertNotNull(record.getNextRetryTime());
 }

 @Test
 void executeTask_retryWaitingRecordFailure_doesNotDoubleCountFailCount() {
 noticeService = serviceWithSender(command -> ChannelSendResult.failed("SEND_FAILED", "再次失败", true));
 NoticeTaskEntity task = task(1L);
 task.setSuccessCount(1);
 task.setFailCount(1);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.RETRY_WAITING);
 record.setRetryCount(1);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));

 int result = noticeService.executeTask(1L);

 assertEquals(0, result);
 assertTaskFinalStatus(NoticeTaskStatus.PARTIAL_SUCCESS, 1, 1);
 assertEquals(2, record.getRetryCount());
 }

 @Test
 void executeTask_allRecordsFailure_marksTaskFailed() {
 noticeService = serviceWithSender(command -> ChannelSendResult.failed("SEND_FAILED", "发送失败", false));
 NoticeTaskEntity task = task(1L);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record(20L, 1L, 100L), record(21L, 2L, 101L)));

 int result = noticeService.executeTask(1L);

 assertEquals(0, result);
 assertTaskFinalStatus(NoticeTaskStatus.FAILED, 0, 2);
 assertSendRecordStatusUpdates(NoticeSendStatus.SENDING, NoticeSendStatus.FAILED,
 NoticeSendStatus.SENDING, NoticeSendStatus.FAILED);
 }

 @Test
 void hasRetryWaitingRecords_whenRecordExists_returnsTrue() {
 when(sendRecordMapper.selectCount(any())).thenReturn(1L);

 boolean result = noticeService.hasRetryWaitingRecords(1L);

 assertTrue(result);
 verify(sendRecordMapper).selectCount(any());
 }

 @Test
 void finalizeRetryWaitingRecords_marksRecordsFinalFailedAndTaskFailed() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.RETRY_WAITING);
 record.setNextRetryTime(LocalDateTime.now());
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));
 when(taskMapper.selectById(1L)).thenReturn(task);

 noticeService.finalizeRetryWaitingRecords(1L, "达到最大重试次数");

 assertEquals(NoticeSendStatus.FINAL_FAILED, record.getStatus());
 assertEquals("达到最大重试次数", record.getFailReason());
 assertNull(record.getNextRetryTime());
 assertEquals(List.of(NoticeSendStatus.FINAL_FAILED), sendRecordStatusUpdates);
 assertEquals(NoticeTaskStatus.FAILED, taskStatusUpdates.get(0));
 assertEquals(0, task.getSuccessCount());
 assertEquals(1, task.getFailCount());
 }

 @Test
 void retrySendRecord_failedRecord_executesTaskImmediately() {
 noticeService = serviceWithSender(command -> ChannelSendResult.providerSuccess("provider-1", "{}"));
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.FAILED);
 record.setRetryCount(1);
 when(sendRecordMapper.selectById(20L)).thenReturn(record);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));

 boolean result = noticeService.retrySendRecord(20L);

 assertTrue(result);
 assertEquals(NoticeSendStatus.SUCCESS, record.getStatus());
 assertNull(record.getNextRetryTime());
 assertSendRecordStatusUpdates(NoticeSendStatus.RETRY_WAITING, NoticeSendStatus.SENDING, NoticeSendStatus.SUCCESS);
 }

 @Test
 void retrySendRecord_successRecord_rejectsOperation() {
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.SUCCESS);
 when(sendRecordMapper.selectById(20L)).thenReturn(record);

 assertThrows(RuntimeException.class, () -> noticeService.retrySendRecord(20L));

 verify(sendRecordMapper, never()).updateById(record);
 }

 @Test
 void markSendRecordManualSuccess_failedRecord_updatesStatusAndTaskCount() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.FAILED);
 HandleNoticeSendRecordCommand command = new HandleNoticeSendRecordCommand();
 command.setReason("外部平台已确认送达");
 when(sendRecordMapper.selectById(20L)).thenReturn(record);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));

 boolean result = noticeService.markSendRecordManualSuccess(20L, command);

 assertTrue(result);
 assertEquals(NoticeSendStatus.MANUAL_SUCCESS, record.getStatus());
 assertEquals("人工确认成功：外部平台已确认送达", record.getFailReason());
 assertEquals(NoticeTaskStatus.SUCCESS, taskStatusUpdates.get(taskStatusUpdates.size() - 1));
 assertEquals(1, task.getSuccessCount());
 assertEquals(0, task.getFailCount());
 }

 @Test
 void ignoreSendRecord_finalFailedRecord_updatesStatusAndTaskCount() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity record = record(20L, 1L, 100L);
 record.setStatus(NoticeSendStatus.FINAL_FAILED);
 HandleNoticeSendRecordCommand command = new HandleNoticeSendRecordCommand();
 command.setReason("测试数据不再处理");
 when(sendRecordMapper.selectById(20L)).thenReturn(record);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(record));

 boolean result = noticeService.ignoreSendRecord(20L, command);

 assertTrue(result);
 assertEquals(NoticeSendStatus.IGNORED, record.getStatus());
 assertEquals("忽略失败：测试数据不再处理", record.getFailReason());
 assertEquals(NoticeTaskStatus.SUCCESS, taskStatusUpdates.get(taskStatusUpdates.size() - 1));
 assertEquals(1, task.getSuccessCount());
 assertEquals(0, task.getFailCount());
 }

 @Test
 void markSendRecordsManualSuccess_failedRecords_updatesStatusesAndRefreshesTask() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity first = record(20L, 1L, 100L);
 NoticeSendRecordEntity second = record(21L, 1L, 101L);
 first.setStatus(NoticeSendStatus.FAILED);
 second.setStatus(NoticeSendStatus.RETRY_WAITING);
 HandleNoticeSendRecordsCommand command = new HandleNoticeSendRecordsCommand();
 command.setIds(List.of(20L, 21L));
 command.setReason("平台侧已送达");
 when(sendRecordMapper.selectById(20L)).thenReturn(first);
 when(sendRecordMapper.selectById(21L)).thenReturn(second);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(first, second));

 boolean result = noticeService.markSendRecordsManualSuccess(command);

 assertTrue(result);
 assertEquals(NoticeSendStatus.MANUAL_SUCCESS, first.getStatus());
 assertEquals(NoticeSendStatus.MANUAL_SUCCESS, second.getStatus());
 assertEquals("人工确认成功：平台侧已送达", first.getFailReason());
 assertEquals("人工确认成功：平台侧已送达", second.getFailReason());
 assertEquals(NoticeTaskStatus.SUCCESS, taskStatusUpdates.get(taskStatusUpdates.size() - 1));
 assertEquals(2, task.getSuccessCount());
 assertEquals(0, task.getFailCount());
 verify(taskMapper, times(1)).selectById(1L);
 }

 @Test
 void ignoreSendRecords_failedRecords_updatesStatusesAndRefreshesTask() {
 NoticeTaskEntity task = task(1L);
 NoticeSendRecordEntity first = record(20L, 1L, 100L);
 NoticeSendRecordEntity second = record(21L, 1L, 101L);
 first.setStatus(NoticeSendStatus.FAILED);
 second.setStatus(NoticeSendStatus.FINAL_FAILED);
 HandleNoticeSendRecordsCommand command = new HandleNoticeSendRecordsCommand();
 command.setIds(List.of(20L, 21L));
 command.setReason("测试失败记录");
 when(sendRecordMapper.selectById(20L)).thenReturn(first);
 when(sendRecordMapper.selectById(21L)).thenReturn(second);
 when(taskMapper.selectById(1L)).thenReturn(task);
 when(sendRecordMapper.selectList(any())).thenReturn(List.of(first, second));

 boolean result = noticeService.ignoreSendRecords(command);

 assertTrue(result);
 assertEquals(NoticeSendStatus.IGNORED, first.getStatus());
 assertEquals(NoticeSendStatus.IGNORED, second.getStatus());
 assertEquals("忽略失败：测试失败记录", first.getFailReason());
 assertEquals("忽略失败：测试失败记录", second.getFailReason());
 assertEquals(NoticeTaskStatus.SUCCESS, taskStatusUpdates.get(taskStatusUpdates.size() - 1));
 assertEquals(2, task.getSuccessCount());
 assertEquals(0, task.getFailCount());
 verify(taskMapper, times(1)).selectById(1L);
 }

 @Test
 void send_recipients_createsRecipientSnapshotWithChannelAddresses() {
 when(businessTypeMapper.selectOne(any())).thenReturn(businessType());
 when(channelTemplateMapper.selectList(any())).thenReturn(List.of(template(10L, SITE, "标题", "内容")));
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(1001L);
 recipient.setRecipientName("张三");
 recipient.setMobile("13800000000");
 recipient.setEmail("zhangsan@example.com");
 recipient.setWechatOpenid("openid-1001");
 recipient.setWecomUserId("wecom-1001");
 recipient.setDingtalkUserId("dingtalk-1001");
 recipient.setExternalId("external-1001");
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setRecipients(List.of(recipient));
 command.setParams(Map.of("orderNo", "SO-1001"));

 noticeService.send(command);

 ArgumentCaptor<NoticeRecipientEntity> captor = ArgumentCaptor.forClass(NoticeRecipientEntity.class);
 verify(recipientMapper).insert(captor.capture());
 NoticeRecipientEntity snapshot = captor.getValue();
 assertEquals(1L, snapshot.getTaskId());
 assertEquals(1001L, snapshot.getUserId());
 assertEquals("张三", snapshot.getRecipientName());
 assertEquals("13800000000", snapshot.getMobile());
 assertEquals("zhangsan@example.com", snapshot.getEmail());
 assertEquals("openid-1001", snapshot.getWechatOpenid());
 assertEquals("wecom-1001", snapshot.getWecomUserId());
 assertEquals("dingtalk-1001", snapshot.getDingtalkUserId());
 assertEquals("external-1001", snapshot.getExternalId());
 }

 @Test
 void send_noBusinessTemplate_dispatchesSiteChannelForAllReceivers() {
 SendNoticeCommand command = new SendNoticeCommand();
 command.setBizType("TEST_NOTICE");
 command.setUserId(1L);
 command.setUserIds(List.of(2L));
 command.setTitle("标题");
 command.setContent("内容");

 var result = noticeService.send(command);

 assertEquals(0, result.getSuccessCount());
 assertEquals(0, result.getFailCount());
 verify(outboxStore).enqueue(any(OutboxMessage.class));
 }

 @Test
 void write_siteMessage_setsUnreadAndNormalStatus() {
 when(messageMapper.insert(any(NoticeSiteMessageEntity.class))).thenAnswer(invocation -> {
 NoticeSiteMessageEntity entity = invocation.getArgument(0);
 entity.setId(1L);
 return 1;
 });
 ChannelSendCommand command = new ChannelSendCommand();
 command.setTaskId(10L);
 command.setSendRecordId(20L);
 command.setUserId(1L);
 command.setTitle("标题");
 command.setContent("内容");
 command.setPriority(NoticePriority.HIGH);
 command.setBizType("TEST_NOTICE");
 command.setBizId("BIZ-1");

 Long id = new NoticeSiteMessageWriterImpl(messageMapper).write(command);

 assertEquals(1L, id);
 ArgumentCaptor<NoticeSiteMessageEntity> captor = ArgumentCaptor.forClass(NoticeSiteMessageEntity.class);
 verify(messageMapper).insert(captor.capture());
 NoticeSiteMessageEntity entity = captor.getValue();
 assertEquals(10L, entity.getTaskId());
 assertEquals(20L, entity.getSendRecordId());
 assertEquals(1L, entity.getUserId());
 assertEquals("标题", entity.getTitle());
 assertEquals("内容", entity.getContent());
 assertEquals(NoticePriority.HIGH, entity.getPriority());
 assertEquals(NoticeReadStatus.UNREAD, entity.getReadStatus());
 assertEquals(NoticeDeleteStatus.NORMAL, entity.getDeleteStatus());
 assertEquals("TEST_NOTICE", entity.getBizType());
 assertEquals("BIZ-1", entity.getBizId());
 }

 @Test
 void getSiteMessage_currentUserVisibleMessage_returnsDetail() {
 NoticeSiteMessageEntity entity = siteMessage(1L, 100L);
 when(messageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

 var result = noticeService.getSiteMessage(1L, 100L);

 assertEquals("测试系统消息", result.getTitle());
 assertEquals("这是一条系统消息内容", result.getContent());
 assertEquals(NoticeReadStatus.UNREAD, result.getReadStatus());
 assertEquals("TEST_NOTICE", result.getBizType());
 assertEquals("BIZ-1", result.getBizId());
 }

 @Test
 void unreadCount_currentUserUnreadMessages_returnsCount() {
 when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

 var result = noticeService.unreadCount(100L);

 assertEquals(3L, result.getCount());
 }

 @Test
 void markSiteMessageRead_updatesOnlyCurrentUserVisibleMessage() {
 when(messageMapper.update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class))).thenReturn(1);

 boolean result = noticeService.markSiteMessageRead(1L, 100L);

 assertTrue(result);
 verify(messageMapper).update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class));
 }

 @Test
 void markSiteMessagesRead_emptyIds_doesNotSelectById() {
 MarkNoticeReadCommand command = new MarkNoticeReadCommand();
 command.setIds(List.of(1L, 2L));
 when(messageMapper.update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class))).thenReturn(2);

 boolean result = noticeService.markSiteMessagesRead(command, 100L);

 assertTrue(result);
 verify(messageMapper, never()).selectById(any());
 }

 @Test
 void markAllSiteMessagesRead_updatesUnreadMessagesForCurrentUser() {
 when(messageMapper.update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class))).thenReturn(2);

 boolean result = noticeService.markAllSiteMessagesRead(100L);

 assertTrue(result);
 ArgumentCaptor<NoticeSiteMessageEntity> captor = ArgumentCaptor.forClass(NoticeSiteMessageEntity.class);
 verify(messageMapper).update(captor.capture(), any(LambdaQueryWrapper.class));
 assertEquals(NoticeReadStatus.READ, captor.getValue().getReadStatus());
 assertNotNull(captor.getValue().getReadTime());
 }

 @Test
 void deleteSiteMessage_marksDeletedForCurrentUser() {
 when(messageMapper.update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class))).thenReturn(1);

 boolean result = noticeService.deleteSiteMessage(1L, 100L);

 assertTrue(result);
 verify(messageMapper).update(any(NoticeSiteMessageEntity.class), any(LambdaQueryWrapper.class));
 }

 private NoticeBusinessChannelTemplateEntity template() {
 NoticeBusinessChannelTemplateEntity template = new NoticeBusinessChannelTemplateEntity();
 template.setId(1L);
 template.setChannelType(SITE);
 template.setTitleTemplate("标题");
 template.setContentTemplate("内容");
 template.setVersion(1);
 return template;
 }

 private NoticeBusinessChannelTemplateEntity template(Long id, NoticeChannelType channelType, String titleTemplate,
 String contentTemplate) {
 NoticeBusinessChannelTemplateEntity template = new NoticeBusinessChannelTemplateEntity();
 template.setId(id);
 template.setBusinessTypeId(1L);
 template.setBizType("TEST_NOTICE");
 template.setChannelType(channelType);
 template.setTitleTemplate(titleTemplate);
 template.setContentTemplate(contentTemplate);
 template.setVersion(1);
 template.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 template.setEnabled(true);
 return template;
 }

 private NoticeBusinessTypeEntity businessType() {
 NoticeBusinessTypeEntity businessType = new NoticeBusinessTypeEntity();
 businessType.setId(1L);
 businessType.setBizType("TEST_NOTICE");
 businessType.setBizName("测试通知");
 businessType.setDefaultPriority(NoticePriority.NORMAL);
 businessType.setEnabled(true);
 return businessType;
 }

 private NoticeBusinessConfigVersionEntity businessConfigVersion(Long id, Integer version,
 NoticeTemplateVersionStatus status) {
 NoticeBusinessConfigVersionEntity entity = new NoticeBusinessConfigVersionEntity();
 entity.setId(id);
 entity.setBusinessTypeId(1L);
 entity.setBizType("TEST_NOTICE");
 entity.setVersion(version);
 entity.setVersionStatus(status);
 entity.setDefaultPriority(NoticePriority.NORMAL);
 return entity;
 }

 private NoticeRecipientEntity recipient(Long userId) {
 NoticeRecipientEntity recipient = new NoticeRecipientEntity();
 recipient.setId(userId + 99);
 recipient.setUserId(userId);
 return recipient;
 }

 private NoticeChannelConfigMapper routeChannelConfigMapper(NoticeChannelType channelType) {
 NoticeChannelConfigMapper mapper = mock(NoticeChannelConfigMapper.class);
 when(mapper.selectList(any())).thenReturn(List.of(channelConfig(1L, channelType)));
 return mapper;
 }

 private NoticeChannelConfigEntity channelConfig(Long id, NoticeChannelType channelType) {
 NoticeChannelConfigEntity config = new NoticeChannelConfigEntity();
 config.setId(id);
 config.setChannelType(channelType);
 config.setProviderCode("TEST");
 config.setConfigName("测试通道");
 config.setConfigJson("{}");
 config.setEnabled(true);
 config.setWeight(100);
 config.setConfigStatus(NoticeChannelConfigStatus.COMPLETE);
 return config;
 }

 private NoticeSiteMessageEntity siteMessage(Long id, Long userId) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setId(id);
 entity.setTaskId(10L);
 entity.setSendRecordId(20L);
 entity.setUserId(userId);
 entity.setTitle("测试系统消息");
 entity.setContent("这是一条系统消息内容");
 entity.setPriority(NoticePriority.NORMAL);
 entity.setReadStatus(NoticeReadStatus.UNREAD);
 entity.setDeleteStatus(NoticeDeleteStatus.NORMAL);
 entity.setBizType("TEST_NOTICE");
 entity.setBizId("BIZ-1");
 return entity;
 }

 private NoticeTaskEntity task(Long id) {
 NoticeTaskEntity task = new NoticeTaskEntity();
 task.setId(id);
 task.setBizType("TEST_NOTICE");
 task.setParamsSnapshot("{}");
 task.setStatus(NoticeTaskStatus.WAITING);
 return task;
 }

 private NoticeSendRecordEntity record(Long id, Long userId, Long recipientId) {
 NoticeSendRecordEntity record = new NoticeSendRecordEntity();
 record.setId(id);
 record.setTaskId(1L);
 record.setRecipientId(recipientId);
 record.setBusinessChannelTemplateId(1L);
 record.setChannelType(SITE);
 record.setRenderedTitle("标题" + userId);
 record.setRenderedContent("内容" + userId);
 record.setTemplateVersion(1);
 record.setStatus(NoticeSendStatus.PENDING);
 return record;
 }

 private IdentityUserInfo identityUser(Long userId, String username) {
 IdentityUserInfo user = new IdentityUserInfo();
 user.setUserId(userId);
 user.setUsername(username);
 user.setNickname(username + "昵称");
 user.setPhone("1380000000" + userId);
 user.setEmail(username + "@example.com");
 user.setStatus(1);
 return user;
 }

 private NoticeService serviceWithSender(java.util.function.Function<ChannelSendCommand, ChannelSendResult> senderFunction) {
 NoticeChannelSender sender = new NoticeChannelSender() {
 @Override
 public NoticeChannelType channelType() {
 return SITE;
 }

 @Override
 public ChannelSendResult send(ChannelSendCommand command) {
 return senderFunction.apply(command);
 }
 };
 return new NoticeService(
 messageMapper,
 businessTypeMapper,
 businessConfigVersionMapper,
 channelTemplateMapper,
 routeChannelConfigMapper(SITE),
	 taskMapper,
	 recipientMapper,
	 recipientAccountMapper,
	 receivePreferenceMapper,
	 sendRecordMapper,
 mock(NoticeSettingMapper.class),
 wecomSyncMappingMapper,
 List.of(sender),
 new ObjectMapper(),
 outboxStore,
 identityUserApi,
 sysOrgApi,
 mock(WecomDirectoryClient.class));
 }

 private void assertTaskFinalStatus(NoticeTaskStatus status, int successCount, int failCount) {
 ArgumentCaptor<NoticeTaskEntity> captor = ArgumentCaptor.forClass(NoticeTaskEntity.class);
 verify(taskMapper, times(2)).updateById(captor.capture());
 NoticeTaskEntity finalTask = captor.getAllValues().get(1);
 assertEquals(List.of(NoticeTaskStatus.SENDING, status), taskStatusUpdates);
 assertEquals(status, finalTask.getStatus());
 assertEquals(successCount, finalTask.getSuccessCount());
 assertEquals(failCount, finalTask.getFailCount());
 }

 private void assertSendRecordStatusUpdates(NoticeSendStatus... statuses) {
 assertEquals(List.of(statuses), sendRecordStatusUpdates);
 }

 private void assertTaskTotalCount(int totalCount, String channelTypes) {
 ArgumentCaptor<NoticeTaskEntity> captor = ArgumentCaptor.forClass(NoticeTaskEntity.class);
 verify(taskMapper).updateById(captor.capture());
 NoticeTaskEntity task = captor.getValue();
 assertEquals(totalCount, task.getTotalCount());
 assertEquals(channelTypes, task.getChannelTypes());
 }
}
