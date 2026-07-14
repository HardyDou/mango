package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.notice.api.command.CreateNoticeBusinessTypeCommand;
import io.mango.notice.api.command.SaveNoticeBusinessConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.command.SaveNoticeChannelTemplateCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSyncStatus;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.core.convert.NoticeBusinessConfigVersionConvert;
import io.mango.notice.core.convert.NoticeBusinessTypeConvert;
import io.mango.notice.core.convert.NoticeChannelConfigConvert;
import io.mango.notice.core.convert.NoticeChannelTemplateConvert;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.service.INoticeConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeConfigurationService implements INoticeConfigurationService {

 private static final String MASKED_VALUE = "***";
 private static final String SITE_INTERNAL_PROVIDER = "INTERNAL";
 private static final Set<String> SENSITIVE_CONFIG_KEYS = Set.of("secret", "password", "token", "key", "appSecret",
         "accessKey", "accessKeySecret", "secretKey", "smtpPassword");

 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessConfigVersionMapper businessConfigVersionMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeTaskMapper taskMapper;
 private final ObjectMapper objectMapper;

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
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务通知配置不能为空");
 Require.notBlank(command.getBizType(), NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型不能为空");
 Require.notBlank(command.getBizName(), NoticeCode.NOTICE_BUSINESS_ERROR, "名称不能为空");
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
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务通知配置不能为空");
 Require.notBlank(command.getBizName(), NoticeCode.NOTICE_BUSINESS_ERROR, "名称不能为空");
 NoticeBusinessTypeEntity entity = businessTypeMapper.selectById(id);
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型不存在");
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
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectById(id);
 Require.notNull(businessType, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型不存在");
 Long runningTaskCount = taskMapper.selectCount(new LambdaQueryWrapper<NoticeTaskEntity>()
 .eq(NoticeTaskEntity::getBizType, businessType.getBizType())
 .in(NoticeTaskEntity::getStatus, List.of(NoticeTaskStatus.WAITING, NoticeTaskStatus.SENDING)));
 Require.isTrue(runningTaskCount == null || runningTaskCount == 0, NoticeCode.NOTICE_BUSINESS_ERROR, "存在待发送或发送中的通知任务，不能删除");
 channelTemplateMapper.delete(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, businessType.getBizType()));
 businessConfigVersionMapper.delete(new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType()));
 return businessTypeMapper.deleteById(id) > 0;
 }

 @Override
 public boolean enableBusinessType(Long id) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
 NoticeBusinessTypeEntity entity = new NoticeBusinessTypeEntity();
 entity.setId(id);
 entity.setEnabled(true);
 return businessTypeMapper.updateById(entity) > 0;
 }

 @Override
 public boolean disableBusinessType(Long id) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
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
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "业务发布配置不能为空");
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
 Require.notNull(draft, NoticeCode.NOTICE_BUSINESS_ERROR, "没有可发布的业务配置草稿");
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
 Require.notNull(version, NoticeCode.NOTICE_BUSINESS_ERROR, "版本号不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessConfigVersionEntity source = businessConfigVersionMapper.selectOne(
 new LambdaQueryWrapper<NoticeBusinessConfigVersionEntity>()
 .eq(NoticeBusinessConfigVersionEntity::getBizType, businessType.getBizType())
 .eq(NoticeBusinessConfigVersionEntity::getVersion, version)
 .last("limit 1"));
 Require.notNull(source, NoticeCode.NOTICE_BUSINESS_ERROR, "业务配置版本不存在");
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
 public NoticeChannelTemplateVO saveChannelTemplate(Long businessTypeId, SaveNoticeChannelTemplateCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道模板不能为空");
 NoticeChannelType channelType = command.getChannelType();
 Require.notNull(channelType, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
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
 Require.notNull(channelType, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
 NoticeBusinessTypeEntity businessType = requireBusinessType(businessTypeId);
 NoticeBusinessChannelTemplateEntity draft = latestChannelTemplate(businessType.getBizType(), channelType,
 NoticeTemplateVersionStatus.DRAFT);
 Require.notNull(draft, NoticeCode.NOTICE_BUSINESS_ERROR, "没有可发布的渠道模板草稿");
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
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置不存在");
 entity.setChannelType(command.getChannelType());
 entity.setProviderCode(command.getProviderCode());
 entity.setConfigName(command.getConfigName());
 String configJson = mergeMaskedConfigJson(entity.getConfigJson(), command.getConfigJson());
 entity.setConfigJson(configJson);
 entity.setEnabled(command.getEnabled());
 entity.setPriority(command.getPriority());
 Integer weight = command.getWeight();
 if (weight == null || weight <= 0) {
 weight = 100;
 }
 entity.setWeight(weight);
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
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信渠道配置不存在");
 Require.isTrue(entity.getChannelType() == NoticeChannelType.WECOM, NoticeCode.NOTICE_BUSINESS_ERROR, "所选渠道不是企业微信渠道");
 Require.isTrue(Boolean.TRUE.equals(entity.getEnabled()), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信渠道未启用");
 } else {
 entity = channelConfigMapper.selectOne(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getChannelType, NoticeChannelType.WECOM)
 .eq(NoticeChannelConfigEntity::getEnabled, true)
 .orderByDesc(NoticeChannelConfigEntity::getWeight)
 .orderByDesc(NoticeChannelConfigEntity::getUpdatedAt)
 .last("limit 1"));
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "未找到已启用的企业微信渠道配置");
 }
 Map<String, Object> config = fromJson(entity.getConfigJson());
 Require.isTrue(booleanValue(config.get("loginEnabled")), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信扫码登录未启用");
 NoticeWecomLoginConfigVO vo = new NoticeWecomLoginConfigVO();
 vo.setChannelConfigId(entity.getId());
 vo.setConfigName(entity.getConfigName());
 vo.setCorpId(firstText(stringValue(config.get("corpId")), stringValue(config.get("corpID"))));
 vo.setAgentId(stringValue(config.get("agentId")));
 vo.setSecret(firstText(stringValue(config.get("secret")), stringValue(config.get("corpSecret"))));
 vo.setRedirectUri(firstText(stringValue(config.get("loginRedirectUri")), stringValue(config.get("redirectUri"))));
 Require.notBlank(vo.getCorpId(), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信 CorpId 未配置");
 Require.notBlank(vo.getAgentId(), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信 AgentId 未配置");
 Require.notBlank(vo.getSecret(), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信通讯录 Secret 未配置");
 Require.notBlank(vo.getRedirectUri(), NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信扫码登录回调地址未配置");
 return vo;
 }

 @Override
 public boolean deleteChannelConfig(Long id) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置ID不能为空");
 NoticeChannelConfigEntity entity = channelConfigMapper.selectById(id);
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置不存在");
 Require.isTrue(!isBuiltinSiteChannel(entity), NoticeCode.NOTICE_BUSINESS_ERROR, "系统消息内置通道不允许删除");
 Long templateCount = channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelConfigId, id));
 Require.isTrue(templateCount == null || templateCount == 0, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道已被消息配置引用，不能删除");
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
 case SMS -> resolveSmsConfigStatus(providerCode, config);
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

 private NoticeChannelConfigStatus resolveSmsConfigStatus(String providerCode, Map<String, Object> config) {
 if ("TENCENT_SMS".equals(providerCode)) {
 return hasAnyText(config, "secretId")
 && hasAnyText(config, "secretKey")
 && hasAnyText(config, "smsSdkAppId", "appId")
 && hasAnyText(config, "signName", "sign") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
 }
 return hasAnyText(config, "accessKeyId", "accessKey", "secretId")
 && hasAnyText(config, "accessKeySecret", "accessSecret", "secretKey")
 && hasAnyText(config, "signName", "sign") ? NoticeChannelConfigStatus.COMPLETE : NoticeChannelConfigStatus.INCOMPLETE;
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
 private NoticeBusinessTypeEntity requireBusinessType(Long businessTypeId) {
 Require.notNull(businessTypeId, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型 ID 不能为空");
 NoticeBusinessTypeEntity businessType = businessTypeMapper.selectById(businessTypeId);
 Require.notNull(businessType, NoticeCode.NOTICE_BUSINESS_ERROR, "业务类型不存在");
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
 private Integer nextTemplateVersion(String bizType, NoticeChannelType channelType) {
 List<NoticeBusinessChannelTemplateEntity> templates = channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getBizType, bizType)
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, channelType));
 return templates.stream().map(NoticeBusinessChannelTemplateEntity::getVersion).max(Integer::compareTo).orElse(0) + 1;
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
