package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.enums.IdentityUserTargetType;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordCommand;
import io.mango.notice.api.command.HandleNoticeSendRecordsCommand;
import io.mango.notice.api.command.MarkNoticeReadCommand;
import io.mango.notice.api.command.NoticeRecipientCommand;
import io.mango.notice.api.command.NoticeRecipientTargetCommand;
import io.mango.notice.api.command.RetryNoticeSendRecordsCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.SaveNoticeReceivePreferenceCommand;
import io.mango.notice.api.command.SaveNoticeRecipientAccountCommand;
import io.mango.notice.api.command.SaveNoticeSettingsCommand;
import io.mango.notice.api.command.SendNoticeCommand;
import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeDeleteStatus;
import io.mango.notice.api.enums.NoticeFailureCode;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeReadStatus;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.mango.notice.api.enums.NoticeSendCancelCode;
import io.mango.notice.api.enums.NoticeSendMode;
import io.mango.notice.api.enums.NoticeSendStatus;
import io.mango.notice.api.enums.NoticeSyncStatus;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeReceivePreferenceQuery;
import io.mango.notice.api.query.NoticeRecipientAccountQuery;
import io.mango.notice.api.query.NoticeSendRecordPageQuery;
import io.mango.notice.api.query.NoticeSiteMessagePageQuery;
import io.mango.notice.api.query.NoticeTaskPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeReceivePreferenceVO;
import io.mango.notice.api.vo.NoticeRecipientAccountVO;
import io.mango.notice.api.vo.NoticeSendRecordVO;
import io.mango.notice.api.vo.NoticeSendResultVO;
import io.mango.notice.api.vo.NoticeSettingsVO;
import io.mango.notice.api.vo.NoticeSiteMessageVO;
import io.mango.notice.api.vo.NoticeTaskVO;
import io.mango.notice.api.vo.NoticeUnreadCountVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.api.vo.WecomUserSyncResultVO;
import io.mango.notice.channel.wecom.WecomApiException;
import io.mango.notice.channel.wecom.WecomChannelConfig;
import io.mango.notice.channel.wecom.WecomDepartment;
import io.mango.notice.channel.wecom.WecomDirectoryClient;
import io.mango.notice.channel.wecom.WecomDirectoryUser;
import io.mango.notice.core.convert.NoticeBusinessConfigVersionConvert;
import io.mango.notice.core.convert.NoticeBusinessTypeConvert;
import io.mango.notice.core.convert.NoticeChannelConfigConvert;
import io.mango.notice.core.convert.NoticeChannelTemplateConvert;
import io.mango.notice.core.convert.NoticeReceivePreferenceConvert;
import io.mango.notice.core.convert.NoticeRecipientAccountConvert;
import io.mango.notice.core.convert.NoticeSendRecordConvert;
import io.mango.notice.core.convert.NoticeSiteMessageConvert;
import io.mango.notice.core.convert.NoticeTaskConvert;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeReceivePreferenceEntity;
import io.mango.notice.core.entity.NoticeRecipientEntity;
import io.mango.notice.core.entity.NoticeRecipientAccountEntity;
import io.mango.notice.core.entity.NoticeSendRecordEntity;
import io.mango.notice.core.entity.NoticeSettingEntity;
import io.mango.notice.core.entity.NoticeSiteMessageEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.entity.NoticeWecomSyncMappingEntity;
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
import io.mango.notice.core.outbox.NoticeOutboxMessageMapper;
import io.mango.notice.core.service.INoticeService;
import io.mango.notice.support.channel.ChannelSendCommand;
import io.mango.notice.support.channel.ChannelSendResult;
import io.mango.notice.support.channel.NoticeChannelSender;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NoticeService implements INoticeService {

 private static final Pattern TEMPLATE_VARIABLE = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*}}|\\$\\{\\s*([^}]+?)\\s*}");
 private static final int MAX_CHANNEL_ATTEMPTS = 3;
 private static final List<NoticeSendStatus> RETRY_OPERABLE_STATUSES = List.of(NoticeSendStatus.FAILED,
 NoticeSendStatus.RETRY_WAITING, NoticeSendStatus.FINAL_FAILED);
 private static final String MASKED_VALUE = "***";
 private static final String SITE_INTERNAL_PROVIDER = "INTERNAL";
 private static final String WECOM_SYNC_TYPE_DEPARTMENT = "DEPARTMENT";
 private static final String WECOM_SYNC_TYPE_USER = "USER";
 private static final long WECOM_ROOT_DEPARTMENT_ID = 1L;
 private static final Integer ORG_TYPE_GROUP = 1;
 private static final Integer ORG_TYPE_COMPANY = 2;
 private static final Integer ORG_TYPE_DEPARTMENT = 3;
 private static final String INTERNAL_ORG_PARTY_TYPE = "INTERNAL_ORG";
 private static final Set<String> SENSITIVE_CONFIG_KEYS = Set.of("secret", "password", "token", "key", "appSecret",
 "accessKey", "accessKeySecret", "secretKey", "smtpPassword");
 private final NoticeSiteMessageMapper messageMapper;
 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessConfigVersionMapper businessConfigVersionMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeTaskMapper taskMapper;
 private final NoticeRecipientMapper recipientMapper;
 private final NoticeRecipientAccountMapper recipientAccountMapper;
 private final NoticeReceivePreferenceMapper receivePreferenceMapper;
 private final NoticeSendRecordMapper sendRecordMapper;
 private final NoticeSettingMapper settingMapper;
 private final NoticeWecomSyncMappingMapper wecomSyncMappingMapper;
 private final List<NoticeChannelSender> channelSenders;
 private final ObjectMapper objectMapper;
 private final IOutboxStore outboxStore;
 private final IdentityUserApi identityUserApi;
 private final SysOrgApi sysOrgApi;
 private final WecomDirectoryClient wecomDirectoryClient;

 @Override
 @Transactional(rollbackFor = Exception.class)
 public NoticeSendResultVO send(SendNoticeCommand command) {
 NoticeBusinessTypeEntity businessType = findBusinessType(command.getBizType());
 List<NoticeBusinessChannelTemplateEntity> templates = resolveTemplates(command, businessType);
 List<NoticeRecipientCommand> recipients = resolveRecipients(command);
 Require.isTrue(!recipients.isEmpty(), "接收用户不能为空");
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
 Require.isTrue(totalCount > 0, "没有可发送的通知记录");
 updateTaskTotalCount(task, totalCount, actualChannels);
 outboxStore.enqueue(NoticeOutboxMessageMapper.toOutboxMessage(task.getId(), nextAttemptAt(task)));
 return new NoticeSendResultVO(0, 0);
 }

 @Override
 public String findTaskTenantId(Long taskId) {
 Require.notNull(taskId, "通知任务 ID 不能为空");
 return taskMapper.selectTenantIdById(taskId);
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
 if (StringUtils.hasText(query.getDomainCode())) {
 wrapper.eq(NoticeBusinessTypeEntity::getDomainCode, query.getDomainCode());
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
 entity.setDomainCode(resolveDomainCode(command.getDomainCode(), command.getBizGroup()));
 entity.setBizGroup(resolveBizGroup(command.getBizGroup(), entity.getDomainCode()));
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
 entity.setDomainCode(resolveDomainCode(command.getDomainCode(), command.getBizGroup()));
 entity.setBizGroup(resolveBizGroup(command.getBizGroup(), entity.getDomainCode()));
 entity.setDescription(command.getDescription());
 businessTypeMapper.updateById(entity);
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
 public NoticeWecomLoginConfigVO getWecomLoginConfig(Long channelConfigId) {
 NoticeChannelConfigEntity entity;
 if (channelConfigId != null) {
 entity = channelConfigMapper.selectById(channelConfigId);
 Require.notNull(entity, "企业微信渠道配置不存在");
 Require.isTrue(entity.getChannelType() == NoticeChannelType.WECOM, "所选渠道不是企业微信渠道");
 Require.isTrue(Boolean.TRUE.equals(entity.getEnabled()), "企业微信渠道未启用");
 } else {
 entity = channelConfigMapper.selectOne(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getChannelType, NoticeChannelType.WECOM)
 .eq(NoticeChannelConfigEntity::getEnabled, true)
 .orderByDesc(NoticeChannelConfigEntity::getWeight)
 .orderByDesc(NoticeChannelConfigEntity::getUpdatedAt)
 .last("limit 1"));
 Require.notNull(entity, "未找到已启用的企业微信渠道配置");
 }
 Map<String, Object> config = fromJson(entity.getConfigJson());
 Require.isTrue(booleanValue(config.get("loginEnabled")), "企业微信扫码登录未启用");
 NoticeWecomLoginConfigVO vo = new NoticeWecomLoginConfigVO();
 vo.setChannelConfigId(entity.getId());
 vo.setConfigName(entity.getConfigName());
 vo.setCorpId(firstText(stringValue(config.get("corpId")), stringValue(config.get("corpID"))));
 vo.setAgentId(stringValue(config.get("agentId")));
 vo.setSecret(firstText(stringValue(config.get("secret")), stringValue(config.get("corpSecret"))));
 vo.setRedirectUri(firstText(stringValue(config.get("loginRedirectUri")), stringValue(config.get("redirectUri"))));
 Require.notBlank(vo.getCorpId(), "企业微信 CorpId 未配置");
 Require.notBlank(vo.getAgentId(), "企业微信 AgentId 未配置");
 Require.notBlank(vo.getSecret(), "企业微信通讯录 Secret 未配置");
 Require.notBlank(vo.getRedirectUri(), "企业微信扫码登录回调地址未配置");
 return vo;
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
 return PageResult.of(result.getRecords().stream().map(this::toTaskVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public PageResult<NoticeSendRecordVO> listSendRecords(NoticeSendRecordPageQuery query) {
 LambdaQueryWrapper<NoticeSendRecordEntity> wrapper = new LambdaQueryWrapper<>();
 Set<String> bizTypes = sendRecordBizTypes(query);
 if (sendRecordHasBizTypeFilter(query) && bizTypes.isEmpty()) {
 return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
 }
 if (!bizTypes.isEmpty()) {
 wrapper.in(NoticeSendRecordEntity::getBizType, bizTypes);
 }
 Set<Long> recipientIds = sendRecordRecipientIds(query);
 if (StringUtils.hasText(query.getRecipientKeyword()) && recipientIds.isEmpty()) {
 return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
 }
 if (!recipientIds.isEmpty()) {
 wrapper.in(NoticeSendRecordEntity::getRecipientId, recipientIds);
 }
 if (query.getTaskId() != null) {
 wrapper.eq(NoticeSendRecordEntity::getTaskId, query.getTaskId());
 }
 if (StringUtils.hasText(query.getBizType()) && bizTypes.isEmpty()) {
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
 if (query.getStartTime() != null) {
 wrapper.ge(NoticeSendRecordEntity::getSentAt, query.getStartTime());
 }
 if (query.getEndTime() != null) {
 wrapper.le(NoticeSendRecordEntity::getSentAt, query.getEndTime());
 }
 wrapper.orderByDesc(NoticeSendRecordEntity::getCreatedAt);
 Page<NoticeSendRecordEntity> result = sendRecordMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(toSendRecordVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean retrySendRecord(Long id) {
 Require.notNull(id, "发送记录 ID 不能为空");
 NoticeSendRecordEntity record = sendRecordMapper.selectById(id);
 Require.notNull(record, "发送记录不存在");
 Require.isTrue(RETRY_OPERABLE_STATUSES.contains(record.getStatus()), "当前状态不允许重试");
 record.setStatus(NoticeSendStatus.RETRY_WAITING);
 record.setNextRetryTime(LocalDateTime.now());
 sendRecordMapper.updateById(record);
 executeTask(record.getTaskId());
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean retrySendRecords(RetryNoticeSendRecordsCommand command) {
 Require.notNull(command, "批量重试参数不能为空");
 Require.isTrue(command.getIds() != null && !command.getIds().isEmpty(), "发送记录 ID 不能为空");
 for (Long id : command.getIds()) {
 retrySendRecord(id);
 }
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean markSendRecordManualSuccess(Long id, HandleNoticeSendRecordCommand command) {
 return handleFailedSendRecord(id, command, NoticeSendStatus.MANUAL_SUCCESS, "人工确认成功");
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean markSendRecordsManualSuccess(HandleNoticeSendRecordsCommand command) {
 handleFailedSendRecords(command, NoticeSendStatus.MANUAL_SUCCESS, "人工确认成功");
 return true;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean ignoreSendRecord(Long id, HandleNoticeSendRecordCommand command) {
 return handleFailedSendRecord(id, command, NoticeSendStatus.IGNORED, "忽略失败");
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean ignoreSendRecords(HandleNoticeSendRecordsCommand command) {
 handleFailedSendRecords(command, NoticeSendStatus.IGNORED, "忽略失败");
 return true;
 }

 private void handleFailedSendRecords(HandleNoticeSendRecordsCommand command, NoticeSendStatus status,
 String operationName) {
 Require.notNull(command, "批量处理参数不能为空");
 Require.isTrue(command.getIds() != null && !command.getIds().isEmpty(), "发送记录 ID 不能为空");
 Require.notBlank(command.getReason(), "处理原因不能为空");
 Set<Long> taskIds = new LinkedHashSet<>();
 for (Long id : command.getIds()) {
 NoticeSendRecordEntity record = handleFailedSendRecord(id, command.getReason(), status, operationName, false);
 taskIds.add(record.getTaskId());
 }
 for (Long taskId : taskIds) {
 refreshTaskStatus(taskId);
 }
 }

 private boolean handleFailedSendRecord(Long id, HandleNoticeSendRecordCommand command, NoticeSendStatus status,
 String operationName) {
 Require.notNull(id, "发送记录 ID 不能为空");
 Require.notNull(command, "处理参数不能为空");
 Require.notBlank(command.getReason(), "处理原因不能为空");
 handleFailedSendRecord(id, command.getReason(), status, operationName, true);
 return true;
 }

 private NoticeSendRecordEntity handleFailedSendRecord(Long id, String reason, NoticeSendStatus status,
 String operationName, boolean refreshTask) {
 NoticeSendRecordEntity record = sendRecordMapper.selectById(id);
 Require.notNull(record, "发送记录不存在");
 Require.isTrue(RETRY_OPERABLE_STATUSES.contains(record.getStatus()), "当前状态不允许处理");
 record.setStatus(status);
 record.setFailReason(operationName + "：" + reason);
 record.setNextRetryTime(null);
 record.setSentAt(LocalDateTime.now());
 sendRecordMapper.updateById(record);
 if (refreshTask) {
 refreshTaskStatus(record.getTaskId());
 }
 return record;
 }

 private void refreshTaskStatus(Long taskId) {
 if (taskId == null) {
 return;
 }
 NoticeTaskEntity task = taskMapper.selectById(taskId);
 if (task == null || task.getStatus() == NoticeTaskStatus.CANCELED) {
 return;
 }
 List<NoticeSendRecordEntity> records = sendRecordMapper.selectList(new LambdaQueryWrapper<NoticeSendRecordEntity>()
 .eq(NoticeSendRecordEntity::getTaskId, taskId));
 int successCount = 0;
 int failCount = 0;
 for (NoticeSendRecordEntity record : records) {
 if (record.getStatus() == NoticeSendStatus.SUCCESS || record.getStatus() == NoticeSendStatus.MANUAL_SUCCESS
 || record.getStatus() == NoticeSendStatus.IGNORED || record.getStatus() == NoticeSendStatus.CANCELED) {
 successCount++;
 } else if (record.getStatus() == NoticeSendStatus.FAILED || record.getStatus() == NoticeSendStatus.RETRY_WAITING
 || record.getStatus() == NoticeSendStatus.FINAL_FAILED) {
 failCount++;
 }
 }
 task.setSuccessCount(successCount);
 task.setFailCount(failCount);
 task.setStatus(resolveTaskStatus(successCount, failCount));
 taskMapper.updateById(task);
 }

 private boolean sendRecordHasBizTypeFilter(NoticeSendRecordPageQuery query) {
 return StringUtils.hasText(query.getBizGroup()) || StringUtils.hasText(query.getMessageName());
 }

 private Set<String> sendRecordBizTypes(NoticeSendRecordPageQuery query) {
 if (!sendRecordHasBizTypeFilter(query)) {
 return Collections.emptySet();
 }
 LambdaQueryWrapper<NoticeBusinessTypeEntity> wrapper = new LambdaQueryWrapper<>();
 if (StringUtils.hasText(query.getBizType())) {
 wrapper.eq(NoticeBusinessTypeEntity::getBizType, query.getBizType());
 }
 if (StringUtils.hasText(query.getBizGroup())) {
 wrapper.like(NoticeBusinessTypeEntity::getBizGroup, query.getBizGroup());
 }
 if (StringUtils.hasText(query.getMessageName())) {
 wrapper.like(NoticeBusinessTypeEntity::getBizName, query.getMessageName());
 }
 return businessTypeMapper.selectList(wrapper).stream()
 .map(NoticeBusinessTypeEntity::getBizType)
 .filter(StringUtils::hasText)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 }

 private Set<Long> sendRecordRecipientIds(NoticeSendRecordPageQuery query) {
 if (!StringUtils.hasText(query.getRecipientKeyword())) {
 return Collections.emptySet();
 }
 String keyword = query.getRecipientKeyword();
 LambdaQueryWrapper<NoticeRecipientEntity> wrapper = new LambdaQueryWrapper<NoticeRecipientEntity>()
 .like(NoticeRecipientEntity::getRecipientName, keyword)
 .or()
 .like(NoticeRecipientEntity::getMobile, keyword)
 .or()
 .like(NoticeRecipientEntity::getEmail, keyword)
 .or()
 .like(NoticeRecipientEntity::getWechatOpenid, keyword)
 .or()
 .like(NoticeRecipientEntity::getWecomUserId, keyword)
 .or()
 .like(NoticeRecipientEntity::getDingtalkUserId, keyword);
 return recipientMapper.selectList(wrapper).stream()
 .map(NoticeRecipientEntity::getId)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 }

 private List<NoticeSendRecordVO> toSendRecordVOs(List<NoticeSendRecordEntity> records) {
 if (records.isEmpty()) {
 return Collections.emptyList();
 }
 Set<String> bizTypes = records.stream()
 .map(NoticeSendRecordEntity::getBizType)
 .filter(StringUtils::hasText)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<String, NoticeBusinessTypeEntity> businessTypeMap = bizTypes.isEmpty()
 ? Collections.emptyMap()
 : businessTypeMapper.selectList(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .in(NoticeBusinessTypeEntity::getBizType, bizTypes)).stream()
 .collect(Collectors.toMap(NoticeBusinessTypeEntity::getBizType, Function.identity(), (left, right) -> left));
 Set<Long> recipientIds = records.stream()
 .map(NoticeSendRecordEntity::getRecipientId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeRecipientEntity> recipientMap = recipientIds.isEmpty()
 ? Collections.emptyMap()
 : recipientMapper.selectList(new LambdaQueryWrapper<NoticeRecipientEntity>()
 .in(NoticeRecipientEntity::getId, recipientIds)).stream()
 .collect(Collectors.toMap(NoticeRecipientEntity::getId, Function.identity(), (left, right) -> left));
 Set<Long> templateIds = records.stream()
 .map(NoticeSendRecordEntity::getBusinessChannelTemplateId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeBusinessChannelTemplateEntity> templateMap = templateIds.isEmpty()
 ? Collections.emptyMap()
 : channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .in(NoticeBusinessChannelTemplateEntity::getId, templateIds)).stream()
 .collect(Collectors.toMap(NoticeBusinessChannelTemplateEntity::getId, Function.identity(), (left, right) -> left));
 Set<Long> channelConfigIds = records.stream()
 .map(NoticeSendRecordEntity::getChannelConfigId)
 .filter(id -> id != null)
 .collect(Collectors.toCollection(LinkedHashSet::new));
 Map<Long, NoticeChannelConfigEntity> channelConfigMap = channelConfigIds.isEmpty()
 ? Collections.emptyMap()
 : channelConfigMapper.selectList(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .in(NoticeChannelConfigEntity::getId, channelConfigIds)).stream()
 .collect(Collectors.toMap(NoticeChannelConfigEntity::getId, Function.identity(), (left, right) -> left));
 return records.stream()
 .map(record -> NoticeSendRecordConvert.toVO(record, businessTypeMap.get(record.getBizType()),
 recipientMap.get(record.getRecipientId()), templateMap.get(record.getBusinessChannelTemplateId()),
 channelConfigMap.get(record.getChannelConfigId())))
 .toList();
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
 public List<NoticeRecipientAccountVO> listRecipientAccounts(Long currentUserId, NoticeRecipientAccountQuery query) {
 Long userId = resolveTargetUserId(currentUserId, query == null ? null : query.getUserId());
 LambdaQueryWrapper<NoticeRecipientAccountEntity> wrapper = new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getUserId, userId)
 .eq(NoticeRecipientAccountEntity::getEnabled, true);
 if (query != null && query.getAccountType() != null) {
 wrapper.eq(NoticeRecipientAccountEntity::getAccountType, query.getAccountType());
 }
 wrapper.orderByDesc(NoticeRecipientAccountEntity::getDefaultAccount)
 .orderByDesc(NoticeRecipientAccountEntity::getUpdatedAt);
 return recipientAccountMapper.selectList(wrapper).stream()
 .map(NoticeRecipientAccountConvert::toVO)
 .toList();
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public NoticeRecipientAccountVO saveRecipientAccount(Long currentUserId, SaveNoticeRecipientAccountCommand command) {
 Require.notNull(command, "接收账户不能为空");
 Long userId = resolveTargetUserId(currentUserId, command.getUserId());
 NoticeRecipientAccountEntity entity = command.getId() == null
 ? new NoticeRecipientAccountEntity()
 : recipientAccountMapper.selectById(command.getId());
 Require.notNull(entity, "接收账户不存在");
 entity.setUserId(userId);
 entity.setAccountType(command.getAccountType());
 entity.setAccountValue(command.getAccountValue());
 entity.setDisplayName(command.getDisplayName());
 entity.setVerifiedStatus(command.getVerifiedStatus() == null
 ? NoticeRecipientAccountStatus.VERIFIED : command.getVerifiedStatus());
 entity.setDefaultAccount(Boolean.TRUE.equals(command.getDefaultAccount()));
 entity.setEnabled(true);
 if (Boolean.TRUE.equals(entity.getDefaultAccount())) {
 clearDefaultAccount(userId, entity.getAccountType());
 }
 if (entity.getId() == null) {
 recipientAccountMapper.insert(entity);
 } else {
 recipientAccountMapper.updateById(entity);
 }
 return NoticeRecipientAccountConvert.toVO(entity);
 }

 @Override
 public WecomUserSyncResultVO syncWecomUsers(SyncWecomUsersCommand command) {
 Require.notNull(command, "同步参数不能为空");
 WecomUserSyncResultVO result = new WecomUserSyncResultVO();
 try {
 validateWecomSyncTarget(command);
 WecomChannelConfig config = resolveWecomSyncConfig(command);
 Long departmentId = resolveEffectiveWecomDepartmentId(command);
 if (Boolean.TRUE.equals(command.getSyncDepartments())) {
 try {
 Long departmentQueryId = Long.valueOf(WECOM_ROOT_DEPARTMENT_ID).equals(departmentId) ? null : departmentId;
 List<WecomDepartment> departments = wecomDirectoryClient.listDepartments(config.corpId(), config.secret(), departmentQueryId);
 result.setDepartmentTotalCount(departments.size());
 syncWecomDepartments(command, result, departments);
 } catch (WecomApiException ex) {
 result.setFailedCount(result.getFailedCount() + 1);
 result.addMessage(ex.getFailReason());
 } catch (RuntimeException ex) {
 result.setFailedCount(result.getFailedCount() + 1);
 result.addMessage("企业微信部门同步失败：" + ex.getMessage());
 }
 }
 if (Boolean.TRUE.equals(command.getSyncUsers())) {
 List<WecomDirectoryUser> users = wecomDirectoryClient.listUsers(config.corpId(), config.secret(), departmentId,
 !Boolean.FALSE.equals(command.getFetchChild()));
 result.setTotalCount(users.size());
 for (WecomDirectoryUser wecomUser : users) {
 syncOneWecomUser(command, result, wecomUser, config);
 }
 }
 } catch (WecomApiException ex) {
 return syncFailure(ex.getFailReason());
 } catch (IllegalArgumentException ex) {
 return syncFailure(ex.getMessage());
 } catch (RuntimeException ex) {
 return syncFailure("企业微信通讯录同步失败：" + ex.getMessage());
 }
 return result;
 }

 @Override
 public boolean disableRecipientAccount(Long currentUserId, Long id, Long userId) {
 Require.notNull(id, "接收账户 ID 不能为空");
 Long targetUserId = resolveTargetUserId(currentUserId, userId);
 NoticeRecipientAccountEntity entity = new NoticeRecipientAccountEntity();
 entity.setId(id);
 entity.setEnabled(false);
 entity.setVerifiedStatus(NoticeRecipientAccountStatus.DISABLED);
 return recipientAccountMapper.update(entity, new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getId, id)
 .eq(NoticeRecipientAccountEntity::getUserId, targetUserId)) > 0;
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean setDefaultRecipientAccount(Long currentUserId, Long id, Long userId) {
 Require.notNull(id, "接收账户 ID 不能为空");
 Long targetUserId = resolveTargetUserId(currentUserId, userId);
 NoticeRecipientAccountEntity account = recipientAccountMapper.selectOne(new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getId, id)
 .eq(NoticeRecipientAccountEntity::getUserId, targetUserId));
 Require.notNull(account, "接收账户不存在");
 clearDefaultAccount(targetUserId, account.getAccountType());
 account.setDefaultAccount(true);
 return recipientAccountMapper.updateById(account) > 0;
 }

 @Override
 public List<NoticeReceivePreferenceVO> listReceivePreferences(Long currentUserId, NoticeReceivePreferenceQuery query) {
 Long userId = resolveTargetUserId(currentUserId, query == null ? null : query.getUserId());
 LambdaQueryWrapper<NoticeReceivePreferenceEntity> wrapper = new LambdaQueryWrapper<NoticeReceivePreferenceEntity>()
 .eq(NoticeReceivePreferenceEntity::getUserId, userId);
 if (query != null && query.getScopeType() != null) {
 wrapper.eq(NoticeReceivePreferenceEntity::getScopeType, query.getScopeType());
 }
 if (query != null && StringUtils.hasText(query.getScopeValue())) {
 wrapper.eq(NoticeReceivePreferenceEntity::getScopeValue, query.getScopeValue());
 }
 wrapper.orderByAsc(NoticeReceivePreferenceEntity::getScopeType)
 .orderByAsc(NoticeReceivePreferenceEntity::getScopeValue)
 .orderByAsc(NoticeReceivePreferenceEntity::getChannelType);
 return receivePreferenceMapper.selectList(wrapper).stream()
 .map(NoticeReceivePreferenceConvert::toVO)
 .toList();
 }

 @Override
 public NoticeReceivePreferenceVO saveReceivePreference(Long currentUserId, SaveNoticeReceivePreferenceCommand command) {
 Require.notNull(command, "接收偏好不能为空");
 Long userId = resolveTargetUserId(currentUserId, command.getUserId());
 String scopeValue = normalizeScopeValue(command.getScopeValue());
 NoticeReceivePreferenceEntity entity = findPreference(userId, command.getScopeType(), scopeValue, command.getChannelType());
 if (entity == null) {
 entity = new NoticeReceivePreferenceEntity();
 entity.setUserId(userId);
 entity.setScopeType(command.getScopeType());
 entity.setScopeValue(scopeValue);
 entity.setChannelType(command.getChannelType());
 }
 entity.setEnabled(command.getEnabled());
 entity.setAccountId(command.getAccountId());
 if (entity.getId() == null) {
 receivePreferenceMapper.insert(entity);
 } else {
 receivePreferenceMapper.updateById(entity);
 }
 return NoticeReceivePreferenceConvert.toVO(entity);
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
 if (query.getPriority() != null) {
 wrapper.eq(NoticeSiteMessageEntity::getPriority, query.getPriority());
 }
 if (StringUtils.hasText(query.getKeyword())) {
 wrapper.and(item -> item.like(NoticeSiteMessageEntity::getTitle, query.getKeyword())
 .or()
 .like(NoticeSiteMessageEntity::getContent, query.getKeyword()));
 }
 if (StringUtils.hasText(query.getBizId())) {
 wrapper.eq(NoticeSiteMessageEntity::getBizId, query.getBizId());
 }
 if (query.getStartTime() != null) {
 wrapper.ge(NoticeSiteMessageEntity::getCreatedAt, query.getStartTime());
 }
 if (query.getEndTime() != null) {
 wrapper.le(NoticeSiteMessageEntity::getCreatedAt, query.getEndTime());
 }
 Set<String> bizTypes = null;
 if (StringUtils.hasText(query.getBizGroup())) {
 bizTypes = businessTypeMapper.selectList(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .eq(NoticeBusinessTypeEntity::getBizGroup, query.getBizGroup()))
 .stream()
 .map(NoticeBusinessTypeEntity::getBizType)
 .collect(Collectors.toSet());
 if (bizTypes.isEmpty()) {
 return PageResult.of(List.of(), 0, query.getPageNum(), query.getPageSize());
 }
 wrapper.in(NoticeSiteMessageEntity::getBizType, bizTypes);
 }
 wrapper.orderByDesc(NoticeSiteMessageEntity::getTopStatus).orderByDesc(NoticeSiteMessageEntity::getCreatedAt);
 Page<NoticeSiteMessageEntity> result = messageMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(this::toSiteMessageVO).toList(), result.getTotal(), result.getCurrent(), result.getSize());
 }

 @Override
 public NoticeSiteMessageVO getSiteMessage(Long id, Long userId) {
 NoticeSiteMessageEntity entity = messageMapper.selectOne(userVisibleWrapper(userId).eq(NoticeSiteMessageEntity::getId, id));
 return entity == null ? null : toSiteMessageVO(entity);
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

 private NoticeTaskVO toTaskVO(NoticeTaskEntity entity) {
 NoticeTaskVO vo = NoticeTaskConvert.toVO(entity);
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .eq(NoticeBusinessTypeEntity::getBizType, entity.getBizType())
 .last("limit 1"));
 if (businessType != null) {
 vo.setBizGroup(businessType.getBizGroup());
 vo.setBizName(businessType.getBizName());
 }
 return vo;
 }

 private NoticeSiteMessageVO toSiteMessageVO(NoticeSiteMessageEntity entity) {
 NoticeSiteMessageVO vo = NoticeSiteMessageConvert.toVO(entity);
 if (entity.getBizType() == null) {
 return vo;
 }
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectOne(new LambdaQueryWrapper<NoticeBusinessTypeEntity>()
 .eq(NoticeBusinessTypeEntity::getBizType, entity.getBizType())
 .last("limit 1"));
 if (businessType != null) {
 vo.setBizGroup(businessType.getBizGroup());
 vo.setBizName(businessType.getBizName());
 }
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
 if (!templates.isEmpty() || businessType.getId() != null) {
 return ensureSiteTemplate(command, businessType, templates);
 }
 if (StringUtils.hasText(command.getTitle()) || StringUtils.hasText(command.getContent())) {
 return directTemplates(command, businessType, requested);
 }
 throw new IllegalStateException("业务类型未配置启用渠道模板");
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

 private List<NoticeRecipientCommand> resolveRecipients(SendNoticeCommand command) {
 Map<Long, NoticeRecipientCommand> userRecipients = new LinkedHashMap<>();
 List<NoticeRecipientCommand> externalRecipients = new ArrayList<>();
 if (command.getRecipients() != null) {
 for (NoticeRecipientCommand recipient : command.getRecipients()) {
 if (recipient.getUserId() == null) {
 externalRecipients.add(recipient);
 } else {
 enrichRecipientFromUser(recipient);
 userRecipients.putIfAbsent(recipient.getUserId(), recipient);
 }
 }
 }
 receiverIds(command).forEach(userId -> {
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(userId);
 enrichRecipientFromUser(recipient);
 userRecipients.putIfAbsent(userId, recipient);
 });
 resolveRecipientTargets(command).forEach(recipient ->
 userRecipients.putIfAbsent(recipient.getUserId(), recipient));
 List<NoticeRecipientCommand> recipients = new ArrayList<>(externalRecipients);
 recipients.addAll(userRecipients.values());
 return recipients;
 }

 private List<NoticeRecipientCommand> resolveRecipientTargets(SendNoticeCommand command) {
 if (command.getRecipientTargets() == null || command.getRecipientTargets().isEmpty() || identityUserApi == null) {
 return List.of();
 }
 return command.getRecipientTargets().stream()
 .filter(target -> target.getTargetType() != null && target.getTargetId() != null)
 .flatMap(target -> listUsersByTarget(target).stream())
 .collect(Collectors.toMap(NoticeRecipientCommand::getUserId, Function.identity(), (left, right) -> left,
 LinkedHashMap::new))
 .values()
 .stream()
 .toList();
 }

 private List<NoticeRecipientCommand> listUsersByTarget(NoticeRecipientTargetCommand target) {
 IdentityUserTargetQuery query = new IdentityUserTargetQuery();
 query.setTargetType(IdentityUserTargetType.valueOf(target.getTargetType().name()));
 query.setTargetId(target.getTargetId());
 query.setStatus(1);
 R<List<IdentityUserInfo>> response = identityUserApi.listUserInfosByTarget(query);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return List.of();
 }
 return response.getData().stream()
 .filter(user -> user.getUserId() != null)
 .map(this::toRecipient)
 .toList();
 }

 private NoticeRecipientCommand toRecipient(IdentityUserInfo user) {
 NoticeRecipientCommand recipient = new NoticeRecipientCommand();
 recipient.setUserId(user.getUserId());
 recipient.setRecipientName(firstText(user.getNickname(), user.getUsername()));
 recipient.setMobile(user.getPhone());
 recipient.setEmail(user.getEmail());
 return recipient;
 }

 private void enrichRecipientFromUser(NoticeRecipientCommand recipient) {
 if (recipient.getUserId() == null || identityUserApi == null) {
 return;
 }
 R<IdentityUserInfo> response = identityUserApi.getUserInfoById(recipient.getUserId());
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return;
 }
 IdentityUserInfo user = response.getData();
 if (!StringUtils.hasText(recipient.getRecipientName())) {
 recipient.setRecipientName(firstText(user.getNickname(), user.getUsername()));
 }
 if (!StringUtils.hasText(recipient.getMobile())) {
 recipient.setMobile(user.getPhone());
 }
 if (!StringUtils.hasText(recipient.getEmail())) {
 recipient.setEmail(user.getEmail());
 }
 }

 private String firstText(String first, String second) {
 return StringUtils.hasText(first) ? first : second;
 }

 private String stringValue(Object value) {
 if (value == null) {
 return null;
 }
 String text = String.valueOf(value).trim();
 return StringUtils.hasText(text) ? text : null;
 }

 private boolean booleanValue(Object value) {
 if (value instanceof Boolean bool) {
 return bool;
 }
 return value != null && Boolean.parseBoolean(String.valueOf(value));
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
 Map<String, Object> params = command.getParams() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(command.getParams());
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

 private Long resolveTargetUserId(Long currentUserId, Long requestedUserId) {
 return requestedUserId == null ? currentUserId : requestedUserId;
 }

 private WecomChannelConfig resolveWecomSyncConfig(SyncWecomUsersCommand command) {
 String corpId = trimToNull(command.getCorpId());
 String secret = trimToNull(command.getSecret());
 return resolveWecomChannelConfig(command.getChannelConfigId(), corpId, secret);
 }

 private WecomChannelConfig resolveWecomChannelConfig(Long channelConfigId, String corpId, String secret) {
 if (corpId == null || secret == null) {
 NoticeChannelConfigEntity config = channelConfigId == null
 ? defaultWecomChannelConfig()
 : channelConfigMapper.selectById(channelConfigId);
 Require.notNull(config, "未找到可用企业微信渠道配置");
 WecomChannelConfig channelConfig = WecomChannelConfig.fromJson(config.getConfigJson());
 corpId = firstText(corpId, channelConfig.corpId());
 secret = firstText(secret, channelConfig.secret());
 }
 Require.notBlank(corpId, "企业微信 CorpId 不能为空");
 Require.notBlank(secret, "企业微信通讯录 Secret 不能为空");
 return new WecomChannelConfig(corpId, null, secret);
 }

 private NoticeChannelConfigEntity defaultWecomChannelConfig() {
 return channelConfigMapper.selectOne(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getChannelType, NoticeChannelType.WECOM)
 .eq(NoticeChannelConfigEntity::getEnabled, true)
 .eq(NoticeChannelConfigEntity::getConfigStatus, NoticeChannelConfigStatus.COMPLETE)
 .orderByDesc(NoticeChannelConfigEntity::getPriority)
 .orderByAsc(NoticeChannelConfigEntity::getId)
 .last("LIMIT 1"));
 }

 private void syncWecomDepartments(SyncWecomUsersCommand command, WecomUserSyncResultVO result,
 List<WecomDepartment> departments) {
 if (sysOrgApi == null) {
 throw new IllegalStateException("组织服务不可用");
 }
 if (departments == null || departments.isEmpty()) {
 return;
 }
 Long rootOrgId = resolveTargetRootOrgId(command);
 Map<Long, NoticeWecomSyncMappingEntity> syncedMappings = new LinkedHashMap<>();
 List<WecomDepartment> pending = departments.stream()
 .filter(department -> department.id() != null)
 .sorted(Comparator.comparing((WecomDepartment department) -> department.id().equals(WECOM_ROOT_DEPARTMENT_ID) ? 0 : 1)
 .thenComparing(department -> department.parentId() == null ? 0L : department.parentId())
 .thenComparing(WecomDepartment::id))
 .collect(Collectors.toCollection(ArrayList::new));
 int lastPendingSize = -1;
 while (!pending.isEmpty() && lastPendingSize != pending.size()) {
 lastPendingSize = pending.size();
 List<WecomDepartment> deferred = new ArrayList<>();
 for (WecomDepartment department : pending) {
 NoticeWecomSyncMappingEntity mapping = syncOneWecomDepartment(command, result, department, rootOrgId, syncedMappings);
 if (mapping == null) {
 deferred.add(department);
 } else {
 syncedMappings.put(department.id(), mapping);
 }
 }
 pending = deferred;
 }
 for (WecomDepartment department : pending) {
 result.setDepartmentSkippedCount(result.getDepartmentSkippedCount() + 1);
 result.addMessage("跳过未找到父部门映射的企业微信部门：" + department.id());
 }
 }

 private Long resolveEffectiveWecomDepartmentId(SyncWecomUsersCommand command) {
 if (command.getDepartmentId() != null) {
 return command.getDepartmentId();
 }
 if (!Boolean.TRUE.equals(command.getSyncDepartments()) && command.getTargetOrgId() != null) {
 NoticeWecomSyncMappingEntity mapping =
 findWecomSyncMappingByLocalId(WECOM_SYNC_TYPE_DEPARTMENT, command.getTargetOrgId());
 if (mapping == null || !StringUtils.hasText(mapping.getExternalId())) {
 throw new IllegalStateException("当前部门未建立企业微信部门映射，请先选择所属公司同步组织架构");
 }
 try {
 return Long.valueOf(mapping.getExternalId());
 } catch (NumberFormatException ex) {
 throw new IllegalStateException("当前部门的企业微信部门映射无效");
 }
 }
 return WECOM_ROOT_DEPARTMENT_ID;
 }

 private void validateWecomSyncTarget(SyncWecomUsersCommand command) {
 if (command.getTargetOrgType() == null) {
 return;
 }
 if (ORG_TYPE_GROUP.equals(command.getTargetOrgType())) {
 throw new IllegalArgumentException("集团节点不支持同步企业微信用户，请选择二级公司或已映射部门");
 }
 if (!ORG_TYPE_COMPANY.equals(command.getTargetOrgType()) && !ORG_TYPE_DEPARTMENT.equals(command.getTargetOrgType())) {
 throw new IllegalArgumentException("当前组织类型不支持同步企业微信用户，请选择二级公司或已映射部门");
 }
 }

 private Long resolveTargetRootOrgId(SyncWecomUsersCommand command) {
 if (command.getTargetOrgId() != null) {
 SysOrg targetOrg = getOrg(command.getTargetOrgId());
 if (targetOrg == null) {
 throw new IllegalStateException("同步目标组织不存在");
 }
 return targetOrg.getId();
 }
 return resolveRootOrgId();
 }

 private NoticeWecomSyncMappingEntity syncOneWecomDepartment(SyncWecomUsersCommand command, WecomUserSyncResultVO result,
 WecomDepartment department, Long rootOrgId, Map<Long, NoticeWecomSyncMappingEntity> syncedMappings) {
 String externalId = String.valueOf(department.id());
 NoticeWecomSyncMappingEntity mapping = findWecomSyncMapping(WECOM_SYNC_TYPE_DEPARTMENT, externalId);
 if (department.id().equals(WECOM_ROOT_DEPARTMENT_ID)) {
 String hash = hashValues(department.name(), department.parentId(), department.order(), rootOrgId);
 if (mapping == null) {
 mapping = saveWecomSyncMapping(null, WECOM_SYNC_TYPE_DEPARTMENT, externalId, rootOrgId, hash,
 firstText(department.name(), "企业微信根部门"));
 } else if (!Objects.equals(mapping.getLocalId(), rootOrgId) || !Objects.equals(mapping.getDataHash(), hash)) {
 mapping = saveWecomSyncMapping(mapping, WECOM_SYNC_TYPE_DEPARTMENT, externalId, rootOrgId, hash,
 firstText(department.name(), "企业微信根部门"));
 }
 result.setDepartmentSkippedCount(result.getDepartmentSkippedCount() + 1);
 return mapping;
 }
 Long parentLocalId = resolveDepartmentParentLocalId(department, rootOrgId, syncedMappings);
 if (parentLocalId == null) {
 return null;
 }
 String hash = hashValues(department.name(), department.parentId(), department.order(), parentLocalId);
 SysOrg existing = mapping == null ? null : getOrg(mapping.getLocalId());
 if (mapping != null && existing != null
 && Boolean.TRUE.equals(command.getSkipUnchanged()) && Objects.equals(mapping.getDataHash(), hash)) {
 result.setDepartmentSkippedCount(result.getDepartmentSkippedCount() + 1);
 return mapping;
 }
 if (existing == null) {
 Long orgId = createWecomOrg(department, parentLocalId);
 mapping = saveWecomSyncMapping(mapping, WECOM_SYNC_TYPE_DEPARTMENT, externalId, orgId, hash, department.name());
 result.setDepartmentCreatedCount(result.getDepartmentCreatedCount() + 1);
 return mapping;
 }
 updateWecomOrg(existing, department, parentLocalId);
 mapping = saveWecomSyncMapping(mapping, WECOM_SYNC_TYPE_DEPARTMENT, externalId, existing.getId(), hash, department.name());
 result.setDepartmentUpdatedCount(result.getDepartmentUpdatedCount() + 1);
 return mapping;
 }

 private Long resolveDepartmentParentLocalId(WecomDepartment department, Long rootOrgId,
 Map<Long, NoticeWecomSyncMappingEntity> syncedMappings) {
 if (department.parentId() == null || department.parentId().equals(WECOM_ROOT_DEPARTMENT_ID)) {
 return rootOrgId;
 }
 NoticeWecomSyncMappingEntity syncedParent = syncedMappings.get(department.parentId());
 if (syncedParent != null) {
 return syncedParent.getLocalId();
 }
 NoticeWecomSyncMappingEntity existingParent =
 findWecomSyncMapping(WECOM_SYNC_TYPE_DEPARTMENT, String.valueOf(department.parentId()));
 return existingParent == null ? null : existingParent.getLocalId();
 }

 private Long resolveRootOrgId() {
 SysOrgTreeQuery query = new SysOrgTreeQuery();
 query.setParentId(0L);
 query.setIncludeDisabled(true);
 R<List<SysOrg>> response = sysOrgApi.tree(query);
 if (response == null || !response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
 throw new IllegalStateException(response == null ? "未找到Mango根组织" : response.getMsg());
 }
 return response.getData().get(0).getId();
 }

 private Long createWecomOrg(WecomDepartment department, Long parentLocalId) {
 CreateOrgCommand create = new CreateOrgCommand();
 create.setPid(parentLocalId);
 create.setOrgName(firstText(department.name(), "企业微信部门" + department.id()));
 create.setOrgCode(wecomDepartmentOrgCode(department.id()));
 create.setOrgType(3);
 create.setOrgSort(department.order() == null ? 0 : department.order());
 create.setOrgStatus("1");
 R<Long> response = sysOrgApi.create(create);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 throw new IllegalStateException(response == null ? "创建组织失败" : response.getMsg());
 }
 return response.getData();
 }

 private void ensureWecomUserOrgRelation(Long userId, Long orgId) {
 if (userId == null || orgId == null || identityUserApi == null || sysOrgApi == null) {
 return;
 }
 R<IdentityUserVO> detailResponse = identityUserApi.detail(userId);
 if (detailResponse == null || !detailResponse.isSuccess() || detailResponse.getData() == null
 || detailResponse.getData().getMemberId() == null) {
 return;
 }
 IdentityUserVO detail = detailResponse.getData();
 if (Objects.equals(detail.getPrimaryOrgId(), orgId)) {
 return;
 }
 AddOrgMemberCommand addMember = new AddOrgMemberCommand();
 addMember.setMemberId(detail.getMemberId());
 addMember.setPrimaryFlag(true);
 addMember.setLeaderFlag(false);
 try {
 R<Void> response = sysOrgApi.addMember(orgId, addMember);
 if (response == null || !response.isSuccess()) {
 String message = response == null ? "加入组织失败" : response.getMsg();
 if (!alreadyExistsMessage(message)) {
 throw new IllegalStateException(message);
 }
 }
 } catch (RuntimeException ex) {
 if (!alreadyExistsMessage(ex.getMessage())) {
 throw ex;
 }
 }
 }

 private boolean alreadyExistsMessage(String message) {
 return StringUtils.hasText(message) && (message.contains("已") || message.contains("exist"));
 }

 private void updateWecomOrg(SysOrg existing, WecomDepartment department, Long parentLocalId) {
 UpdateOrgCommand update = new UpdateOrgCommand();
 update.setId(existing.getId());
 update.setPid(parentLocalId);
 update.setOrgName(firstText(department.name(), existing.getOrgName()));
 update.setOrgCode(firstText(existing.getOrgCode(), wecomDepartmentOrgCode(department.id())));
 update.setOrgType(existing.getOrgType() == null ? 3 : existing.getOrgType());
 update.setOrgSort(department.order() == null ? existing.getOrgSort() : department.order());
 update.setOrgStatus(firstText(existing.getOrgStatus(), "1"));
 R<Void> response = sysOrgApi.update(update);
 if (response == null || !response.isSuccess()) {
 throw new IllegalStateException(response == null ? "更新组织失败" : response.getMsg());
 }
 }

 private SysOrg getOrg(Long orgId) {
 if (orgId == null) {
 return null;
 }
 try {
 R<SysOrg> response = sysOrgApi.getById(orgId);
 if (response == null || !response.isSuccess()) {
 return null;
 }
 return response.getData();
 } catch (RuntimeException ex) {
 return null;
 }
 }

 private String wecomDepartmentOrgCode(Long departmentId) {
 return "WECOM_DEPT_" + departmentId;
 }

 private void syncOneWecomUser(SyncWecomUsersCommand command, WecomUserSyncResultVO result,
 WecomDirectoryUser wecomUser, WecomChannelConfig config) {
 if (!StringUtils.hasText(wecomUser.userId())) {
 result.setSkippedCount(result.getSkippedCount() + 1);
 result.addMessage("跳过缺少 userid 的企业微信成员");
 return;
 }
 try {
 Long primaryOrgId = resolveUserPrimaryOrgId(command, wecomUser);
 String dataHash = hashWecomUser(wecomUser, primaryOrgId);
 NoticeWecomSyncMappingEntity mapping = findWecomSyncMapping(WECOM_SYNC_TYPE_USER, wecomUser.userId());
 if (mapping != null && Boolean.TRUE.equals(command.getSkipUnchanged())
 && Objects.equals(mapping.getDataHash(), dataHash)) {
 ensureWecomUserOrgRelation(mapping.getLocalId(), primaryOrgId);
 if (Boolean.TRUE.equals(command.getBindLoginIdentity())) {
 bindWecomLoginIdentity(mapping.getLocalId(), wecomUser, config);
 }
 result.setSkippedCount(result.getSkippedCount() + 1);
 result.setUnchangedCount(result.getUnchangedCount() + 1);
 return;
 }
 IdentityUserVO user = mapping == null ? null : findIdentityUserById(mapping.getLocalId());
 if (user == null) {
 user = findMatchedIdentityUser(wecomUser);
 if (user != null) {
 result.setMatchedCount(result.getMatchedCount() + 1);
 }
 }
 if (user == null && Boolean.TRUE.equals(command.getCreateMissingUsers())) {
 user = createWecomIdentityUser(wecomUser, primaryOrgId);
 result.setCreatedCount(result.getCreatedCount() + 1);
 } else if (user != null) {
 if (Boolean.TRUE.equals(command.getUpdateMatchedUsers()) && updateWecomIdentityUser(user, wecomUser, primaryOrgId)) {
 result.setUpdatedCount(result.getUpdatedCount() + 1);
 }
 }
 if (user == null || user.getUserId() == null) {
 result.setSkippedCount(result.getSkippedCount() + 1);
 result.addMessage("未匹配成员：" + wecomUser.userId());
 return;
 }
 ensureWecomUserOrgRelation(user.getUserId(), primaryOrgId);
 saveWecomSyncMapping(mapping, WECOM_SYNC_TYPE_USER, wecomUser.userId(), user.getUserId(), dataHash,
 firstText(wecomUser.name(), wecomUser.userId()));
 if (Boolean.TRUE.equals(command.getBindLoginIdentity())) {
 bindWecomLoginIdentity(user.getUserId(), wecomUser, config);
 }
 if (Boolean.TRUE.equals(command.getBindNoticeAccount())) {
 upsertWecomRecipientAccount(user.getUserId(), wecomUser);
 result.setBoundAccountCount(result.getBoundAccountCount() + 1);
 }
 } catch (RuntimeException ex) {
 result.setFailedCount(result.getFailedCount() + 1);
 result.addMessage("同步失败：" + wecomUser.userId() + "，" + ex.getMessage());
 }
 }

 private void bindWecomLoginIdentity(Long userId, WecomDirectoryUser wecomUser, WecomChannelConfig config) {
 if (userId == null || wecomUser == null || !StringUtils.hasText(wecomUser.userId())) {
 return;
 }
 BindExternalIdentityCommand bind = new BindExternalIdentityCommand();
 bind.setUserId(userId);
 bind.setProvider("WECOM");
 bind.setCorpId(config.corpId());
 bind.setExternalUserId(wecomUser.userId());
 bind.setDisplayName(firstText(wecomUser.name(), wecomUser.userId()));
 bind.setBindSource("SYNC");
 R<?> response = identityUserApi.bindExternalIdentity(bind);
 if (response == null || !response.isSuccess()) {
 throw new IllegalStateException(response == null ? "绑定企微登录身份失败" : response.getMsg());
 }
 }

 private Long resolveUserPrimaryOrgId(SyncWecomUsersCommand command, WecomDirectoryUser wecomUser) {
 if (wecomUser.departments() == null || wecomUser.departments().isEmpty()) {
 return command.getTargetOrgId();
 }
 for (Long departmentId : wecomUser.departments()) {
 NoticeWecomSyncMappingEntity mapping =
 findWecomSyncMapping(WECOM_SYNC_TYPE_DEPARTMENT, String.valueOf(departmentId));
 if (mapping != null && mapping.getLocalId() != null && getOrg(mapping.getLocalId()) != null) {
 return mapping.getLocalId();
 }
 }
 return command.getTargetOrgId();
 }

 private IdentityUserVO findMatchedIdentityUser(WecomDirectoryUser wecomUser) {
 IdentityUserVO user = findIdentityUserBy(wecomUser.mobile(),
 item -> StringUtils.hasText(item.getPhone()) && Objects.equals(item.getPhone(), wecomUser.mobile()),
 IdentityUserPageQuery::setPhone);
 if (user != null) {
 return user;
 }
 String email = firstText(wecomUser.email(), wecomUser.bizMail());
 user = findIdentityUserBy(email,
 item -> StringUtils.hasText(item.getEmail()) && Objects.equals(item.getEmail(), email),
 IdentityUserPageQuery::setEmail);
 if (user != null) {
 return user;
 }
 return findIdentityUserBy(wecomUser.userId(),
 item -> Objects.equals(item.getUsername(), wecomUser.userId()),
 IdentityUserPageQuery::setUsername);
 }

 private IdentityUserVO findIdentityUserBy(String value, Predicate<IdentityUserVO> exactMatcher,
 java.util.function.BiConsumer<IdentityUserPageQuery, String> querySetter) {
 if (!StringUtils.hasText(value)) {
 return null;
 }
 IdentityUserPageQuery query = new IdentityUserPageQuery();
 query.setPage(1);
 query.setSize(20);
 querySetter.accept(query, value.trim());
 R<PageResult<IdentityUserVO>> response = identityUserApi.page(query);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return null;
 }
 return response.getData().getList().stream()
 .filter(exactMatcher)
 .findFirst()
 .orElse(null);
 }

 private IdentityUserVO findIdentityUserById(Long userId) {
 if (userId == null) {
 return null;
 }
 R<IdentityUserVO> detailResponse = identityUserApi.detail(userId);
 if (detailResponse != null && detailResponse.isSuccess() && detailResponse.getData() != null) {
 return detailResponse.getData();
 }
 R<IdentityUserInfo> response = identityUserApi.getUserInfoById(userId);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return null;
 }
 IdentityUserInfo info = response.getData();
 IdentityUserVO user = new IdentityUserVO();
 user.setUserId(info.getUserId());
 user.setUsername(info.getUsername());
 user.setNickname(info.getNickname());
 user.setPartyType(info.getPartyType());
 user.setPartyId(info.getPartyId());
 user.setPhone(info.getPhone());
 user.setEmail(info.getEmail());
 user.setAvatar(info.getAvatar());
 user.setStatus(info.getStatus());
 return user;
 }

 private IdentityUserVO createWecomIdentityUser(WecomDirectoryUser wecomUser, Long primaryOrgId) {
 CreateIdentityUserCommand create = new CreateIdentityUserCommand();
 create.setUsername(wecomUser.userId().trim());
 create.setNickname(firstText(wecomUser.name(), wecomUser.userId()));
 create.setRealm("INTERNAL");
 create.setActorType("INTERNAL_USER");
 if (primaryOrgId != null) {
 create.setPartyType(INTERNAL_ORG_PARTY_TYPE);
 create.setPartyId(primaryOrgId);
 }
 create.setPhone(trimToNull(wecomUser.mobile()));
 create.setEmail(trimToNull(firstText(wecomUser.email(), wecomUser.bizMail())));
 create.setAvatar(trimToNull(wecomUser.avatar()));
 create.setStatus(wecomActive(wecomUser) ? 1 : 0);
 create.setRemark("企业微信同步");
 R<Long> response = identityUserApi.create(create);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 throw new IllegalStateException(response == null ? "创建成员失败" : response.getMsg());
 }
 IdentityUserVO user = new IdentityUserVO();
 user.setUserId(response.getData());
 user.setUsername(create.getUsername());
 user.setNickname(create.getNickname());
 user.setPhone(create.getPhone());
 user.setEmail(create.getEmail());
 user.setAvatar(create.getAvatar());
 user.setPartyType(create.getPartyType());
 user.setPartyId(create.getPartyId());
 user.setStatus(create.getStatus());
 return user;
 }

 private boolean updateWecomIdentityUser(IdentityUserVO user, WecomDirectoryUser wecomUser, Long primaryOrgId) {
 UpdateIdentityUserCommand update = new UpdateIdentityUserCommand();
 update.setUserId(user.getUserId());
 update.setNickname(firstText(wecomUser.name(), user.getNickname()));
 update.setPartyType(primaryOrgId == null ? user.getPartyType() : INTERNAL_ORG_PARTY_TYPE);
 update.setPartyId(primaryOrgId == null ? user.getPartyId() : primaryOrgId);
 update.setPhone(firstText(wecomUser.mobile(), user.getPhone()));
 update.setEmail(firstText(firstText(wecomUser.email(), wecomUser.bizMail()), user.getEmail()));
 update.setAvatar(firstText(wecomUser.avatar(), user.getAvatar()));
 update.setStatus(wecomActive(wecomUser) ? 1 : 0);
 update.setRemark(user.getRemark());
 R<Boolean> response = identityUserApi.update(update);
 if (response == null || !response.isSuccess()) {
 throw new IllegalStateException(response == null ? "更新成员失败" : response.getMsg());
 }
 return Boolean.TRUE.equals(response.getData());
 }

 private NoticeWecomSyncMappingEntity findWecomSyncMapping(String syncType, String externalId) {
 if (!StringUtils.hasText(syncType) || !StringUtils.hasText(externalId)) {
 return null;
 }
 return wecomSyncMappingMapper.selectOne(new LambdaQueryWrapper<NoticeWecomSyncMappingEntity>()
 .eq(NoticeWecomSyncMappingEntity::getTenantId, tenantId())
 .eq(NoticeWecomSyncMappingEntity::getSyncType, syncType)
 .eq(NoticeWecomSyncMappingEntity::getExternalId, externalId)
 .last("LIMIT 1"));
 }

 private NoticeWecomSyncMappingEntity saveWecomSyncMapping(NoticeWecomSyncMappingEntity mapping, String syncType,
 String externalId, Long localId, String dataHash, String displayName) {
 NoticeWecomSyncMappingEntity entity = mapping == null ? new NoticeWecomSyncMappingEntity() : mapping;
 entity.setSyncType(syncType);
 entity.setExternalId(externalId);
 entity.setLocalId(localId);
 entity.setDataHash(dataHash);
 entity.setDisplayName(displayName);
 entity.setTenantId(tenantId());
 if (entity.getId() == null) {
 wecomSyncMappingMapper.insert(entity);
 } else {
 wecomSyncMappingMapper.updateById(entity);
 }
 return entity;
 }

 private NoticeWecomSyncMappingEntity findWecomSyncMappingByLocalId(String syncType, Long localId) {
 if (!StringUtils.hasText(syncType) || localId == null) {
 return null;
 }
 return wecomSyncMappingMapper.selectOne(new LambdaQueryWrapper<NoticeWecomSyncMappingEntity>()
 .eq(NoticeWecomSyncMappingEntity::getTenantId, tenantId())
 .eq(NoticeWecomSyncMappingEntity::getSyncType, syncType)
 .eq(NoticeWecomSyncMappingEntity::getLocalId, localId)
 .last("LIMIT 1"));
 }

 private String hashWecomUser(WecomDirectoryUser wecomUser, Long primaryOrgId) {
 return hashValues(wecomUser.userId(), wecomUser.name(), wecomUser.mobile(), wecomUser.email(), wecomUser.bizMail(),
 wecomUser.avatar(), wecomUser.status(), primaryOrgId, wecomUser.departments());
 }

 private String hashValues(Object... values) {
 try {
 MessageDigest digest = MessageDigest.getInstance("SHA-256");
 for (Object value : values) {
 digest.update(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
 digest.update((byte) 0);
 }
 return HexFormat.of().formatHex(digest.digest());
 } catch (NoSuchAlgorithmException ex) {
 throw new IllegalStateException("同步数据指纹计算失败", ex);
 }
 }

 private String tenantId() {
 return firstText(MangoContextHolder.tenantId(), "default");
 }

 private NoticeRecipientAccountEntity upsertWecomRecipientAccount(Long userId, WecomDirectoryUser wecomUser) {
 NoticeRecipientAccountEntity account = recipientAccountMapper.selectOne(
 new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getUserId, userId)
 .eq(NoticeRecipientAccountEntity::getTenantId, tenantId())
 .eq(NoticeRecipientAccountEntity::getAccountType, NoticeRecipientAccountType.WECOM)
 .eq(NoticeRecipientAccountEntity::getAccountValue, wecomUser.userId())
 .last("LIMIT 1"));
 if (account == null) {
 account = new NoticeRecipientAccountEntity();
 account.setUserId(userId);
 account.setAccountType(NoticeRecipientAccountType.WECOM);
 account.setAccountValue(wecomUser.userId());
 }
 account.setDisplayName(firstText(wecomUser.name(), wecomUser.userId()));
 account.setVerifiedStatus(NoticeRecipientAccountStatus.VERIFIED);
 account.setDefaultAccount(true);
 account.setEnabled(true);
 account.setTenantId(tenantId());
 clearDefaultAccount(userId, NoticeRecipientAccountType.WECOM);
 if (account.getId() == null) {
 recipientAccountMapper.insert(account);
 } else {
 recipientAccountMapper.updateById(account);
 }
 return account;
 }

 private boolean wecomActive(WecomDirectoryUser wecomUser) {
 return wecomUser.status() == null || Integer.valueOf(1).equals(wecomUser.status());
 }

 private WecomUserSyncResultVO syncFailure(String reason) {
 WecomUserSyncResultVO result = new WecomUserSyncResultVO();
 result.setFailedCount(1);
 result.addMessage(reason);
 return result;
 }

 private String trimToNull(String value) {
 if (!StringUtils.hasText(value)) {
 return null;
 }
 return value.trim();
 }

 private String normalizeScopeValue(String scopeValue) {
 return StringUtils.hasText(scopeValue) ? scopeValue : "";
 }

 private void clearDefaultAccount(Long userId, NoticeRecipientAccountType accountType) {
 NoticeRecipientAccountEntity update = new NoticeRecipientAccountEntity();
 update.setDefaultAccount(false);
 recipientAccountMapper.update(update, new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getUserId, userId)
 .eq(NoticeRecipientAccountEntity::getAccountType, accountType)
 .eq(NoticeRecipientAccountEntity::getDefaultAccount, true));
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

 private String resolveDomainCode(String domainCode, String bizGroup) {
 if (StringUtils.hasText(domainCode)) {
 return domainCode.trim();
 }
 if (StringUtils.hasText(bizGroup)) {
 return bizGroup.trim();
 }
 return "NOTICE";
 }

 private String resolveBizGroup(String bizGroup, String domainCode) {
 return StringUtils.hasText(bizGroup) ? bizGroup.trim() : domainCode;
 }
}
