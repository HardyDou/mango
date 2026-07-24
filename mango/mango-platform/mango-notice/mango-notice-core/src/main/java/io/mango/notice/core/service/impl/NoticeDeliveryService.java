package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelRouteMode;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.mango.notice.api.enums.NoticeSendCancelCode;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeChannelConfigRouteTagEntity;
import io.mango.notice.core.entity.NoticeChannelRouteTagEntity;
import io.mango.notice.core.entity.NoticeReceivePreferenceEntity;
import io.mango.notice.core.entity.NoticeRecipientAccountEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
import io.mango.notice.core.entity.NoticeSendRecordEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigRouteTagMapper;
import io.mango.notice.core.mapper.NoticeChannelRouteTagMapper;
import io.mango.notice.core.mapper.NoticeReceivePreferenceMapper;
import io.mango.notice.core.mapper.NoticeRecipientAccountMapper;
import io.mango.notice.core.mapper.NoticeRecipientMapper;
import io.mango.notice.core.mapper.NoticeSendRecordMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.outbox.NoticeOutboxMessageFactory;
import io.mango.notice.core.service.INoticeDeliveryService;
import io.mango.notice.core.service.NoticeRecipientResolver;
import io.mango.notice.core.service.NoticeChannelSecretMaterializer;
import io.mango.notice.core.service.NoticeChannelSecretResolutionException;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelMessage;
import io.mango.notice.support.channel.NoticeChannelSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class NoticeDeliveryService implements INoticeDeliveryService {

 private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}|\\$\\{\\s*([^}]+?)\\s*}");
 private static final int MAX_CHANNEL_ATTEMPTS = 3;

 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeChannelRouteTagMapper routeTagMapper;
 private final NoticeChannelConfigRouteTagMapper configRouteTagMapper;
 private final NoticeTaskMapper taskMapper;
 private final NoticeRecipientMapper recipientMapper;
 private final NoticeRecipientAccountMapper recipientAccountMapper;
 private final NoticeReceivePreferenceMapper receivePreferenceMapper;
 private final NoticeSendRecordMapper sendRecordMapper;
 private final List<NoticeChannelSender> channelSenders;
 private final ObjectMapper objectMapper;
 private final IOutboxStore outboxStore;
 private final NoticeRecipientResolver recipientResolver;
 private final NoticeChannelSecretMaterializer secretMaterializer;

 @Override
 @Transactional(rollbackFor = Exception.class)
 public NoticeSendResultVO send(SendNoticeCommand command) {
 NoticeBusinessTypeEntity businessType = findBusinessType(command.getBizType());
 List<NoticeBusinessChannelTemplateEntity> templates = resolveTemplates(command, businessType);
 List<NoticeRecipientCommand> recipients = recipientResolver.resolveRecipients(command);
 Require.isTrue(!recipients.isEmpty(), NoticeCode.NOTICE_BUSINESS_ERROR, "接收用户不能为空");
 validateMessageActions(command);
 NoticeTaskEntity task = createTask(command, templates, recipients);
 int totalCount = 0;
 Set<NoticeChannelType> actualChannels = new LinkedHashSet<>();
 for (NoticeRecipientCommand recipientCommand : recipients) {
 NoticeRecipientEntity recipient = createRecipient(task.getId(), recipientCommand);
 for (NoticeBusinessChannelTemplateEntity template : templates) {
 SendDecision decision = evaluateSendDecision(businessType, template, recipient);
 createSendRecord(task, recipient, template, command, decision);
 totalCount++;
 actualChannels.add(template.getChannelType());
 if (!decision.allowed()) {
 continue;
 }
 }
 }
 Require.isTrue(totalCount > 0, NoticeCode.NOTICE_BUSINESS_ERROR, "没有可发送的通知记录");
 updateTaskTotalCount(task, totalCount, actualChannels);
 outboxStore.enqueue(NoticeOutboxMessageFactory.toOutboxMessage(task.getId(), nextAttemptAt(task)));
 return new NoticeSendResultVO(0, 0);
 }

 @Override
 public String findTaskTenantId(Long taskId) {
 Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
 return taskMapper.selectTenantIdById(taskId);
 }

 @Override
 public int executeTask(Long taskId) {
 Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
 NoticeTaskEntity task = taskMapper.selectById(taskId);
 if (task == null || task.getStatus() == NoticeTaskStatus.CANCELED) {
 return 0;
 }
 int previousSuccessCount = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
 int previousFailCount = task.getFailCount() == null ? 0 : task.getFailCount();
 task.setStatus(NoticeTaskStatus.SENDING);
 taskMapper.updateById(task);
 List<NoticeSendRecordEntity> records = sendRecordMapper.selectList(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId)
 .in(NoticeSendRecordEntity::getStatus, List.of(NoticeSendStatus.PENDING, NoticeSendStatus.RETRY_WAITING)));
 if (records.isEmpty() && hasOnlyCanceledRecords(taskId)) {
 task.setStatus(NoticeTaskStatus.CANCELED);
 task.setSuccessCount(0);
 task.setFailCount(0);
 taskMapper.updateById(task);
 return 0;
 }
 int successCount = 0;
 int failCount = 0;
 int retryWaitingCount = 0;
 int claimedCount = 0;
 for (NoticeSendRecordEntity record : records) {
 boolean retryWaiting = record.getStatus() == NoticeSendStatus.RETRY_WAITING;
 if (!claimSendRecord(record)) {
 continue;
 }
 claimedCount++;
 if (retryWaiting) {
 retryWaitingCount++;
 }
 NoticeRecipientEntity recipient = recipientMapper.selectById(record.getRecipientId());
 NoticeBusinessChannelTemplateEntity template = record.getBusinessChannelTemplateId() == null
 ? directTemplate(task, record)
 : channelTemplateMapper.selectById(record.getBusinessChannelTemplateId());
 ChannelSendResult result = sendRecord(record, recipient, template, task);
 if (result.isSuccess()) {
 successCount++;
 } else {
 failCount++;
 }
 }
 if (!records.isEmpty() && claimedCount == 0) {
 return 0;
 }
 int totalSuccessCount = previousSuccessCount + successCount;
 int totalFailCount = Math.max(0, previousFailCount - retryWaitingCount) + failCount;
 task.setSuccessCount(totalSuccessCount);
 task.setFailCount(totalFailCount);
 task.setStatus(resolveTaskStatus(totalSuccessCount, totalFailCount));
 taskMapper.updateById(task);
 return successCount;
 }

 private boolean hasOnlyCanceledRecords(Long taskId) {
 Long total = sendRecordMapper.selectCount(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId));
 Long canceled = sendRecordMapper.selectCount(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId)
 .eq(NoticeSendRecordEntity::getStatus, NoticeSendStatus.CANCELED));
 return total != null && total > 0 && total.equals(canceled);
 }

 @Override
 public boolean hasRetryWaitingRecords(Long taskId) {
 Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
 return sendRecordMapper.selectCount(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId)
 .eq(NoticeSendRecordEntity::getStatus, NoticeSendStatus.RETRY_WAITING)) > 0;
 }

 @Override
 public void finalizeRetryWaitingRecords(Long taskId, String failReason) {
 Require.notNull(taskId, NoticeCode.NOTICE_BUSINESS_ERROR, "通知任务 ID 不能为空");
 List<NoticeSendRecordEntity> records = sendRecordMapper.selectList(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId)
 .eq(NoticeSendRecordEntity::getStatus, NoticeSendStatus.RETRY_WAITING));
 for (NoticeSendRecordEntity record : records) {
 record.setStatus(NoticeSendStatus.FINAL_FAILED);
 if (StringUtils.hasText(failReason)) {
 record.setFailReason(failReason);
 }
 record.setNextRetryTime(null);
 sendRecordMapper.updateById(record);
 }
 NoticeTaskEntity task = taskMapper.selectById(taskId);
 if (task != null && task.getStatus() != NoticeTaskStatus.CANCELED && !records.isEmpty()) {
 int totalSuccessCount = task.getSuccessCount() == null ? 0 : task.getSuccessCount();
 int totalFailCount = task.getFailCount() == null ? records.size() : Math.max(task.getFailCount(), records.size());
 task.setStatus(resolveTaskStatus(totalSuccessCount, totalFailCount));
 task.setSuccessCount(totalSuccessCount);
 task.setFailCount(totalFailCount);
 taskMapper.updateById(task);
 }
 }

 private void updateChannelSendStatus(NoticeChannelConfigEntity config, ChannelSendResult result) {
 NoticeChannelConfigEntity entity = new NoticeChannelConfigEntity();
 entity.setId(config.getId());
 entity.setLastSendStatus(result.isSuccess() ? NoticeChannelSendHealthStatus.SUCCESS : NoticeChannelSendHealthStatus.FAILED);
 entity.setLastSendTime(LocalDateTime.now());
 entity.setLastFailureCode(result.isSuccess() ? null : result.getFailCode());
 entity.setLastFailureReason(result.isSuccess() ? null : result.getFailReason());
 channelConfigMapper.updateById(entity);
 }

 private NoticeBusinessTypeEntity findBusinessType(String bizType) {
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .eq(NoticeBusinessTypeEntity::getBizType, bizType)
 .eq(NoticeBusinessTypeEntity::getEnabled, true));
 if (businessType != null) {
 return businessType;
 }
 NoticeBusinessTypeEntity fallback = new NoticeBusinessTypeEntity();
 fallback.setBizType(bizType);
 fallback.setBizName(bizType);
 fallback.setDefaultPriority(NoticePriority.NORMAL);
 return fallback;
 }

 private List<NoticeBusinessChannelTemplateEntity> resolveTemplates(SendNoticeCommand command, NoticeBusinessTypeEntity businessType) {
 List<NoticeBusinessChannelTemplateEntity> templates = channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, command.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE)
 .eq(NoticeBusinessChannelTemplateEntity::getEnabled, true));
 templates = templates.stream()
 .filter(template -> template.getVersionStatus() == NoticeTemplateVersionStatus.ACTIVE)
 .filter(template -> Boolean.TRUE.equals(template.getEnabled()))
 .toList();
 Set<NoticeChannelType> requested = requestedChannels(command);
 if (!requested.isEmpty()) {
 templates = templates.stream().filter(template -> requested.contains(template.getChannelType())).toList();
 }
 if (!templates.isEmpty() || businessType.getId() != null) {
 return ensureSiteTemplate(command, businessType, templates);
 }
 if (StringUtils.hasText(command.getTitle()) || StringUtils.hasText(command.getContent())) {
 return directTemplates(command, businessType, requested);
 }
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型未配置启用渠道模板");
 }

 private List<NoticeBusinessChannelTemplateEntity> ensureSiteTemplate(SendNoticeCommand command,
 NoticeBusinessTypeEntity businessType, List<NoticeBusinessChannelTemplateEntity> templates) {
 boolean hasSite = templates.stream().anyMatch(template -> template.getChannelType() == NoticeChannelType.SITE);
 if (hasSite) {
 return templates;
 }
 NoticeBusinessChannelTemplateEntity template = new NoticeBusinessChannelTemplateEntity();
 template.setBizType(command.getBizType());
 template.setBusinessTypeId(businessType.getId());
 template.setChannelType(NoticeChannelType.SITE);
 template.setTitleTemplate(businessType.getBizName());
 template.setContentTemplate(StringUtils.hasText(command.getContent()) ? command.getContent() : businessType.getBizName());
 template.setVersion(1);
 template.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 template.setEnabled(true);
 List<NoticeBusinessChannelTemplateEntity> result = new ArrayList<>(templates);
 result.add(template);
 return result;
 }

 private Set<NoticeChannelType> requestedChannels(SendNoticeCommand command) {
 if (command.getChannelTypes() == null || command.getChannelTypes().isEmpty()) {
 return new LinkedHashSet<>();
 }
 return new LinkedHashSet<>(command.getChannelTypes());
 }

 private List<NoticeBusinessChannelTemplateEntity> directTemplates(SendNoticeCommand command, NoticeBusinessTypeEntity businessType,
 Set<NoticeChannelType> requested) {
 Set<NoticeChannelType> channels = requested.isEmpty() ? Set.of(NoticeChannelType.SITE) : requested;
 return channels.stream().map(channel -> {
 NoticeBusinessChannelTemplateEntity template = new NoticeBusinessChannelTemplateEntity();
 template.setBizType(command.getBizType());
 template.setBusinessTypeId(businessType.getId());
 template.setChannelType(channel);
 template.setTitleTemplate(command.getTitle());
 template.setContentTemplate(command.getContent());
 template.setVersion(1);
 template.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 template.setEnabled(true);
 return template;
 }).toList();
 }

 private void validateMessageActions(SendNoticeCommand command) {
 if (command.getMessageTarget() != null) {
 validateTarget(command.getMessageTarget());
 }
 if (command.getMessageActions() == null || command.getMessageActions().isEmpty()) {
 return;
 }
 Set<String> actionCodes = new LinkedHashSet<>();
 for (NoticeSiteMessageActionCommand action : command.getMessageActions()) {
 Require.notBlank(action.getActionCode(), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作编码不能为空");
 Require.notBlank(action.getActionLabel(), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作名称不能为空");
 Require.isTrue(actionCodes.add(action.getActionCode()), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息动作编码不能重复");
 NoticeSiteMessageActionInteractionType type = action.getInteractionType() == null
 ? NoticeSiteMessageActionInteractionType.EVENT
 : action.getInteractionType();
 action.setInteractionType(type);
 if (type == NoticeSiteMessageActionInteractionType.EVENT) {
 Require.notBlank(action.getEventType(), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息事件动作必须配置事件类型");
 } else {
 Require.notNull(action.getTarget(), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息路由动作必须配置目标");
 validateTarget(action.getTarget());
 }
 }
 }

 private void validateTarget(NoticeSiteMessageTargetCommand target) {
 if (target.getTargetType() == null || target.getTargetType() == NoticeSiteMessageTargetType.NONE) {
 return;
 }
 Require.isTrue(target.getTargetType() == NoticeSiteMessageTargetType.ROUTE
 || target.getTargetType() == NoticeSiteMessageTargetType.FLOW, NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息目标类型非法");
 Require.notBlank(target.getTargetKey(), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息目标键不能为空");
 }

 private void writeTaskMessageProtocol(NoticeTaskEntity task, SendNoticeCommand command) {
 task.setMessageScene(command.getMessageScene());
 NoticeSiteMessageSubjectCommand subject = command.getMessageSubject();
 if (subject != null) {
 task.setMessageSubjectType(subject.getSubjectType());
 task.setMessageSubjectId(subject.getSubjectId());
 task.setMessageSubjectName(subject.getSubjectName());
 }
 NoticeSiteMessageTargetCommand target = command.getMessageTarget();
 if (target != null) {
 task.setMessageTargetType(target.getTargetType() == null ? null : target.getTargetType().name());
 task.setMessageTargetKey(target.getTargetKey());
 task.setMessageTargetParamsJson(toJson(target.getParams() == null
         ? Collections.emptyMap() : target.getParams().toMap()));
 task.setMessageTargetOpenMode(target.getOpenMode());
 }
 task.setMessageDataJson(toJson(command.getMessageData() == null
         ? Collections.emptyMap() : command.getMessageData().toMap()));
 task.setMessageActionsJson(toJson(command.getMessageActions() == null ? Collections.emptyList() : command.getMessageActions()));
 task.setMessageExpireTime(command.getMessageExpireTime());
 }

 private NoticeSiteMessageSubjectCommand taskMessageSubject(NoticeTaskEntity task) {
 if (!StringUtils.hasText(task.getMessageSubjectType())
 && !StringUtils.hasText(task.getMessageSubjectId())
 && !StringUtils.hasText(task.getMessageSubjectName())) {
 return null;
 }
 NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
 subject.setSubjectType(task.getMessageSubjectType());
 subject.setSubjectId(task.getMessageSubjectId());
 subject.setSubjectName(task.getMessageSubjectName());
 return subject;
 }

 private NoticeSiteMessageTargetCommand taskMessageTarget(NoticeTaskEntity task) {
 if (!StringUtils.hasText(task.getMessageTargetType())) {
 return null;
 }
 NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
 target.setTargetType(NoticeSiteMessageTargetType.valueOf(task.getMessageTargetType()));
 target.setTargetKey(task.getMessageTargetKey());
 target.setParams(NoticeJsonRequest.of(fromJson(task.getMessageTargetParamsJson())));
 target.setOpenMode(task.getMessageTargetOpenMode());
 return target;
 }

 private List<NoticeSiteMessageActionCommand> readMessageActions(String value) {
 if (!StringUtils.hasText(value)) {
 return Collections.emptyList();
 }
 try {
 return objectMapper.readValue(value, new TypeReference<List<NoticeSiteMessageActionCommand>>() {
 });
 } catch (JsonProcessingException ex) {
 return Collections.emptyList();
 }
 }

 private NoticeTaskEntity createTask(SendNoticeCommand command, List<NoticeBusinessChannelTemplateEntity> templates,
 List<NoticeRecipientCommand> recipients) {
 NoticeTaskEntity task = new NoticeTaskEntity();
 task.setTaskCode("NT" + UUID.randomUUID().toString().replace("-", ""));
 task.setBizType(command.getBizType());
 task.setBizId(command.getBizId());
 task.setIdempotentKey(command.getIdempotentKey());
 task.setParamsSnapshot(toJson(taskParams(command)));
 task.setRecipientTargetsSnapshot(toJson(recipientTargetsSnapshot(command)));
 task.setChannelTypes(templates.stream().map(template -> template.getChannelType().name()).distinct().collect(Collectors.joining(",")));
 writeTaskMessageProtocol(task, command);
 task.setSendMode(command.getSendMode() == null ? NoticeSendMode.IMMEDIATE : command.getSendMode());
 task.setScheduledTime(command.getScheduledTime());
 task.setStatus(task.getSendMode() == NoticeSendMode.SCHEDULED ? NoticeTaskStatus.WAITING : NoticeTaskStatus.SENDING);
 task.setTotalCount(0);
 task.setSuccessCount(0);
 task.setFailCount(0);
 taskMapper.insert(task);
 return task;
 }

 private void updateTaskTotalCount(NoticeTaskEntity task, int totalCount, Set<NoticeChannelType> actualChannels) {
 task.setTotalCount(totalCount);
 task.setChannelTypes(actualChannels.stream()
 .map(Enum::name)
 .distinct()
 .collect(Collectors.joining(",")));
 taskMapper.updateById(task);
 }

 private Instant nextAttemptAt(NoticeTaskEntity task) {
 if (task.getSendMode() == NoticeSendMode.SCHEDULED && task.getScheduledTime() != null) {
 return task.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant();
 }
 return Instant.now();
 }

 private Map<String, Object> taskParams(SendNoticeCommand command) {
 Map<String, Object> params = command.getParams() == null
         ? new java.util.LinkedHashMap<>()
         : new java.util.LinkedHashMap<>(command.getParams().toMap());
 if (command.getAttachmentFileIds() != null && !command.getAttachmentFileIds().isEmpty()) {
 params.put("attachments", command.getAttachmentFileIds());
 }
 return params;
 }

 private List<NoticeRecipientTargetCommand> recipientTargetsSnapshot(SendNoticeCommand command) {
 List<NoticeRecipientTargetCommand> targets = new ArrayList<>();
 if (command.getRecipientTargets() != null) {
 targets.addAll(command.getRecipientTargets());
 }
 if (command.getUserId() != null) {
 NoticeRecipientTargetCommand target = new NoticeRecipientTargetCommand();
 target.setTargetType(io.mango.notice.api.enums.NoticeRecipientTargetType.USER);
 target.setTargetId(command.getUserId());
 targets.add(target);
 }
 if (command.getUserIds() != null) {
 command.getUserIds().stream()
 .filter(userId -> userId != null && !userId.equals(command.getUserId()))
 .forEach(userId -> {
 NoticeRecipientTargetCommand target = new NoticeRecipientTargetCommand();
 target.setTargetType(io.mango.notice.api.enums.NoticeRecipientTargetType.USER);
 target.setTargetId(userId);
 targets.add(target);
 });
 }
 return targets;
 }

 private NoticeRecipientEntity createRecipient(Long taskId, NoticeRecipientCommand command) {
 NoticeRecipientEntity recipient = new NoticeRecipientEntity();
 recipient.setTaskId(taskId);
 recipient.setUserId(command.getUserId());
 recipient.setRecipientName(command.getRecipientName());
 recipient.setMobile(command.getMobile());
 recipient.setEmail(command.getEmail());
 recipient.setWechatOpenid(command.getWechatOpenid());
 recipient.setWecomUserId(command.getWecomUserId());
 recipient.setDingtalkUserId(command.getDingtalkUserId());
 recipient.setExternalId(command.getExternalId());
 recipientMapper.insert(recipient);
 return recipient;
 }

 private boolean canSendToRecipient(NoticeChannelType channelType, NoticeRecipientEntity recipient) {
 return switch (channelType) {
 case SITE -> recipient.getUserId() != null;
 case SMS -> StringUtils.hasText(recipient.getMobile());
 case EMAIL -> StringUtils.hasText(recipient.getEmail());
 case WECHAT_OFFICIAL -> StringUtils.hasText(recipient.getWechatOpenid());
 case WECOM -> StringUtils.hasText(recipient.getWecomUserId());
 case DINGTALK -> StringUtils.hasText(recipient.getDingtalkUserId());
 };
 }

 private SendDecision evaluateSendDecision(NoticeBusinessTypeEntity businessType,
 NoticeBusinessChannelTemplateEntity template, NoticeRecipientEntity recipient) {
 if (template.getEnabled() != null && !template.getEnabled()) {
 return SendDecision.canceled(NoticeSendCancelCode.CHANNEL_TEMPLATE_DISABLED, "渠道模板未启用");
 }
 if (template.getChannelType() == NoticeChannelType.SITE && !canSendToRecipient(template.getChannelType(), recipient)) {
 return missingRecipientAccountDecision(template.getChannelType());
 }
 PreferenceMatch preference = effectivePreference(recipient.getUserId(), businessType, template.getChannelType());
 if (!preference.enabled()) {
 return SendDecision.canceled(preference.cancelCode(), preference.cancelReason());
 }
 AccountMatch account = applyRecipientAccount(recipient, template.getChannelType(), preference.accountId());
 if (!account.allowed()) {
 return SendDecision.canceled(account.cancelCode(), account.cancelReason());
 }
 return SendDecision.allowed(account.accountId());
 }

 private SendDecision missingRecipientAccountDecision(NoticeChannelType channelType) {
 if (channelType == NoticeChannelType.SMS) {
 return SendDecision.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING, "缺少已验证手机号");
 }
 if (channelType == NoticeChannelType.EMAIL) {
 return SendDecision.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING, "缺少已验证邮箱");
 }
 return SendDecision.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING, "缺少可用接收账户");
 }

 private PreferenceMatch effectivePreference(Long userId, NoticeBusinessTypeEntity businessType, NoticeChannelType channelType) {
 if (userId == null) {
 return PreferenceMatch.enabled(null);
 }
 NoticeReceivePreferenceEntity messageChannel = findPreference(userId, NoticeReceivePreferenceScopeType.BIZ_TYPE,
 businessType.getBizType(), channelType);
 if (messageChannel != null) {
 return toPreferenceMatch(messageChannel, NoticeSendCancelCode.USER_CHANNEL_DISABLED, "用户关闭该消息渠道");
 }
 NoticeReceivePreferenceEntity message = findPreference(userId, NoticeReceivePreferenceScopeType.BIZ_TYPE,
 businessType.getBizType(), null);
 if (message != null) {
 return toPreferenceMatch(message, NoticeSendCancelCode.USER_MESSAGE_DISABLED, "用户关闭该消息");
 }
 NoticeReceivePreferenceEntity groupChannel = findPreference(userId, NoticeReceivePreferenceScopeType.BIZ_GROUP,
 businessType.getBizGroup(), channelType);
 if (groupChannel != null) {
 return toPreferenceMatch(groupChannel, NoticeSendCancelCode.USER_CHANNEL_DISABLED, "用户关闭该业务域渠道");
 }
 NoticeReceivePreferenceEntity group = findPreference(userId, NoticeReceivePreferenceScopeType.BIZ_GROUP,
 businessType.getBizGroup(), null);
 if (group != null) {
 return toPreferenceMatch(group, NoticeSendCancelCode.USER_BIZ_GROUP_DISABLED, "用户关闭该业务域");
 }
 NoticeReceivePreferenceEntity globalChannel = findPreference(userId, NoticeReceivePreferenceScopeType.GLOBAL,
 null, channelType);
 if (globalChannel != null) {
 return toPreferenceMatch(globalChannel, NoticeSendCancelCode.USER_CHANNEL_DISABLED, "用户关闭该渠道");
 }
 NoticeReceivePreferenceEntity global = findPreference(userId, NoticeReceivePreferenceScopeType.GLOBAL, null, null);
 if (global != null) {
 return toPreferenceMatch(global, NoticeSendCancelCode.USER_MESSAGE_DISABLED, "用户关闭全部通知");
 }
 return PreferenceMatch.enabled(null);
 }

 private PreferenceMatch toPreferenceMatch(NoticeReceivePreferenceEntity preference, NoticeSendCancelCode cancelCode,
 String cancelReason) {
 if (Boolean.FALSE.equals(preference.getEnabled())) {
 return PreferenceMatch.canceled(cancelCode, cancelReason);
 }
 return PreferenceMatch.enabled(preference.getAccountId());
 }

 private NoticeReceivePreferenceEntity findPreference(Long userId, NoticeReceivePreferenceScopeType scopeType,
 String scopeValue, NoticeChannelType channelType) {
 if (userId == null || scopeType == null) {
 return null;
 }
 LambdaQueryWrapper<NoticeReceivePreferenceEntity> wrapper = new LambdaQueryWrapper<NoticeReceivePreferenceEntity>()
 .eq(NoticeReceivePreferenceEntity::getUserId, userId)
 .eq(NoticeReceivePreferenceEntity::getScopeType, scopeType)
 .eq(NoticeReceivePreferenceEntity::getScopeValue, normalizeScopeValue(scopeValue));
 if (channelType == null) {
 wrapper.isNull(NoticeReceivePreferenceEntity::getChannelType);
 } else {
 wrapper.eq(NoticeReceivePreferenceEntity::getChannelType, channelType);
 }
 return receivePreferenceMapper.selectOne(wrapper);
 }

 private AccountMatch applyRecipientAccount(NoticeRecipientEntity recipient, NoticeChannelType channelType, Long accountId) {
 if (channelType == NoticeChannelType.SITE) {
 return AccountMatch.allowed(null);
 }
 NoticeRecipientAccountType accountType = accountType(channelType);
 if (accountType == null || recipient.getUserId() == null) {
 return canSendToRecipient(channelType, recipient)
 ? AccountMatch.allowed(null)
 : AccountMatch.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING, "缺少接收账户");
 }
 NoticeRecipientAccountEntity account = accountId == null
 ? defaultVerifiedAccount(recipient.getUserId(), accountType)
 : recipientAccountMapper.selectById(accountId);
 if (account == null && canSendToRecipient(channelType, recipient)) {
 return AccountMatch.allowed(null);
 }
 if (account == null) {
 return AccountMatch.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_MISSING, "缺少接收账户");
 }
 if (!Boolean.TRUE.equals(account.getEnabled()) || account.getVerifiedStatus() == NoticeRecipientAccountStatus.DISABLED) {
 return AccountMatch.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_UNVERIFIED, "接收账户不可用");
 }
 if (account.getVerifiedStatus() != NoticeRecipientAccountStatus.VERIFIED) {
 return AccountMatch.canceled(NoticeSendCancelCode.RECIPIENT_ACCOUNT_UNVERIFIED, "接收账户未验证");
 }
 applyRecipientAccountValue(recipient, channelType, account);
 return AccountMatch.allowed(account.getId());
 }

 private NoticeRecipientAccountEntity defaultVerifiedAccount(Long userId, NoticeRecipientAccountType accountType) {
 List<NoticeRecipientAccountEntity> accounts = recipientAccountMapper.selectList(new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getUserId, userId)
 .eq(NoticeRecipientAccountEntity::getAccountType, accountType)
 .eq(NoticeRecipientAccountEntity::getVerifiedStatus, NoticeRecipientAccountStatus.VERIFIED)
 .eq(NoticeRecipientAccountEntity::getEnabled, true)
 .orderByDesc(NoticeRecipientAccountEntity::getDefaultAccount)
 .orderByDesc(NoticeRecipientAccountEntity::getUpdatedAt));
 if (accounts == null) {
 return null;
 }
 return accounts.isEmpty() ? null : accounts.get(0);
 }

 private void applyRecipientAccountValue(NoticeRecipientEntity recipient, NoticeChannelType channelType,
 NoticeRecipientAccountEntity account) {
 if (channelType == NoticeChannelType.SMS) {
 recipient.setMobile(account.getAccountValue());
 } else if (channelType == NoticeChannelType.EMAIL) {
 recipient.setEmail(account.getAccountValue());
 } else if (channelType == NoticeChannelType.WECHAT_OFFICIAL) {
 recipient.setWechatOpenid(account.getAccountValue());
 } else if (channelType == NoticeChannelType.WECOM) {
 recipient.setWecomUserId(account.getAccountValue());
 } else if (channelType == NoticeChannelType.DINGTALK) {
 recipient.setDingtalkUserId(account.getAccountValue());
 }
 recipientMapper.updateById(recipient);
 }

 private NoticeRecipientAccountType accountType(NoticeChannelType channelType) {
 return switch (channelType) {
 case SMS -> NoticeRecipientAccountType.MOBILE;
 case EMAIL -> NoticeRecipientAccountType.EMAIL;
 case WECHAT_OFFICIAL -> NoticeRecipientAccountType.WECHAT;
 case WECOM -> NoticeRecipientAccountType.WECOM;
 case DINGTALK -> NoticeRecipientAccountType.DINGTALK;
 case SITE -> null;
 };
 }

 private NoticeSendRecordEntity createSendRecord(NoticeTaskEntity task, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, SendNoticeCommand command, SendDecision decision) {
 Map<String, Object> params = taskParams(command);
 NoticeSendRecordEntity record = new NoticeSendRecordEntity();
 record.setTaskId(task.getId());
 record.setRecipientId(recipient.getId());
 record.setBizType(task.getBizType());
 record.setBizId(task.getBizId());
 record.setBusinessChannelTemplateId(template.getId());
 record.setTemplateVersion(template.getVersion());
 record.setChannelType(template.getChannelType());
 record.setRequestId("NR" + UUID.randomUUID().toString().replace("-", ""));
 record.setStatus(decision.allowed() ? NoticeSendStatus.PENDING : NoticeSendStatus.CANCELED);
 record.setRenderedTitle(render(template.getTitleTemplate(), params));
 record.setRenderedContent(render(template.getContentTemplate(), params));
 record.setRequestSnapshot(toJson(sendRecordRequestSnapshot(task, recipient, template, params, decision)));
 record.setFailCode(decision.cancelCode());
 record.setFailReason(decision.cancelReason());
 record.setRetryCount(0);
 sendRecordMapper.insert(record);
 return record;
 }

 private Map<String, Object> sendRecordRequestSnapshot(NoticeTaskEntity task, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, Map<String, Object> params, SendDecision decision) {
 Map<String, Object> snapshot = new LinkedHashMap<>();
 snapshot.put("bizType", task.getBizType());
 snapshot.put("bizId", task.getBizId());
 snapshot.put("taskId", task.getId());
 snapshot.put("recipientId", recipient.getId());
 snapshot.put("userId", recipient.getUserId());
 snapshot.put("channelType", template.getChannelType().name());
 snapshot.put("businessChannelTemplateId", template.getId());
 snapshot.put("templateVersion", template.getVersion());
 snapshot.put("accountId", decision.accountId());
 snapshot.put("cancelCode", decision.cancelCode());
 snapshot.put("params", params == null ? Collections.emptyMap() : params);
 return snapshot;
 }

 private ChannelSendResult sendRecord(NoticeSendRecordEntity record, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, NoticeTaskEntity task) {
 NoticeChannelSender sender = senderMap().get(template.getChannelType());
 ChannelSendResult result;
 if (sender == null) {
 result = ChannelSendResult.failed(NoticeFailureCode.CHANNEL_UNAVAILABLE.name(), "通知渠道未装配", false);
 } else {
 result = sendWithRoute(sender, task, record, recipient, template);
 }
 record.setStatus(recordStatus(result));
 record.setProviderMessageId(result.getProviderMessageId());
 record.setFailCode(result.getFailCode());
 record.setFailReason(result.getFailReason());
 record.setResponseSnapshot(result.getResponseSnapshot());
 if (result.isSuccess()) {
 record.setNextRetryTime(null);
 } else if (result.isRetryable()) {
 record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
 record.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
 }
 record.setSentAt(LocalDateTime.now());
 sendRecordMapper.updateById(record);
 return result;
 }

 private boolean claimSendRecord(NoticeSendRecordEntity record) {
 NoticeSendRecordEntity update = new NoticeSendRecordEntity();
 update.setStatus(NoticeSendStatus.SENDING);
 int updated = sendRecordMapper.update(update, new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getId, record.getId())
 .in(NoticeSendRecordEntity::getStatus, List.of(NoticeSendStatus.PENDING, NoticeSendStatus.RETRY_WAITING)));
 if (updated > 0) {
 record.setStatus(NoticeSendStatus.SENDING);
 }
 return updated > 0;
 }

 private ChannelSendResult sendWithRoute(NoticeChannelSender sender, NoticeTaskEntity task, NoticeSendRecordEntity record,
 NoticeRecipientEntity recipient, NoticeBusinessChannelTemplateEntity template) {
 List<NoticeChannelConfigEntity> configs = routeChannelConfigs(template, record.getId());
 if (configs.isEmpty()) {
 if (routeMode(template) == NoticeChannelRouteMode.TAG) {
 return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_ROUTE_TAG_UNAVAILABLE.name(),
 "路由标签没有可用通知通道", false);
 }
 return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_UNAVAILABLE.name(), "没有可用通知通道", true);
 }
 ChannelSendResult lastResult = null;
 for (NoticeChannelConfigEntity config : configs) {
 for (int attempt = 1; attempt <= MAX_CHANNEL_ATTEMPTS; attempt++) {
 ChannelSendResult result;
 try {
 NoticeChannelMessage command = toChannelCommand(task, record, recipient, template, config);
 result = sender.send(command);
 } catch (NoticeChannelSecretResolutionException ex) {
 result = ChannelSendResult.failed(NoticeFailureCode.CHANNEL_CONFIG_INVALID.name(), ex.getMessage(), false);
 }
 lastResult = result;
 record.setChannelConfigId(config.getId());
 updateChannelSendStatus(config, result);
 if (result.isSuccess()) {
 return result;
 }
 if (!result.isRetryable()) {
 return result;
 }
 }
 }
 return lastResult == null
 ? ChannelSendResult.failed(NoticeFailureCode.CHANNEL_UNAVAILABLE.name(), "没有可用通知通道", true)
 : lastResult;
 }

 private NoticeSendStatus recordStatus(ChannelSendResult result) {
 if (result.isSuccess()) {
 return NoticeSendStatus.SUCCESS;
 }
 return result.isRetryable() ? NoticeSendStatus.RETRY_WAITING : NoticeSendStatus.FAILED;
 }

 private NoticeChannelMessage toChannelCommand(NoticeTaskEntity task, NoticeSendRecordEntity record,
 NoticeRecipientEntity recipient, NoticeBusinessChannelTemplateEntity template, NoticeChannelConfigEntity config) {
 NoticeChannelMessage sendCommand = new NoticeChannelMessage();
 sendCommand.setTaskId(record.getTaskId());
 sendCommand.setSendRecordId(record.getId());
 sendCommand.setUserId(recipient.getUserId());
 sendCommand.setRecipientName(recipient.getRecipientName());
 sendCommand.setMobile(recipient.getMobile());
 sendCommand.setEmail(recipient.getEmail());
 sendCommand.setWechatOpenid(recipient.getWechatOpenid());
 sendCommand.setWecomUserId(recipient.getWecomUserId());
 sendCommand.setDingtalkUserId(recipient.getDingtalkUserId());
 sendCommand.setTitle(record.getRenderedTitle());
 sendCommand.setContent(record.getRenderedContent());
 sendCommand.setMessageScene(task.getMessageScene());
 sendCommand.setMessageSubject(taskMessageSubject(task));
 sendCommand.setMessageTarget(taskMessageTarget(task));
 sendCommand.setMessageData(fromJson(task.getMessageDataJson()));
 sendCommand.setMessageActions(readMessageActions(task.getMessageActionsJson()));
 sendCommand.setMessageExpireTime(task.getMessageExpireTime());
 sendCommand.setAttachmentFileIds(attachmentFileIds(task.getParamsSnapshot()));
 sendCommand.setPriority(NoticePriority.NORMAL);
 sendCommand.setBizType(task.getBizType());
 sendCommand.setBizId(task.getBizId());
 sendCommand.setParams(fromJson(task.getParamsSnapshot()));
 sendCommand.setChannelConfigId(config.getId());
 sendCommand.setChannelProviderCode(config.getProviderCode());
 sendCommand.setChannelConfigName(config.getConfigName());
 sendCommand.setChannelConfigJson(secretMaterializer.materialize(config));
 sendCommand.setChannelTemplateId(template.getChannelTemplateId());
 sendCommand.setVariableMapping(template.getVariableMapping());
 return sendCommand;
 }

 private NoticeBusinessChannelTemplateEntity directTemplate(NoticeTaskEntity task, NoticeSendRecordEntity record) {
 NoticeBusinessChannelTemplateEntity template = new NoticeBusinessChannelTemplateEntity();
 template.setBizType(task.getBizType());
 template.setChannelType(record.getChannelType());
 template.setTitleTemplate(record.getRenderedTitle());
 template.setContentTemplate(record.getRenderedContent());
 template.setVersion(record.getTemplateVersion());
 template.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 template.setEnabled(true);
 return template;
 }

 private List<NoticeChannelConfigEntity> routeChannelConfigs(NoticeBusinessChannelTemplateEntity template, Long seed) {
 NoticeChannelRouteMode routeMode = routeMode(template);
 if (routeMode == NoticeChannelRouteMode.EXACT) {
 NoticeChannelConfigEntity config = channelConfigMapper.selectById(template.getChannelConfigId());
 if (config == null || config.getChannelType() != template.getChannelType()
 || !Boolean.TRUE.equals(config.getEnabled())) {
 return Collections.emptyList();
 }
 if (config.getConfigStatus() == NoticeChannelConfigStatus.INCOMPLETE) {
 return Collections.emptyList();
 }
 return List.of(config);
 }
 List<Long> taggedConfigIds = null;
 if (routeMode == NoticeChannelRouteMode.TAG) {
 if (!StringUtils.hasText(template.getRouteTagCode())) {
 return Collections.emptyList();
 }
 NoticeChannelRouteTagEntity tag = routeTagMapper.selectOne(new LambdaQueryWrapper<NoticeChannelRouteTagEntity>()
 .eq(NoticeChannelRouteTagEntity::getChannelType, template.getChannelType())
 .eq(NoticeChannelRouteTagEntity::getTagCode, template.getRouteTagCode())
 .last("limit 1"));
 if (tag == null) {
 return Collections.emptyList();
 }
 taggedConfigIds = configRouteTagMapper.selectList(new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getRouteTagId, tag.getId())).stream()
 .map(NoticeChannelConfigRouteTagEntity::getChannelConfigId).toList();
 if (taggedConfigIds.isEmpty()) {
 return Collections.emptyList();
 }
 }
 LambdaQueryWrapper<NoticeChannelConfigEntity> wrapper = new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getChannelType, template.getChannelType())
 .eq(NoticeChannelConfigEntity::getEnabled, true)
 .eq(NoticeChannelConfigEntity::getConfigStatus, NoticeChannelConfigStatus.COMPLETE);
 if (taggedConfigIds != null) {
 wrapper.in(NoticeChannelConfigEntity::getId, taggedConfigIds);
 }
 List<NoticeChannelConfigEntity> configs = channelConfigMapper.selectList(wrapper);
 if (configs.isEmpty()) {
 return Collections.emptyList();
 }
 return orderedRotation(configs, seed == null ? 0L : seed);
 }

 private NoticeChannelRouteMode routeMode(NoticeBusinessChannelTemplateEntity template) {
 if (template.getRouteMode() != null) {
 return template.getRouteMode();
 }
 return template.getChannelConfigId() == null ? NoticeChannelRouteMode.AUTO : NoticeChannelRouteMode.EXACT;
 }

 private List<NoticeChannelConfigEntity> orderedRotation(List<NoticeChannelConfigEntity> configs, long seed) {
 Map<String, List<NoticeChannelConfigEntity>> groups = configs.stream()
 .sorted(Comparator.comparingInt(this::priority).thenComparingInt(this::healthRank))
 .collect(Collectors.groupingBy(config -> priority(config) + ":" + healthRank(config),
 LinkedHashMap::new, Collectors.toList()));
 List<NoticeChannelConfigEntity> ordered = new ArrayList<>();
 long groupSeed = seed;
 for (List<NoticeChannelConfigEntity> group : groups.values()) {
 ordered.addAll(weightedRotation(group, groupSeed++));
 }
 return ordered;
 }

 private int priority(NoticeChannelConfigEntity config) {
 return config.getPriority() == null ? 0 : config.getPriority();
 }

 private int healthRank(NoticeChannelConfigEntity config) {
 return config.getLastSendStatus() == NoticeChannelSendHealthStatus.FAILED ? 1 : 0;
 }

 private List<NoticeChannelConfigEntity> weightedRotation(List<NoticeChannelConfigEntity> configs, long seed) {
 List<NoticeChannelConfigEntity> weighted = new ArrayList<>();
 for (NoticeChannelConfigEntity config : configs) {
 int weight = config.getWeight() == null || config.getWeight() <= 0 ? 1 : config.getWeight();
 for (int i = 0; i < weight; i++) {
 weighted.add(config);
 }
 }
 if (weighted.isEmpty()) {
 return configs;
 }
 int start = (int) Math.floorMod(seed, weighted.size());
 List<NoticeChannelConfigEntity> ordered = new ArrayList<>();
 Set<Long> seen = new LinkedHashSet<>();
 for (int i = 0; i < weighted.size(); i++) {
 NoticeChannelConfigEntity config = weighted.get((start + i) % weighted.size());
 if (seen.add(config.getId())) {
 ordered.add(config);
 }
 }
 return ordered;
 }

 private String normalizeScopeValue(String scopeValue) {
 return StringUtils.hasText(scopeValue) ? scopeValue : "";
 }

 private Map<String, Object> fromJson(String value) {
 if (!StringUtils.hasText(value)) {
 return Collections.emptyMap();
 }
 try {
 return objectMapper.readValue(value, Map.class);
 } catch (JsonProcessingException ex) {
 return Collections.emptyMap();
 }
 }

 private List<Long> attachmentFileIds(String paramsSnapshot) {
 Map<String, Object> params = fromJson(paramsSnapshot);
 Object value = params.get("attachments");
 if (value == null) {
 return Collections.emptyList();
 }
 if (value instanceof List<?> values) {
 return values.stream()
 .map(this::toLong)
 .filter(item -> item != null)
 .toList();
 }
 Long single = toLong(value);
 return single == null ? Collections.emptyList() : List.of(single);
 }

 private Long toLong(Object value) {
 if (value instanceof Number number) {
 return number.longValue();
 }
 if (value instanceof String text && StringUtils.hasText(text)) {
 try {
 return Long.valueOf(text);
 } catch (NumberFormatException ex) {
 return null;
 }
 }
 return null;
 }

 private String render(String template, Map<String, Object> params) {
 if (template == null) {
 return "";
 }
 if (params == null || params.isEmpty()) {
 return template;
 }
 Matcher matcher = TEMPLATE_VARIABLE.matcher(template);
 StringBuffer buffer = new StringBuffer();
 while (matcher.find()) {
 String variableName = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
 Object value = params.get(variableName.trim());
 matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
 }
 matcher.appendTail(buffer);
 return buffer.toString();
 }

 private NoticeTaskStatus resolveTaskStatus(int successCount, int failCount) {
 if (successCount > 0 && failCount == 0) {
 return NoticeTaskStatus.SUCCESS;
 }
 if (successCount > 0) {
 return NoticeTaskStatus.PARTIAL_SUCCESS;
 }
 return NoticeTaskStatus.FAILED;
 }

 private record SendDecision(boolean allowed, Long accountId, String cancelCode, String cancelReason) {

 static SendDecision allowed(Long accountId) {
 return new SendDecision(true, accountId, null, null);
 }

 static SendDecision canceled(NoticeSendCancelCode cancelCode, String cancelReason) {
 return new SendDecision(false, null, cancelCode.name(), cancelReason);
 }
 }

 private record PreferenceMatch(boolean enabled, Long accountId, NoticeSendCancelCode cancelCode, String cancelReason) {

 static PreferenceMatch enabled(Long accountId) {
 return new PreferenceMatch(true, accountId, null, null);
 }

 static PreferenceMatch canceled(NoticeSendCancelCode cancelCode, String cancelReason) {
 return new PreferenceMatch(false, null, cancelCode, cancelReason);
 }
 }

 private record AccountMatch(boolean allowed, Long accountId, NoticeSendCancelCode cancelCode, String cancelReason) {

 static AccountMatch allowed(Long accountId) {
 return new AccountMatch(true, accountId, null, null);
 }

 static AccountMatch canceled(NoticeSendCancelCode cancelCode, String cancelReason) {
 return new AccountMatch(false, null, cancelCode, cancelReason);
 }
 }

 private Map<NoticeChannelType, NoticeChannelSender> senderMap() {
 return channelSenders.stream().collect(Collectors.toMap(NoticeChannelSender::channelType, Function.identity()));
 }

 private List<Long> receiverIds(SendNoticeCommand command) {
 return Stream.concat(command.getUserId() == null ? Stream.empty() : Stream.of(command.getUserId()),
 command.getUserIds() == null ? Stream.empty() : command.getUserIds().stream())
 .distinct()
 .toList();
 }

 private String toJson(Object value) {
 try {
 return objectMapper.writeValueAsString(value);
 } catch (JsonProcessingException ex) {
 return "{}";
 }
 }

}
