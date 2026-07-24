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
import io.mango.notice.api.command.SaveNoticeRouteTagCommand;
import io.mango.notice.api.command.UpdateNoticeBusinessTypeCommand;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelRouteMode;
import io.mango.notice.api.enums.NoticeChannelSecretStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSyncStatus;
import io.mango.notice.api.enums.NoticeTaskStatus;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.mango.notice.api.query.NoticeBusinessTypePageQuery;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeChannelReferenceImpactQuery;
import io.mango.notice.api.query.NoticeRouteTagQuery;
import io.mango.notice.api.vo.NoticeBusinessConfigVersionVO;
import io.mango.notice.api.vo.NoticeBusinessTypeVO;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.api.vo.NoticeChannelTemplateVO;
import io.mango.notice.api.vo.NoticeChannelReferenceImpactVO;
import io.mango.notice.api.vo.NoticeRouteTagVO;
import io.mango.notice.api.vo.NoticeWecomLoginConfigVO;
import io.mango.notice.core.convert.NoticeBusinessConfigVersionConvert;
import io.mango.notice.core.convert.NoticeBusinessTypeConvert;
import io.mango.notice.core.convert.NoticeChannelConfigConvert;
import io.mango.notice.core.convert.NoticeChannelTemplateConvert;
import io.mango.notice.core.entity.NoticeBusinessChannelTemplateEntity;
import io.mango.notice.core.entity.NoticeBusinessConfigVersionEntity;
import io.mango.notice.core.entity.NoticeBusinessTypeEntity;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeChannelConfigRouteTagEntity;
import io.mango.notice.core.entity.NoticeChannelRouteTagEntity;
import io.mango.notice.core.entity.NoticeTaskEntity;
import io.mango.notice.core.mapper.NoticeBusinessChannelTemplateMapper;
import io.mango.notice.core.mapper.NoticeBusinessConfigVersionMapper;
import io.mango.notice.core.mapper.NoticeBusinessTypeMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeChannelConfigRouteTagMapper;
import io.mango.notice.core.mapper.NoticeChannelRouteTagMapper;
import io.mango.notice.core.mapper.NoticeTaskMapper;
import io.mango.notice.core.service.INoticeConfigurationService;
import io.mango.notice.core.service.NoticeChannelSecretMaterializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeConfigurationService implements INoticeConfigurationService {

 private static final String MASKED_VALUE = "***";
 private static final String SITE_INTERNAL_PROVIDER = "INTERNAL";
 private static final Set<String> SENSITIVE_CONFIG_KEYS = Set.of("secret", "password", "token", "appsecret",
         "accesskeysecret", "accesssecret", "secretkey", "smtppassword", "corpsecret");

 private final NoticeBusinessTypeMapper businessTypeMapper;
 private final NoticeBusinessConfigVersionMapper businessConfigVersionMapper;
 private final NoticeBusinessChannelTemplateMapper channelTemplateMapper;
 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeChannelRouteTagMapper routeTagMapper;
 private final NoticeChannelConfigRouteTagMapper configRouteTagMapper;
 private final NoticeTaskMapper taskMapper;
 private final ObjectMapper objectMapper;
 private final NoticeChannelSecretMaterializer secretMaterializer;

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
 NoticeChannelRouteMode routeMode = command.getRouteMode() == null
 ? (command.getChannelConfigId() == null ? NoticeChannelRouteMode.AUTO : NoticeChannelRouteMode.EXACT)
 : command.getRouteMode();
 validateRouteSelection(channelType, routeMode, command.getChannelConfigId(), command.getRouteTagCode());
 draft.setRouteMode(routeMode);
 draft.setChannelConfigId(routeMode == NoticeChannelRouteMode.EXACT ? command.getChannelConfigId() : null);
 draft.setRouteTagCode(routeMode == NoticeChannelRouteMode.TAG
 ? command.getRouteTagCode().trim().toUpperCase(Locale.ROOT) : null);
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
 if (StringUtils.hasText(query.getConfigCode())) {
 wrapper.like(NoticeChannelConfigEntity::getConfigCode, query.getConfigCode().trim());
 }
 if (StringUtils.hasText(query.getResourceSource())) {
 wrapper.eq(NoticeChannelConfigEntity::getResourceSource, query.getResourceSource().trim().toUpperCase(Locale.ROOT));
 }
 if (query.getSecretStatus() != null) {
 wrapper.eq(NoticeChannelConfigEntity::getSecretStatus, query.getSecretStatus());
 }
 wrapper.orderByDesc(NoticeChannelConfigEntity::getUpdatedAt);
 Page<NoticeChannelConfigEntity> result = channelConfigMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
 return PageResult.of(result.getRecords().stream().map(this::toChannelConfigVO).toList(), result.getTotal(),
 result.getCurrent(), result.getSize());
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public NoticeChannelConfigVO saveChannelConfig(SaveNoticeChannelConfigCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置不能为空");
 Require.notNull(command.getChannelType(), NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
 NoticeChannelConfigEntity entity = command.getId() == null ? new NoticeChannelConfigEntity() : channelConfigMapper.selectById(command.getId());
 Require.notNull(entity, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置不存在");
 boolean resourceManaged = entity.getId() != null && "RESOURCE".equals(entity.getResourceSource());
 if (entity.getId() == null) {
 entity.setConfigCode(resolveNewConfigCode(command.getConfigCode()));
 entity.setResourceSource("MANUAL");
 } else if (StringUtils.hasText(command.getConfigCode())) {
 Require.isTrue(command.getConfigCode().trim().equals(entity.getConfigCode()), NoticeCode.NOTICE_BUSINESS_ERROR,
 "渠道配置稳定编码创建后不可修改");
 }
 Map<String, Object> legacyConfig = new LinkedHashMap<>(fromJson(entity.getConfigJson()));
 Map<String, Object> legacySecrets = new LinkedHashMap<>();
 extractSecrets(legacyConfig, legacySecrets);
 Map<String, Object> submittedConfig = new LinkedHashMap<>(fromJson(command.getConfigJson()));
 Map<String, Object> extractedSecrets = new LinkedHashMap<>();
 extractSecrets(submittedConfig, extractedSecrets);
 if (!resourceManaged && entity.getId() != null && Boolean.TRUE.equals(entity.getEnabled())
         && Boolean.FALSE.equals(command.getEnabled())) {
     Require.isTrue(referencingTemplatesForConfig(entity.getId()).isEmpty(), NoticeCode.NOTICE_BUSINESS_ERROR,
             "渠道已被消息配置引用，不能停用");
 }
 if (command.getRouteTagCodes() != null && entity.getId() != null) {
     validateRouteTagRemovals(entity, command.getRouteTagCodes());
 }
 if (!resourceManaged) {
 entity.setChannelType(command.getChannelType());
 entity.setProviderCode(command.getProviderCode());
 entity.setConfigName(command.getConfigName());
 entity.setConfigJson(toJson(submittedConfig));
 entity.setEnabled(command.getEnabled() == null ? Boolean.TRUE : command.getEnabled());
 entity.setPriority(command.getPriority() == null ? 0 : command.getPriority());
 entity.setRateLimitConfig(command.getRateLimitConfig());
 }
 Map<String, Object> secrets = new LinkedHashMap<>(fromJson(entity.getSecretConfigJson()));
 legacySecrets.forEach(secrets::putIfAbsent);
 secrets.putAll(extractedSecrets);
 mergeSecretValues(secrets, command.getSecretValues());
 entity.setSecretConfigJson(secrets.isEmpty() ? null : toJson(secrets));
 Integer weight = command.getWeight();
 if (weight == null || weight <= 0) {
 weight = 100;
 }
 if (!resourceManaged) {
 entity.setWeight(weight);
 }
 Map<String, Object> effectiveConfig = effectiveConfig(entity);
 entity.setSecretStatus(resolveSecretStatus(entity.getChannelType(), entity.getProviderCode(), effectiveConfig));
 entity.setConfigStatus(resolveConfigStatus(entity.getChannelType(), entity.getProviderCode(),
 toJson(effectiveConfig)));
 if (entity.getLastSendStatus() == null) {
 entity.setLastSendStatus(NoticeChannelSendHealthStatus.NONE);
 }
 if (entity.getId() == null) {
 channelConfigMapper.insert(entity);
 } else {
 channelConfigMapper.updateById(entity);
 }
 if (command.getRouteTagCodes() != null) {
 replaceConfigRouteTags(entity, command.getRouteTagCodes());
 }
 return toChannelConfigVO(entity);
 }

 @Override
 public List<NoticeRouteTagVO> listRouteTags(NoticeRouteTagQuery query) {
 LambdaQueryWrapper<NoticeChannelRouteTagEntity> wrapper = new LambdaQueryWrapper<>();
 if (query != null && query.getChannelType() != null) {
 wrapper.eq(NoticeChannelRouteTagEntity::getChannelType, query.getChannelType());
 }
 if (query != null && StringUtils.hasText(query.getKeyword())) {
 wrapper.and(item -> item.like(NoticeChannelRouteTagEntity::getTagCode, query.getKeyword().trim())
 .or().like(NoticeChannelRouteTagEntity::getTagName, query.getKeyword().trim()));
 }
 wrapper.orderByAsc(NoticeChannelRouteTagEntity::getChannelType)
 .orderByAsc(NoticeChannelRouteTagEntity::getTagCode);
 return routeTagMapper.selectList(wrapper).stream().map(this::toRouteTagVO).toList();
 }

 @Override
 public NoticeRouteTagVO saveRouteTag(SaveNoticeRouteTagCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签不能为空");
 Require.notNull(command.getChannelType(), NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
 Require.notBlank(command.getTagCode(), NoticeCode.NOTICE_BUSINESS_ERROR, "标签编码不能为空");
 Require.notBlank(command.getTagName(), NoticeCode.NOTICE_BUSINESS_ERROR, "标签名称不能为空");
 String tagCode = command.getTagCode().trim().toUpperCase(Locale.ROOT);
 NoticeChannelRouteTagEntity entity = command.getId() == null ? null : routeTagMapper.selectById(command.getId());
 if (entity == null && command.getId() == null) {
 entity = routeTagMapper.selectOne(new LambdaQueryWrapper<NoticeChannelRouteTagEntity>()
 .eq(NoticeChannelRouteTagEntity::getChannelType, command.getChannelType())
 .eq(NoticeChannelRouteTagEntity::getTagCode, tagCode).last("limit 1"));
 }
 if (entity == null) {
 entity = new NoticeChannelRouteTagEntity();
 entity.setChannelType(command.getChannelType());
 entity.setTagCode(tagCode);
 } else {
 Require.isTrue(entity.getChannelType() == command.getChannelType() && entity.getTagCode().equals(tagCode),
 NoticeCode.NOTICE_BUSINESS_ERROR, "标签编码和渠道类型创建后不可修改");
 }
 entity.setTagName(command.getTagName().trim());
 entity.setDescription(command.getDescription());
 if (entity.getId() == null) {
 routeTagMapper.insert(entity);
 } else {
 routeTagMapper.updateById(entity);
 }
 return toRouteTagVO(entity);
 }

 @Override
 @Transactional(rollbackFor = Exception.class)
 public boolean deleteRouteTag(Long id) {
 Require.notNull(id, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签 ID 不能为空");
 NoticeChannelRouteTagEntity tag = routeTagMapper.selectById(id);
 Require.notNull(tag, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签不存在");
 Long templateCount = channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, tag.getChannelType())
 .eq(NoticeBusinessChannelTemplateEntity::getRouteMode, NoticeChannelRouteMode.TAG)
 .eq(NoticeBusinessChannelTemplateEntity::getRouteTagCode, tag.getTagCode()));
 Require.isTrue(templateCount == null || templateCount == 0, NoticeCode.NOTICE_BUSINESS_ERROR,
 "路由标签已被消息配置引用，不能删除");
 configRouteTagMapper.delete(new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getRouteTagId, id));
 return routeTagMapper.deleteById(id) > 0;
 }

 @Override
 public NoticeChannelReferenceImpactVO getChannelReferenceImpact(NoticeChannelReferenceImpactQuery query) {
 Require.notNull(query, NoticeCode.NOTICE_BUSINESS_ERROR, "引用影响查询不能为空");
 List<NoticeBusinessChannelTemplateEntity> templates;
 if (query.getConfigId() != null) {
 templates = referencingTemplatesForConfig(query.getConfigId());
 } else {
 Require.notNull(query.getChannelType(), NoticeCode.NOTICE_BUSINESS_ERROR, "渠道类型不能为空");
 Require.notBlank(query.getRouteTagCode(), NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签编码不能为空");
 templates = channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, query.getChannelType())
 .eq(NoticeBusinessChannelTemplateEntity::getRouteMode, NoticeChannelRouteMode.TAG)
 .eq(NoticeBusinessChannelTemplateEntity::getRouteTagCode,
         query.getRouteTagCode().trim().toUpperCase(Locale.ROOT))
 .orderByAsc(NoticeBusinessChannelTemplateEntity::getBizType)
 .orderByAsc(NoticeBusinessChannelTemplateEntity::getTemplateName));
 }
 NoticeChannelReferenceImpactVO impact = new NoticeChannelReferenceImpactVO();
 impact.setReferenceCount(templates.size());
 impact.setBusinessTemplateNames(templates.stream()
 .map(template -> template.getBizType() + " / " + firstText(template.getTemplateName(),
 template.getChannelType().name())).distinct().toList());
 return impact;
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
 Map<String, Object> config = fromJson(secretMaterializer.materialize(entity));
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
 Require.isTrue(referencingTemplatesForConfig(id).isEmpty(), NoticeCode.NOTICE_BUSINESS_ERROR,
 "渠道已被消息配置引用，不能删除");
 configRouteTagMapper.delete(new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, id));
 return channelConfigMapper.deleteById(id) > 0;
 }

 private void validateRouteSelection(NoticeChannelType channelType, NoticeChannelRouteMode routeMode,
 Long channelConfigId, String routeTagCode) {
 if (routeMode == NoticeChannelRouteMode.EXACT) {
 Require.notNull(channelConfigId, NoticeCode.NOTICE_BUSINESS_ERROR, "EXACT 模式必须选择渠道账号");
 Require.isTrue(!StringUtils.hasText(routeTagCode), NoticeCode.NOTICE_BUSINESS_ERROR,
 "EXACT 模式不能同时选择路由标签");
 NoticeChannelConfigEntity config = channelConfigMapper.selectById(channelConfigId);
 Require.notNull(config, NoticeCode.NOTICE_BUSINESS_ERROR, "所选渠道账号不存在");
 Require.isTrue(config.getChannelType() == channelType, NoticeCode.NOTICE_BUSINESS_ERROR,
 "所选渠道账号与模板渠道类型不一致");
 Require.isTrue(Boolean.TRUE.equals(config.getEnabled()), NoticeCode.NOTICE_BUSINESS_ERROR,
 "所选渠道账号未启用");
 Require.isTrue(config.getConfigStatus() == NoticeChannelConfigStatus.COMPLETE,
 NoticeCode.NOTICE_BUSINESS_ERROR, "所选渠道账号配置不完整");
 return;
 }
 Require.isTrue(channelConfigId == null, NoticeCode.NOTICE_BUSINESS_ERROR,
 routeMode + " 模式不能同时选择精确渠道账号");
 if (routeMode == NoticeChannelRouteMode.TAG) {
 Require.notBlank(routeTagCode, NoticeCode.NOTICE_BUSINESS_ERROR, "TAG 模式必须选择路由标签");
 NoticeChannelRouteTagEntity tag = findRouteTag(channelType, routeTagCode);
 Require.notNull(tag, NoticeCode.NOTICE_BUSINESS_ERROR, "所选路由标签不存在");
 } else {
 Require.isTrue(!StringUtils.hasText(routeTagCode), NoticeCode.NOTICE_BUSINESS_ERROR,
 "AUTO 模式不能同时选择路由标签");
 }
 }

 private void validateRouteTagRemovals(NoticeChannelConfigEntity config, List<String> requestedCodes) {
 Set<String> requested = requestedCodes.stream().filter(StringUtils::hasText)
 .map(code -> code.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
 List<NoticeChannelConfigRouteTagEntity> relations = configRouteTagMapper.selectList(
 new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, config.getId()));
 if (relations.isEmpty()) {
 return;
 }
 List<NoticeChannelRouteTagEntity> removedTags = routeTagMapper.selectBatchIds(relations.stream()
 .map(NoticeChannelConfigRouteTagEntity::getRouteTagId).toList()).stream()
 .filter(tag -> !requested.contains(tag.getTagCode())).toList();
 for (NoticeChannelRouteTagEntity tag : removedTags) {
 Long references = channelTemplateMapper.selectCount(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, config.getChannelType())
 .eq(NoticeBusinessChannelTemplateEntity::getRouteMode, NoticeChannelRouteMode.TAG)
 .eq(NoticeBusinessChannelTemplateEntity::getRouteTagCode, tag.getTagCode()));
 Require.isTrue(references == null || references == 0, NoticeCode.NOTICE_BUSINESS_ERROR,
 "路由标签“" + tag.getTagName() + "”已被消息配置引用，不能从账号移除");
 }
 }

 private List<NoticeBusinessChannelTemplateEntity> referencingTemplatesForConfig(Long configId) {
 NoticeChannelConfigEntity config = channelConfigMapper.selectById(configId);
 if (config == null) {
 return List.of();
 }
 Map<Long, NoticeBusinessChannelTemplateEntity> references = new LinkedHashMap<>();
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelConfigId, configId))
 .forEach(template -> references.put(template.getId(), template));
 List<NoticeChannelConfigRouteTagEntity> relations = configRouteTagMapper.selectList(
 new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, configId));
 if (!relations.isEmpty()) {
 List<String> tagCodes = routeTagMapper.selectBatchIds(relations.stream()
 .map(NoticeChannelConfigRouteTagEntity::getRouteTagId).toList()).stream()
 .map(NoticeChannelRouteTagEntity::getTagCode).toList();
 if (!tagCodes.isEmpty()) {
 channelTemplateMapper.selectList(new LambdaQueryWrapper<NoticeBusinessChannelTemplateEntity>()
 .eq(NoticeBusinessChannelTemplateEntity::getChannelType, config.getChannelType())
 .eq(NoticeBusinessChannelTemplateEntity::getRouteMode, NoticeChannelRouteMode.TAG)
 .in(NoticeBusinessChannelTemplateEntity::getRouteTagCode, tagCodes))
 .forEach(template -> references.put(template.getId(), template));
 }
 }
 return references.values().stream()
 .sorted(Comparator.comparing(NoticeBusinessChannelTemplateEntity::getBizType,
 Comparator.nullsLast(String::compareTo))
 .thenComparing(NoticeBusinessChannelTemplateEntity::getTemplateName,
 Comparator.nullsLast(String::compareTo)))
 .toList();
 }

 private String resolveNewConfigCode(String configCode) {
 String resolved = StringUtils.hasText(configCode) ? configCode.trim().toUpperCase(Locale.ROOT)
 : "MANUAL_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
 Long count = channelConfigMapper.selectCount(new LambdaQueryWrapper<NoticeChannelConfigEntity>()
 .eq(NoticeChannelConfigEntity::getConfigCode, resolved));
 Require.isTrue(count == null || count == 0, NoticeCode.NOTICE_BUSINESS_ERROR, "渠道配置稳定编码已存在");
 return resolved;
 }

 private void extractSecrets(Map<String, Object> config, Map<String, Object> secrets) {
 List<String> keys = new ArrayList<>(config.keySet());
 for (String key : keys) {
 Object value = config.get(key);
 if (isSensitiveConfigKey(key)) {
 config.remove(key);
 if (value != null && !MASKED_VALUE.equals(String.valueOf(value))) {
 secrets.put(key, value);
 }
 } else if (value instanceof Map<?, ?> nested) {
 Map<String, Object> nestedConfig = new LinkedHashMap<>((Map<String, Object>) nested);
 Map<String, Object> nestedSecrets = new LinkedHashMap<>();
 extractSecrets(nestedConfig, nestedSecrets);
 config.put(key, nestedConfig);
 if (!nestedSecrets.isEmpty()) {
 secrets.put(key, nestedSecrets);
 }
 }
 }
 }

 private void mergeSecretValues(Map<String, Object> target, Map<String, String> values) {
 if (values == null) {
 return;
 }
 values.forEach((key, value) -> {
 if (StringUtils.hasText(key) && StringUtils.hasText(value) && !MASKED_VALUE.equals(value)) {
 target.put(key.trim(), value);
 }
 });
 }

 private Map<String, Object> effectiveConfig(NoticeChannelConfigEntity entity) {
 Map<String, Object> effective = new LinkedHashMap<>(fromJson(entity.getConfigJson()));
 Map<String, Object> manualSecrets = fromJson(entity.getSecretConfigJson());
 manualSecrets.forEach(effective::putIfAbsent);
 fromJson(entity.getSecretRefsJson()).keySet().forEach(key -> effective.put(key, "referenced"));
 return effective;
 }

 private NoticeChannelSecretStatus resolveSecretStatus(NoticeChannelType channelType, String providerCode,
 Map<String, Object> effectiveConfig) {
 if (channelType == NoticeChannelType.SITE) {
 return NoticeChannelSecretStatus.NOT_REQUIRED;
 }
 return missingSecretKeys(channelType, providerCode, effectiveConfig).isEmpty()
 ? NoticeChannelSecretStatus.COMPLETE : NoticeChannelSecretStatus.INCOMPLETE;
 }

 private List<String> missingSecretKeys(NoticeChannelType channelType, String providerCode,
 Map<String, Object> config) {
 List<String> missing = new ArrayList<>();
 switch (channelType) {
 case SITE -> { }
 case EMAIL -> {
 if ("ALIYUN_DM".equals(providerCode)) {
 if (!hasAnyText(config, "accessKeySecret", "accessSecret")) {
 missing.add("accessKeySecret");
 }
 } else if (!hasAnyText(config, "password", "smtpPassword")) {
 missing.add("password");
 }
 }
 case SMS -> {
 if ("TENCENT_SMS".equals(providerCode)) {
 if (!hasAnyText(config, "secretKey")) {
 missing.add("secretKey");
 }
 } else if (!hasAnyText(config, "accessKeySecret", "accessSecret")) {
 missing.add("accessKeySecret");
 }
 }
 case WECHAT_OFFICIAL, DINGTALK -> {
 if (!hasAnyText(config, "appSecret", "secret", "webhookUrl")) {
 missing.add("appSecret");
 }
 }
 case WECOM -> {
 if (!hasAnyText(config, "secret", "corpSecret", "webhookUrl")) {
 missing.add("secret");
 }
 }
 }
 return missing;
 }

 private void replaceConfigRouteTags(NoticeChannelConfigEntity config, List<String> routeTagCodes) {
 configRouteTagMapper.delete(new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, config.getId()));
 if (routeTagCodes == null) {
 return;
 }
 for (String code : new LinkedHashSet<>(routeTagCodes)) {
 if (!StringUtils.hasText(code)) {
 continue;
 }
 NoticeChannelRouteTagEntity tag = findRouteTag(config.getChannelType(), code);
 Require.notNull(tag, NoticeCode.NOTICE_BUSINESS_ERROR, "路由标签不存在：" + code);
 NoticeChannelConfigRouteTagEntity relation = new NoticeChannelConfigRouteTagEntity();
 relation.setChannelConfigId(config.getId());
 relation.setRouteTagId(tag.getId());
 configRouteTagMapper.insert(relation);
 }
 }

 private NoticeChannelRouteTagEntity findRouteTag(NoticeChannelType channelType, String routeTagCode) {
 return routeTagMapper.selectOne(new LambdaQueryWrapper<NoticeChannelRouteTagEntity>()
 .eq(NoticeChannelRouteTagEntity::getChannelType, channelType)
 .eq(NoticeChannelRouteTagEntity::getTagCode, routeTagCode.trim().toUpperCase(Locale.ROOT))
 .last("limit 1"));
 }

 private NoticeChannelConfigVO toChannelConfigVO(NoticeChannelConfigEntity entity) {
 NoticeChannelConfigVO vo = NoticeChannelConfigConvert.toVO(entity);
 Map<String, Object> effective = effectiveConfig(entity);
 vo.setMissingSecretKeys(missingSecretKeys(entity.getChannelType(), entity.getProviderCode(), effective));
 List<NoticeChannelConfigRouteTagEntity> relations = configRouteTagMapper.selectList(
 new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getChannelConfigId, entity.getId()));
 if (relations.isEmpty()) {
 vo.setRouteTagCodes(List.of());
 } else {
 List<Long> tagIds = relations.stream().map(NoticeChannelConfigRouteTagEntity::getRouteTagId).toList();
 vo.setRouteTagCodes(routeTagMapper.selectBatchIds(tagIds).stream()
 .map(NoticeChannelRouteTagEntity::getTagCode).sorted().toList());
 }
 return vo;
 }

 private NoticeRouteTagVO toRouteTagVO(NoticeChannelRouteTagEntity entity) {
 NoticeRouteTagVO vo = new NoticeRouteTagVO();
 vo.setId(entity.getId());
 vo.setChannelType(entity.getChannelType());
 vo.setTagCode(entity.getTagCode());
 vo.setTagName(entity.getTagName());
 vo.setDescription(entity.getDescription());
 List<NoticeChannelConfigRouteTagEntity> relations = configRouteTagMapper.selectList(
 new LambdaQueryWrapper<NoticeChannelConfigRouteTagEntity>()
 .eq(NoticeChannelConfigRouteTagEntity::getRouteTagId, entity.getId()));
 List<NoticeChannelConfigEntity> configs = relations.isEmpty() ? List.of()
 : channelConfigMapper.selectBatchIds(relations.stream()
 .map(NoticeChannelConfigRouteTagEntity::getChannelConfigId).toList());
 vo.setCandidateCount(configs.size());
 vo.setCandidateConfigNames(configs.stream().map(NoticeChannelConfigEntity::getConfigName).sorted().toList());
 return vo;
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
 String normalized = key.toLowerCase(Locale.ROOT);
 return SENSITIVE_CONFIG_KEYS.contains(normalized)
 || normalized.endsWith("password") || normalized.endsWith("token");
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
 activated.setRouteMode(source.getRouteMode());
 activated.setRouteTagCode(source.getRouteTagCode());
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
