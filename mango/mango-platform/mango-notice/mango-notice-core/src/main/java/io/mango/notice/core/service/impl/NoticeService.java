package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeSyncStatus;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.core.convert.NoticeBusinessConfigVersionConvert;
import io.mango.notice.core.convert.NoticeBusinessTypeConvert;
import io.mango.notice.core.convert.NoticeChannelConfigConvert;
import io.mango.notice.core.convert.NoticeChannelTemplateConvert;
import io.mango.notice.core.convert.NoticeSendRecordConvert;
import io.mango.notice.core.convert.NoticeSiteMessageConvert;
import io.mango.notice.core.convert.NoticeTaskConvert;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
import io.mango.notice.core.entity.NoticeSendRecordEntity;
import io.mango.notice.core.entity.NoticeSettingEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeRecipientMapper;
import io.mango.notice.core.mapper.NoticeSendRecordMapper;
import io.mango.notice.core.mapper.NoticeSettingMapper;
import io.mango.notice.core.mapper.NoticeSiteMessageMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.outbox.NoticeOutboxMessageMapper;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
@RequiredArgsConstructor
public class NoticeService implements INoticeService {

 private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");
 private static final int MAX_CHANNEL_ATTEMPTS = 3;
 private static final String MASKED_VALUE = "***";
 private static final String SITE_INTERNAL_PROVIDER = "INTERNAL";
 private static final Set<String> SENSITIVE_CONFIG_KEYS = Set.of("secret", "password", "token", "key", "appSecret",
 "accessKey", "accessKeySecret", "secretKey", "smtpPassword");
 private final NoticeSiteMessageMapper messageMapper;
 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessConfigVersionMapper businessConfigVersionMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeTaskMapper taskMapper;
 private final NoticeRecipientMapper recipientMapper;
 private final NoticeSendRecordMapper sendRecordMapper;
 private final NoticeSettingMapper settingMapper;
 private final List<NoticeChannelSender> channelSenders;
 private final ObjectMapper objectMapper;
 private final IOutboxStore outboxStore;

 @Override
 public NoticeSendResultVO send(SendNoticeCommand command) {
 NoticeBusinessTypeEntity businessType = findBusinessType(command.getBizType());
 List<NoticeBusinessChannelTemplateEntity> templates = resolveTemplates(command, businessType);
 List<NoticeRecipientCommand> recipients = resolveRecipients(command);
 NoticeTaskEntity task = createTask(command, templates, recipients);
 for (NoticeRecipientCommand recipientCommand : recipients) {
 NoticeRecipientEntity recipient = createRecipient(task.getId(), recipientCommand);
 for (NoticeBusinessChannelTemplateEntity template : templates) {
 createSendRecord(task, recipient, template, command);
 }
 }
 outboxStore.enqueue(NoticeOutboxMessageMapper.toOutboxMessage(task.getId(), nextAttemptAt(task)));
 return new NoticeSendResultVO(0, 0);
 }

 @Override
 public int executeTask(Long taskId) {
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
 int successCount = 0;
 int failCount = 0;
 int retryWaitingCount = 0;
 for (NoticeSendRecordEntity record : records) {
 if (record.getStatus() == NoticeSendStatus.RETRY_WAITING) {
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
 int totalSuccessCount = previousSuccessCount + successCount;
 int totalFailCount = Math.max(0, previousFailCount - retryWaitingCount) + failCount;
 task.setSuccessCount(totalSuccessCount);
 task.setFailCount(totalFailCount);
 task.setStatus(resolveTaskStatus(totalSuccessCount, totalFailCount));
 taskMapper.updateById(task);
 return successCount;
 }

 @Override
 public boolean hasRetryWaitingRecords(Long taskId) {
 Require.notNull(taskId, "通知任务 ID 不能为空");
 return sendRecordMapper.selectCount(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId)
 .eq(NoticeSendRecordEntity::getStatus, NoticeSendStatus.RETRY_WAITING)) > 0;
 }

 @Override
 public void finalizeRetryWaitingRecords(Long taskId, String failReason) {
 Require.notNull(taskId, "通知任务 ID 不能为空");
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

 @Override
 public PageResult<NoticeBusinessTypeVO> listBusinessTypes(NoticeBusinessTypePageQuery query) {
 LambdaQueryWrapper<NoticeBusinessTypeEntity> wrapper = new LambdaQueryWrapper<>();
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.like(NoticeBusinessTypeEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizGroup())) {
 wrapper.eq(NoticeBusinessTypeEntity::getBizGroup, query.getBizGroup());
 }
 if (query.getEnabled() != null) {
 wrapper.eq(NoticeBusinessTypeEntity::getEnabled, query.getEnabled());
 }
 wrapper.orderByDesc(NoticeBusinessTypeEntity::getCreatedAt);
 Page<NoticeBusinessTypeEntity> result = businessTypeMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(this::toBusinessTypeVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public NoticeBusinessTypeVO createBusinessType(CreateNoticeBusinessTypeCommand command) {
 Require.notNull(command, "业务通知配置不能为空");
 Require.notBlank(command.getBizType(), "业务类型不能为空");
 Require.notBlank(command.getBizName(), "名称不能为空");
 NoticeBusinessTypeEntity entity = new NoticeBusinessTypeEntity();
 entity.setBizType(command.getBizType());
 entity.setBizName(command.getBizName());
 entity.setBizGroup(command.getBizGroup());
 entity.setDescription(command.getDescription());
 entity.setParamsSchema(command.getParamsSchema());
 entity.setEnabled(true);
 entity.setDefaultPriority(command.getDefaultPriority());
 entity.setIdempotentStrategy(command.getIdempotentStrategy());
 businessTypeMapper.insert(entity);
 saveBusinessConfigDraft(entity.getId(), draftCommand(entity));
 return toBusinessTypeVO(entity);
 }

 @Override
 public NoticeBusinessTypeVO updateBusinessType(Long id, UpdateNoticeBusinessTypeCommand command) {
 Require.notNull(id, "业务类型 ID 不能为空");
 Require.notNull(command, "业务通知配置不能为空");
 Require.notBlank(command.getBizName(), "名称不能为空");
 NoticeBusinessTypeEntity entity = businessTypeMapper.selectById(id);
 Require.notNull(entity, "业务类型不存在");
 entity.setBizName(command.getBizName());
 entity.setBizGroup(command.getBizGroup());
 entity.setDescription(command.getDescription());
 businessTypeMapper.updateById(entity);
 saveBusinessConfigDraft(entity.getId(), draftCommand(entity));
 return toBusinessTypeVO(entity);
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean deleteBusinessType(Long id) {
 Require.notNull(id, "业务类型 ID 不能为空");
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectById(id);
 Require.notNull(businessType, "业务类型不存在");
 Long runningTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<NoticeTaskEntity>()
 .eq(NoticeTaskEntity::getBizType, businessType.getBizType())
 .in(NoticeTaskEntity::getStatus, List.of(NoticeTaskStatus.WAITING, NoticeTaskStatus.SENDING)));
 Require.isTrue(runningTaskCount == null || runningTaskCount == 0, "存在待发送或发送中的通知任务，不能删除");
 channelTemplateMapper.delete(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType()));
 businessConfigVersionMapper.delete(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType()));
 return businessTypeMapper.deleteById(id) > 0;
 }

 @Override
 public boolean enableBusinessType(Long id) {
 NoticeBusinessTypeEntity entity = new NoticeBusinessTypeEntity();
 entity.setId(id);
 entity.setEnabled(true);
 return businessTypeMapper.updateById(entity) > 0;
 }

 @Override
 public boolean disableBusinessType(Long id) {
 NoticeBusinessTypeEntity entity = new NoticeBusinessTypeEntity();
 entity.setId(id);
 entity.setEnabled(false);
 return businessTypeMapper.updateById(entity) > 0;
 }

 @Override
 public List<NoticeBusinessConfigVersionVO> listBusinessConfigVersions(Long businessTypeId) {
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 return businessConfigVersionMapper.selectList(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .orderByAsc(NoticeBusinessConfigVersionEntity::getVersionStatus)
 .orderByDesc(NoticeBusinessConfigVersionEntity::getVersion))
 .stream().map(NoticeBusinessConfigVersionConvert::toVO).toList();
 }

 @Override
 public NoticeBusinessConfigVersionVO saveBusinessConfigDraft(Long businessTypeId,
 SaveNoticeBusinessConfigCommand command) {
 Require.notNull(command, "业务发布配置不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessConfigVersionEntity draft = latestBusinessConfigVersion(businessType.getBizType(),
 NoticeTemplateVersionStatus.DRAFT);
 if (draft == null) {
 draft = new NoticeBusinessConfigVersionEntity();
 draft.setBusinessTypeId(businessTypeId);
 draft.setBizType(businessType.getBizType());
 draft.setVersion(nextBusinessConfigVersion(businessType.getBizType()));
 draft.setVersionStatus(NoticeTemplateVersionStatus.DRAFT);
 }
 draft.setParamsSchema(command.getParamsSchema());
 draft.setDefaultPriority(command.getDefaultPriority() == null ? NoticePriority.NORMAL : command.getDefaultPriority());
 draft.setIdempotentStrategy(command.getIdempotentStrategy());
 if (draft.getId() == null) {
 businessConfigVersionMapper.insert(draft);
 } else {
 businessConfigVersionMapper.updateById(draft);
 }
 return NoticeBusinessConfigVersionConvert.toVO(draft);
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean publishBusinessConfigDraft(Long businessTypeId) {
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessConfigVersionEntity draft = latestBusinessConfigVersion(businessType.getBizType(),
 NoticeTemplateVersionStatus.DRAFT);
 Require.notNull(draft, "没有可发布的业务配置草稿");
 businessConfigVersionMapper.selectList(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessConfigVersionEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE))
 .forEach(active -> {
 active.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 businessConfigVersionMapper.updateById(active);
 });
 draft.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 draft.setPublishTime(LocalDateTime.now());
 businessConfigVersionMapper.updateById(draft);
 businessType.setParamsSchema(draft.getParamsSchema());
 businessType.setDefaultPriority(draft.getDefaultPriority());
 businessType.setIdempotentStrategy(draft.getIdempotentStrategy());
 return businessTypeMapper.updateById(businessType) > 0;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean activateBusinessConfigVersion(Long businessTypeId, Integer version) {
 Require.notNull(version, "版本号不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessConfigVersionEntity source = businessConfigVersionMapper.selectOne(
 new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessConfigVersionEntity::getVersion, version)
 .last("limit 1"));
 Require.notNull(source, "业务配置版本不存在");
 if (source.getVersionStatus() == NoticeTemplateVersionStatus.ACTIVE) {
 return true;
 }
 businessConfigVersionMapper.selectList(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessConfigVersionEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE))
 .forEach(active -> {
 active.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 businessConfigVersionMapper.updateById(active);
 });
 businessConfigVersionMapper.selectList(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessConfigVersionEntity::getVersionStatus, NoticeTemplateVersionStatus.DRAFT))
 .forEach(draft -> {
 draft.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 businessConfigVersionMapper.updateById(draft);
 });
 NoticeBusinessConfigVersionEntity activated = new NoticeBusinessConfigVersionEntity();
 activated.setBusinessTypeId(businessTypeId);
 activated.setBizType(businessType.getBizType());
 activated.setParamsSchema(source.getParamsSchema());
 activated.setDefaultPriority(source.getDefaultPriority());
 activated.setIdempotentStrategy(source.getIdempotentStrategy());
 activated.setVersion(nextBusinessConfigVersion(businessType.getBizType()));
 activated.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 activated.setPublishTime(LocalDateTime.now());
 businessConfigVersionMapper.insert(activated);
 activateChannelTemplatesFromVersion(businessType, version);
 businessType.setParamsSchema(activated.getParamsSchema());
 businessType.setDefaultPriority(activated.getDefaultPriority());
 businessType.setIdempotentStrategy(activated.getIdempotentStrategy());
 return businessTypeMapper.updateById(businessType) > 0;
 }

 @Override
 public List<NoticeChannelTemplateVO> listChannelTemplates(Long businessTypeId) {
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 return channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType())
 .orderByAsc(NoticeBusinessChannelTemplateEntity::getChannelType)
 .orderByDesc(NoticeBusinessChannelTemplateEntity::getVersion))
 .stream().map(NoticeChannelTemplateConvert::toVO).toList();
 }

 @Override
 public NoticeChannelTemplateVO saveChannelTemplate(Long businessTypeId, NoticeChannelType channelType,
 SaveNoticeChannelTemplateCommand command) {
 Require.notNull(channelType, "渠道类型不能为空");
 Require.notNull(command, "渠道模板不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessChannelTemplateEntity draft = latestChannelTemplate(businessType.getBizType(), channelType,
 NoticeTemplateVersionStatus.DRAFT);
 if (draft == null) {
 draft = new NoticeBusinessChannelTemplateEntity();
 draft.setBusinessTypeId(businessTypeId);
 draft.setBizType(businessType.getBizType());
 draft.setChannelType(channelType);
 draft.setVersion(nextTemplateVersion(businessType.getBizType(), channelType));
 draft.setVersionStatus(NoticeTemplateVersionStatus.DRAFT);
 }
 draft.setTemplateName(command.getTemplateName());
 draft.setTitleTemplate(command.getTitleTemplate());
 draft.setContentTemplate(command.getContentTemplate());
 draft.setChannelTemplateId(command.getChannelTemplateId());
 draft.setVariableMapping(command.getVariableMapping());
 draft.setEnabled(command.getEnabled() == null ? Boolean.TRUE : command.getEnabled());
 draft.setChannelConfigId(command.getChannelConfigId());
 if (draft.getId() == null) {
 channelTemplateMapper.insert(draft);
 } else {
 channelTemplateMapper.updateById(draft);
 }
 return NoticeChannelTemplateConvert.toVO(draft);
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean publishChannelTemplate(Long businessTypeId, NoticeChannelType channelType) {
 Require.notNull(channelType, "渠道类型不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessChannelTemplateEntity draft = latestChannelTemplate(businessType.getBizType(), channelType,
 NoticeTemplateVersionStatus.DRAFT);
 Require.notNull(draft, "没有可发布的渠道模板草稿");
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, channelType)
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE))
 .forEach(active -> {
 active.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 channelTemplateMapper.updateById(active);
 });
 draft.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 draft.setPublishTime(LocalDateTime.now());
 return channelTemplateMapper.updateById(draft) > 0;
 }

 @Override
 public PageResult<NoticeChannelConfigVO> listChannelConfigs(NoticeChannelConfigPageQuery query) {
 LambdaQueryWrapper<NoticeChannelConfigEntity> wrapper = new LambdaQueryWrapper<>();
 if (query.getChannelType() != null) {
 wrapper.eq(NoticeChannelConfigEntity::getChannelType, query.getChannelType());
 }
 if (query.getEnabled() != null) {
 wrapper.eq(NoticeChannelConfigEntity::getEnabled, query.getEnabled());
 }
 wrapper.orderByDesc(NoticeChannelConfigEntity::getUpdatedAt);
 Page<NoticeChannelConfigEntity> result = channelConfigMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(NoticeChannelConfigConvert::toVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public NoticeChannelConfigVO saveChannelConfig(SaveNoticeChannelConfigCommand command) {
 NoticeChannelConfigEntity entity = command.getId() == null ? new NoticeChannelConfigEntity() : channelConfigMapper.selectById(command.getId());
 Require.notNull(entity, "渠道配置不存在");
 entity.setChannelType(command.getChannelType());
 entity.setProviderCode(command.getProviderCode());
 entity.setConfigName(command.getConfigName());
 String configJson = mergeMaskedConfigJson(entity.getConfigJson(), command.getConfigJson());
 entity.setConfigJson(configJson);
 entity.setEnabled(command.getEnabled());
 entity.setPriority(command.getPriority());
 entity.setWeight(command.getWeight() == null || command.getWeight() <= 0 ? 100 : command.getWeight());
 entity.setConfigStatus(resolveConfigStatus(command.getChannelType(), command.getProviderCode(), configJson));
 if (entity.getLastSendStatus() == null) {
 entity.setLastSendStatus(NoticeChannelSendHealthStatus.NONE);
 }
 entity.setRateLimitConfig(command.getRateLimitConfig());
 if (entity.getId() == null) {
 channelConfigMapper.insert(entity);
 } else {
 channelConfigMapper.updateById(entity);
 }
 return NoticeChannelConfigConvert.toVO(entity);
 }

 @Override
 public boolean deleteChannelConfig(Long id) {
 Require.notNull(id, "渠道配置ID不能为空");
 NoticeChannelConfigEntity entity = channelConfigMapper.selectById(id);
 Require.notNull(entity, "渠道配置不存在");
 Require.isFalse(isBuiltinSiteChannel(entity), "系统消息内置通道不允许删除");
 Long templateCount = channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelConfigId, id));
 Require.isTrue(templateCount == null || templateCount == 0, "渠道已被消息配置引用，不能删除");
 return channelConfigMapper.deleteById(id) > 0;
 }

 private boolean isBuiltinSiteChannel(NoticeChannelConfigEntity entity) {
 return entity.getChannelType() == NoticeChannelType.SITE && SITE_INTERNAL_PROVIDER.equals(entity.getProviderCode());
 }

 private String mergeMaskedConfigJson(String originalJson, String submittedJson) {
 Map<String, Object> submitted = new LinkedHashMap<>(fromJson(submittedJson));
 if (submitted.isEmpty()) {
 return submittedJson;
 }
 Map<String, Object> original = fromJson(originalJson);
 if (original.isEmpty()) {
 return submittedJson;
 }
 submitted.replaceAll((key, value) -> shouldKeepOriginalConfigValue(key, value) && original.containsKey(key)
 ? original.get(key) : value);
 return toJson(submitted);
 }

 private boolean shouldKeepOriginalConfigValue(String key, Object value) {
 return isSensitiveConfigKey(key) && (value == null || MASKED_VALUE.equals(String.valueOf(value)));
 }

 private boolean isSensitiveConfigKey(String key) {
 return SENSITIVE_CONFIG_KEYS.stream().anyMatch(secretKey -> secretKey.equalsIgnoreCase(key)
 || key.toLowerCase().contains(secretKey.toLowerCase()));
 }

 @Override
 public PageResult<NoticeTaskVO> listTasks(NoticeTaskPageQuery query) {
 LambdaQueryWrapper<NoticeTaskEntity> wrapper = new LambdaQueryWrapper<>();
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeTaskEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeTaskEntity::getBizId, query.getBizId());
 }
 if (query.getStatus() != null) {
 wrapper.eq(NoticeTaskEntity::getStatus, query.getStatus());
 }
 wrapper.orderByDesc(NoticeTaskEntity::getCreatedAt);
 Page<NoticeTaskEntity> result = taskMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(NoticeTaskConvert::toVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query) {
 LambdaQueryWrapper<NoticeSendRecordEntity> wrapper = new LambdaQueryWrapper<>();
 if (query.getTaskId() != null) {
 wrapper.eq(NoticeSendRecordEntity::getTaskId, query.getTaskId());
 }
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeSendRecordEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeSendRecordEntity::getBizId, query.getBizId());
 }
 if (query.getChannelType() != null) {
 wrapper.eq(NoticeSendRecordEntity::getChannelType, query.getChannelType());
 }
 if (query.getStatus() != null) {
 wrapper.eq(NoticeSendRecordEntity::getStatus, query.getStatus());
 }
 wrapper.orderByDesc(NoticeSendRecordEntity::getCreatedAt);
 Page<NoticeSendRecordEntity> result = sendRecordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(NoticeSendRecordConvert::toVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public NoticeSettingsVO getSettings() {
 NoticeSettingsVO settings = defaultSettings();
 settingMapper.selectList(new LambdaQueryWrapper<NoticeSettingEntity>())
 .forEach(setting -> applySetting(settings, setting.getSettingKey(), setting.getSettingValue()));
 return settings;
 }

 @Override
 public boolean saveSettings(SaveNoticeSettingsCommand command) {
 NoticeSettingsVO defaults = defaultSettings();
 upsertSetting("soundEnabled", String.valueOf(command.getSoundEnabled() == null
 ? defaults.getSoundEnabled() : command.getSoundEnabled()));
 upsertSetting("desktopEnabled", String.valueOf(command.getDesktopEnabled() == null
 ? defaults.getDesktopEnabled() : command.getDesktopEnabled()));
 upsertSetting("maxRetry", String.valueOf(command.getMaxRetry() == null
 ? defaults.getMaxRetry() : command.getMaxRetry()));
 upsertSetting("retentionDays", String.valueOf(command.getRetentionDays() == null
 ? defaults.getRetentionDays() : command.getRetentionDays()));
 return true;
 }

 @Override
 public PageResult<NoticeSiteMessageVO> listSiteMessages(Long userId, NoticeSiteMessagePageQuery query) {
 LambdaQueryWrapper<NoticeSiteMessageEntity> wrapper = userVisibleWrapper(userId);
 if (Boolean.TRUE.equals(query.getUnreadOnly())) {
 wrapper.eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD);
 }
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeSiteMessageEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeSiteMessageEntity::getBizId, query.getBizId());
 }
 wrapper.orderByDesc(NoticeSiteMessageEntity::getTopStatus).orderByDesc(NoticeSiteMessageEntity::getCreatedAt);
 Page<NoticeSiteMessageEntity> result = messageMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(NoticeSiteMessageConvert::toVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public NoticeSiteMessageVO getSiteMessage(Long id, Long userId) {
 NoticeSiteMessageEntity entity = messageMapper.selectOne(userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
 return entity == null ? null : NoticeSiteMessageConvert.toVO(entity);
 }

 @Override
 public NoticeUnreadCountVO unreadCount(Long userId) {
 Long count = messageMapper.selectCount(userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD));
 return new NoticeUnreadCountVO(count);
 }

 @Override
 public boolean markSiteMessageRead(Long id, Long userId) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setReadStatus(NoticeReadStatus.READ);
 entity.setReadTime(LocalDateTime.now());
 return messageMapper.update(entity, userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id)) > 0;
 }

 @Override
 public boolean markSiteMessagesRead(MarkNoticeReadCommand command, Long userId) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setReadStatus(NoticeReadStatus.READ);
 entity.setReadTime(LocalDateTime.now());
 return messageMapper.update(entity, userVisibleWrapper(userId).in(NoticeSiteMessageEntity::getId, command.getIds())) >= 0;
 }

 @Override
 public boolean markAllSiteMessagesRead(Long userId) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setReadStatus(NoticeReadStatus.READ);
 entity.setReadTime(LocalDateTime.now());
 return messageMapper.update(entity, userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getReadStatus, NoticeReadStatus.UNREAD)) >= 0;
 }

 @Override
 public boolean deleteSiteMessage(Long id, Long userId) {
 NoticeSiteMessageEntity entity = new NoticeSiteMessageEntity();
 entity.setDeleteStatus(NoticeDeleteStatus.DELETED);
 return messageMapper.update(entity, userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id)) > 0;
 }

 private NoticeBusinessTypeVO toBusinessTypeVO(NoticeBusinessTypeEntity entity) {
 NoticeBusinessTypeVO vo = NoticeBusinessTypeConvert.toVO(entity);
 NoticeBusinessConfigVersionEntity draft = latestBusinessConfigVersion(entity.getBizType(), NoticeTemplateVersionStatus.DRAFT);
 NoticeBusinessConfigVersionEntity active = latestBusinessConfigVersion(entity.getBizType(), NoticeTemplateVersionStatus.ACTIVE);
 List<NoticeBusinessChannelTemplateEntity> activeTemplates = channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, entity.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE)
 .eq(NoticeBusinessChannelTemplateEntity::getEnabled, true));
 boolean hasDraftTemplate = channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, entity.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.DRAFT)) > 0;
 vo.setActiveVersion(active == null ? null : active.getVersion());
 vo.setDraftVersion(draft == null ? null : draft.getVersion());
 vo.setLastPublishTime(active == null ? null : active.getPublishTime());
 vo.setEnabledChannels(activeTemplates.stream()
 .map(template -> template.getChannelType().name())
 .distinct()
 .collect(Collectors.joining(",")));
 boolean pending = draft != null || hasDraftTemplate;
 vo.setSyncStatus(pending ? NoticeSyncStatus.PENDING_PUBLISH : NoticeSyncStatus.SYNCED);
 vo.setSyncReason(pending ? "存在未发布草稿，修改发布后才生效" : "当前配置已发布生效");
 return vo;
 }

 private SaveNoticeBusinessConfigCommand draftCommand(NoticeBusinessTypeEntity entity) {
 SaveNoticeBusinessConfigCommand command = new SaveNoticeBusinessConfigCommand();
 command.setParamsSchema(entity.getParamsSchema());
 command.setDefaultPriority(entity.getDefaultPriority() == null ? NoticePriority.NORMAL : entity.getDefaultPriority());
 command.setIdempotentStrategy(entity.getIdempotentStrategy());
 return command;
 }

 private NoticeChannelConfigStatus resolveConfigStatus(NoticeChannelType channelType, String providerCode, String configJson) {
 if (channelType == NoticeChannelType.SITE) {
 return NoticeChannelConfigStatus.COMPLETE;
 }
 Map<String, Object> config = fromJson(configJson);
 if (config.isEmpty()) {
 return NoticeChannelConfigStatus.INCOMPLETE;
 }
 return switch (channelType) {
 case SMS -> hasAnyText(config, "accessKeyId", "accessKey", "secretId")
 && hasAnyText(config, "accessKeySecret", "accessSecret", "secretKey")
 && hasAnyText(config, "signName", "sign") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 case EMAIL -> resolveEmailConfigStatus(providerCode, config);
 case WECHAT_OFFICIAL -> hasAnyText(config, "appId") && hasAnyText(config, "appSecret", "secret")
 ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 case WECOM -> hasAnyText(config, "corpId")
 && hasAnyText(config, "agentId", "webhookUrl")
 && hasAnyText(config, "secret", "corpSecret", "webhookUrl") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 case DINGTALK -> hasAnyText(config, "appKey", "webhookUrl")
 && hasAnyText(config, "appSecret", "webhookUrl") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 case SITE -> NoticeChannelConfigStatus.COMPLETE;
 };
 }

 private NoticeChannelConfigStatus resolveEmailConfigStatus(String providerCode, Map<String, Object> config) {
 if ("ALIYUN_DM".equals(providerCode)) {
 return hasAnyText(config, "accessKeyId", "accessKey")
 && hasAnyText(config, "accessKeySecret", "accessSecret")
 && hasAnyText(config, "regionId", "region")
 && hasAnyText(config, "endpoint")
 && hasAnyText(config, "accountName", "fromAddress")
 ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 }
 return hasAnyText(config, "host", "smtpHost")
 && hasAnyText(config, "username", "account")
 && hasAnyText(config, "password", "smtpPassword")
 && hasAnyText(config, "from", "fromAddress") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 }

 private boolean hasAnyText(Map<String, Object> config, String... keys) {
 for (String key : keys) {
 Object value = config.get(key);
 if (value != null && StringUtils.hasText(String.valueOf(value))) {
 return true;
 }
 }
 return false;
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

 private NoticeBusinessTypeEntity requireBusinessType(Long businessTypeId) {
 Require.notNull(businessTypeId, "业务类型 ID 不能为空");
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectById(businessTypeId);
 Require.notNull(businessType, "业务类型不存在");
 return businessType;
 }

 private NoticeBusinessConfigVersionEntity latestBusinessConfigVersion(String bizType,
 NoticeTemplateVersionStatus status) {
 return businessConfigVersionMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, bizType)
 .eq(NoticeBusinessConfigVersionEntity::getVersionStatus, status)
 .orderByDesc(NoticeBusinessConfigVersionEntity::getVersion)
 .last("limit 1"));
 }

 private Integer nextBusinessConfigVersion(String bizType) {
 List<NoticeBusinessConfigVersionEntity> versions = businessConfigVersionMapper.selectList(
 new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, bizType));
 return versions.stream().map(NoticeBusinessConfigVersionEntity::getVersion).max(Integer::compareTo).orElse(0) + 1;
 }

 private void activateChannelTemplatesFromVersion(NoticeBusinessTypeEntity businessType, Integer version) {
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.ACTIVE))
 .forEach(active -> {
 active.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 channelTemplateMapper.updateById(active);
 });
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, NoticeTemplateVersionStatus.DRAFT))
 .forEach(draft -> {
 draft.setVersionStatus(NoticeTemplateVersionStatus.HISTORY);
 channelTemplateMapper.updateById(draft);
 });
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessChannelTemplateEntity::getVersion, version))
 .forEach(source -> {
 NoticeBusinessChannelTemplateEntity activated = new NoticeBusinessChannelTemplateEntity();
 activated.setBusinessTypeId(businessType.getId());
 activated.setBizType(businessType.getBizType());
 activated.setChannelType(source.getChannelType());
 activated.setTemplateName(source.getTemplateName());
 activated.setTitleTemplate(source.getTitleTemplate());
 activated.setContentTemplate(source.getContentTemplate());
 activated.setChannelTemplateId(source.getChannelTemplateId());
 activated.setVariableMapping(source.getVariableMapping());
 activated.setVersion(nextTemplateVersion(businessType.getBizType(), source.getChannelType()));
 activated.setVersionStatus(NoticeTemplateVersionStatus.ACTIVE);
 activated.setEnabled(source.getEnabled());
 activated.setChannelConfigId(source.getChannelConfigId());
 activated.setPublishTime(LocalDateTime.now());
 channelTemplateMapper.insert(activated);
 });
 }

 private NoticeBusinessChannelTemplateEntity latestChannelTemplate(String bizType, NoticeChannelType channelType,
 NoticeTemplateVersionStatus status) {
 return channelTemplateMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, channelType)
 .eq(NoticeBusinessChannelTemplateEntity::getVersionStatus, status)
 .orderByDesc(NoticeBusinessChannelTemplateEntity::getVersion)
 .last("limit 1"));
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
 if (!templates.isEmpty()) {
 return templates;
 }
 if (StringUtils.hasText(command.getTitle()) || StringUtils.hasText(command.getContent())) {
 return directTemplates(command, businessType, requested);
 }
 throw new IllegalStateException("业务类型未配置启用渠道模板");
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

 private List<NoticeRecipientCommand> resolveRecipients(SendNoticeCommand command) {
 List<NoticeRecipientCommand> recipients = new ArrayList<>();
 if (command.getRecipients() != null) {
 recipients.addAll(command.getRecipients());
 }
 receiverIds(command).forEach(userId -> {
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(userId);
 recipients.add(recipient);
 });
 return recipients;
 }

 private NoticeTaskEntity createTask(SendNoticeCommand command, List<NoticeBusinessChannelTemplateEntity> templates,
 List<NoticeRecipientCommand> recipients) {
 NoticeTaskEntity task = new NoticeTaskEntity();
 task.setTaskCode("NT" + UUID.randomUUID().toString().replace("-", ""));
 task.setBizType(command.getBizType());
 task.setBizId(command.getBizId());
 task.setIdempotentKey(command.getIdempotentKey());
 task.setParamsSnapshot(toJson(taskParams(command)));
 task.setChannelTypes(templates.stream().map(template -> template.getChannelType().name()).distinct().collect(Collectors.joining(",")));
 task.setSendMode(command.getSendMode() == null ? NoticeSendMode.IMMEDIATE : command.getSendMode());
 task.setScheduledTime(command.getScheduledTime());
 task.setStatus(task.getSendMode() == NoticeSendMode.SCHEDULED ? NoticeTaskStatus.WAITING : NoticeTaskStatus.SENDING);
 task.setTotalCount(recipients.size() * templates.size());
 task.setSuccessCount(0);
 task.setFailCount(0);
 taskMapper.insert(task);
 return task;
 }

 private Instant nextAttemptAt(NoticeTaskEntity task) {
 if (task.getSendMode() == NoticeSendMode.SCHEDULED && task.getScheduledTime() != null) {
 return task.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant();
 }
 return Instant.now();
 }

 private Map<String, Object> taskParams(SendNoticeCommand command) {
 Map<String, Object> params = command.getParams() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(command.getParams());
 if (command.getAttachmentFileIds() != null && !command.getAttachmentFileIds().isEmpty()) {
 params.put("attachments", command.getAttachmentFileIds());
 }
 return params;
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

 private NoticeSendRecordEntity createSendRecord(NoticeTaskEntity task, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, SendNoticeCommand command) {
 NoticeSendRecordEntity record = new NoticeSendRecordEntity();
 record.setTaskId(task.getId());
 record.setRecipientId(recipient.getId());
 record.setBizType(task.getBizType());
 record.setBizId(task.getBizId());
 record.setBusinessChannelTemplateId(template.getId());
 record.setTemplateVersion(template.getVersion());
 record.setChannelType(template.getChannelType());
 record.setRequestId("NR" + UUID.randomUUID().toString().replace("-", ""));
 record.setStatus(NoticeSendStatus.PENDING);
 record.setRenderedTitle(render(template.getTitleTemplate(), command.getParams()));
 record.setRenderedContent(render(template.getContentTemplate(), command.getParams()));
 record.setRequestSnapshot(toJson(sendRecordRequestSnapshot(task, recipient, template, command)));
 record.setRetryCount(0);
 sendRecordMapper.insert(record);
 return record;
 }

 private Map<String, Object> sendRecordRequestSnapshot(NoticeTaskEntity task, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, SendNoticeCommand command) {
 Map<String, Object> snapshot = new LinkedHashMap<>();
 snapshot.put("bizType", task.getBizType());
 snapshot.put("bizId", task.getBizId());
 snapshot.put("taskId", task.getId());
 snapshot.put("recipientId", recipient.getId());
 snapshot.put("userId", recipient.getUserId());
 snapshot.put("channelType", template.getChannelType().name());
 snapshot.put("businessChannelTemplateId", template.getId());
 snapshot.put("templateVersion", template.getVersion());
 snapshot.put("params", command.getParams() == null ? Collections.emptyMap() : command.getParams());
 return snapshot;
 }

 private ChannelSendResult sendRecord(NoticeSendRecordEntity record, NoticeRecipientEntity recipient,
 NoticeBusinessChannelTemplateEntity template, NoticeTaskEntity task) {
 record.setStatus(NoticeSendStatus.SENDING);
 sendRecordMapper.updateById(record);
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

 private ChannelSendResult sendWithRoute(NoticeChannelSender sender, NoticeTaskEntity task, NoticeSendRecordEntity record,
 NoticeRecipientEntity recipient, NoticeBusinessChannelTemplateEntity template) {
 List<NoticeChannelConfigEntity> configs = routeChannelConfigs(template, record.getId());
 if (configs.isEmpty()) {
 return ChannelSendResult.failed(NoticeFailureCode.CHANNEL_UNAVAILABLE.name(), "没有可用通知通道", true);
 }
 ChannelSendResult lastResult = null;
 for (NoticeChannelConfigEntity config : configs) {
 for (int attempt = 1; attempt <= MAX_CHANNEL_ATTEMPTS; attempt++) {
 ChannelSendCommand command = toChannelCommand(task, record, recipient, template, config);
 ChannelSendResult result = sender.send(command);
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

 private ChannelSendCommand toChannelCommand(NoticeTaskEntity task, NoticeSendRecordEntity record,
 NoticeRecipientEntity recipient, NoticeBusinessChannelTemplateEntity template, NoticeChannelConfigEntity config) {
 ChannelSendCommand sendCommand = new ChannelSendCommand();
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
 sendCommand.setAttachmentFileIds(attachmentFileIds(task.getParamsSnapshot()));
 sendCommand.setPriority(NoticePriority.NORMAL);
 sendCommand.setBizType(task.getBizType());
 sendCommand.setBizId(task.getBizId());
 sendCommand.setParams(fromJson(task.getParamsSnapshot()));
 sendCommand.setChannelConfigId(config.getId());
 sendCommand.setChannelProviderCode(config.getProviderCode());
 sendCommand.setChannelConfigName(config.getConfigName());
 sendCommand.setChannelConfigJson(config.getConfigJson());
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
 if (template.getChannelConfigId() != null) {
 NoticeChannelConfigEntity config = channelConfigMapper.selectById(template.getChannelConfigId());
 if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
 return Collections.emptyList();
 }
 if (config.getConfigStatus() == NoticeChannelConfigStatus.INCOMPLETE) {
 return Collections.emptyList();
 }
 return List.of(config);
 }
 List<NoticeChannelConfigEntity> configs = channelConfigMapper.selectList(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getChannelType, template.getChannelType())
 .eq(NoticeChannelConfigEntity::getEnabled, true)
 .eq(NoticeChannelConfigEntity::getConfigStatus, NoticeChannelConfigStatus.COMPLETE));
 if (configs.isEmpty()) {
 return Collections.emptyList();
 }
 return weightedRotation(configs, seed == null ? 0L : seed);
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

 private void upsertSetting(String key, String value) {
 NoticeSettingEntity existing = settingMapper.selectOne(new LambdaQueryWrapper<NoticeSettingEntity>()
 .eq(NoticeSettingEntity::getSettingKey, key)
 .last("limit 1"));
 NoticeSettingEntity entity = existing == null ? new NoticeSettingEntity() : existing;
 entity.setSettingKey(key);
 entity.setSettingValue(value);
 if (entity.getId() == null) {
 settingMapper.insert(entity);
 } else {
 settingMapper.updateById(entity);
 }
 }

 private NoticeSettingsVO defaultSettings() {
 NoticeSettingsVO settings = new NoticeSettingsVO();
 settings.setSoundEnabled(true);
 settings.setDesktopEnabled(true);
 settings.setMaxRetry(3);
 settings.setRetentionDays(180);
 return settings;
 }

 private void applySetting(NoticeSettingsVO settings, String key, String value) {
 if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
 return;
 }
 switch (key) {
 case "soundEnabled" -> settings.setSoundEnabled(Boolean.parseBoolean(value));
 case "desktopEnabled" -> settings.setDesktopEnabled(Boolean.parseBoolean(value));
 case "maxRetry" -> settings.setMaxRetry(parseInteger(value, settings.getMaxRetry()));
 case "retentionDays" -> settings.setRetentionDays(parseInteger(value, settings.getRetentionDays()));
 default -> {
 }
 }
 }

 private Integer parseInteger(String value, Integer defaultValue) {
 try {
 return Integer.valueOf(value);
 } catch (NumberFormatException ex) {
 return defaultValue;
 }
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
 Object value = params.get(matcher.group(1));
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

 private Integer nextTemplateVersion(String bizType, NoticeChannelType channelType) {
 List<NoticeBusinessChannelTemplateEntity> templates = channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, channelType));
 return templates.stream().map(NoticeBusinessChannelTemplateEntity::getVersion).max(Integer::compareTo).orElse(0) + 1;
 }

 private LambdaQueryWrapper<NoticeSiteMessageEntity> userVisibleWrapper(Long userId) {
 return new LambdaQueryWrapper<NoticeSiteMessageEntity>()
 .eq(NoticeSiteMessageEntity::getUserId, userId)
 .eq(NoticeSiteMessageEntity::getDeleteStatus, NoticeDeleteStatus.NORMAL);
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
