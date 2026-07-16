package io.mango.notice.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.notice.api.command.SyncWecomUsersCommand;
import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeCode;
import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.mango.notice.api.vo.WecomUserSyncResultVO;
import io.mango.notice.channel.wecom.WecomApiException;
import io.mango.notice.channel.wecom.WecomChannelConfig;
import io.mango.notice.channel.wecom.WecomDepartment;
import io.mango.notice.channel.wecom.WecomDirectoryClient;
import io.mango.notice.channel.wecom.WecomDirectoryUser;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.entity.NoticeRecipientAccountEntity;
import io.mango.notice.core.entity.NoticeWecomSyncMappingEntity;
import io.mango.notice.core.integration.NoticeIdentityGateway;
import io.mango.notice.core.integration.NoticeOrgGateway;
import io.mango.notice.core.integration.NoticeRemoteResult;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.mapper.NoticeRecipientAccountMapper;
import io.mango.notice.core.mapper.NoticeWecomSyncMappingMapper;
import io.mango.notice.core.service.INoticeWecomSyncService;
import io.mango.org.api.command.AddOrgMemberCommand;
import io.mango.org.api.command.CreateOrgCommand;
import io.mango.org.api.command.UpdateOrgCommand;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.api.query.SysOrgTreeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeWecomSyncService implements INoticeWecomSyncService {

 private static final String WECOM_SYNC_TYPE_DEPARTMENT = "DEPARTMENT";
 private static final String WECOM_SYNC_TYPE_USER = "USER";
 private static final long WECOM_ROOT_DEPARTMENT_ID = 1L;
 private static final Integer ORG_TYPE_GROUP = 1;
 private static final Integer ORG_TYPE_COMPANY = 2;
 private static final Integer ORG_TYPE_DEPARTMENT = 3;
 private static final String INTERNAL_ORG_PARTY_TYPE = "INTERNAL_ORG";

 private final NoticeChannelConfigMapper channelConfigMapper;
 private final NoticeRecipientAccountMapper recipientAccountMapper;
 private final NoticeWecomSyncMappingMapper wecomSyncMappingMapper;
 private final NoticeIdentityGateway identityGateway;
 private final NoticeOrgGateway orgGateway;
 private final WecomDirectoryClient wecomDirectoryClient;

 @Override
 public WecomUserSyncResultVO syncWecomUsers(SyncWecomUsersCommand command) {
 Require.notNull(command, NoticeCode.NOTICE_BUSINESS_ERROR, "同步参数不能为空");
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
 Require.notNull(config, NoticeCode.NOTICE_BUSINESS_ERROR, "未找到可用企业微信渠道配置");
 WecomChannelConfig channelConfig = WecomChannelConfig.fromJson(config.getConfigJson());
 corpId = firstText(corpId, channelConfig.corpId());
 secret = firstText(secret, channelConfig.secret());
 }
 Require.notBlank(corpId, NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信 CorpId 不能为空");
 Require.notBlank(secret, NoticeCode.NOTICE_BUSINESS_ERROR, "企业微信通讯录 Secret 不能为空");
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
 if (orgGateway == null) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "组织服务不可用");
 }
 if (departments == null || departments.isEmpty()) {
 return;
 }
 Long rootOrgId = resolveTargetRootOrgId(command);
 Map<Long, NoticeWecomSyncMappingEntity> syncedMappings = new LinkedHashMap<>();
 List<WecomDepartment> pending = departments.stream()
 .filter(department -> department.id() != null)
 .sorted(Comparator.comparing(this::wecomRootDepartmentOrder)
 .thenComparing(this::wecomDepartmentParentOrder)
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

 private int wecomRootDepartmentOrder(WecomDepartment department) {
 if (department.id().equals(WECOM_ROOT_DEPARTMENT_ID)) {
 return 0;
 }
 return 1;
 }

 private Long wecomDepartmentParentOrder(WecomDepartment department) {
 if (department.parentId() == null) {
 return 0L;
 }
 return department.parentId();
 }

 private Long resolveEffectiveWecomDepartmentId(SyncWecomUsersCommand command) {
 if (command.getDepartmentId() != null) {
 return command.getDepartmentId();
 }
 if (!Boolean.TRUE.equals(command.getSyncDepartments()) && command.getTargetOrgId() != null) {
 NoticeWecomSyncMappingEntity mapping =
 findWecomSyncMappingByLocalId(WECOM_SYNC_TYPE_DEPARTMENT, command.getTargetOrgId());
 if (mapping == null || !StringUtils.hasText(mapping.getExternalId())) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "当前部门未建立企业微信部门映射，请先选择所属公司同步组织架构");
 }
 try {
 return Long.valueOf(mapping.getExternalId());
 } catch (NumberFormatException ex) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "当前部门的企业微信部门映射无效");
 }
 }
 return WECOM_ROOT_DEPARTMENT_ID;
 }

 private void validateWecomSyncTarget(SyncWecomUsersCommand command) {
 if (command.getTargetOrgType() == null) {
 return;
 }
 if (ORG_TYPE_GROUP.equals(command.getTargetOrgType())) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "集团节点不支持同步企业微信用户，请选择二级公司或已映射部门");
 }
 if (!ORG_TYPE_COMPANY.equals(command.getTargetOrgType()) && !ORG_TYPE_DEPARTMENT.equals(command.getTargetOrgType())) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "当前组织类型不支持同步企业微信用户，请选择二级公司或已映射部门");
 }
 }

 private Long resolveTargetRootOrgId(SyncWecomUsersCommand command) {
 if (command.getTargetOrgId() != null) {
 SysOrg targetOrg = getOrg(command.getTargetOrgId());
 if (targetOrg == null) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "同步目标组织不存在");
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
 NoticeRemoteResult<List<SysOrg>> response = orgGateway.tree(query);
 if (response == null || !response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "未找到Mango根组织" : response.getMsg());
 }
 return response.getData().get(0).getId();
 }

 private Long createWecomOrg(WecomDepartment department, Long parentLocalId) {
 CreateOrgCommand create = new CreateOrgCommand();
 create.setPid(parentLocalId);
 create.setOrgName(firstText(department.name(), "企业微信部门" + department.id()));
 create.setOrgCode(wecomDepartmentOrgCode(department.id()));
 create.setOrgType(3);
 Integer orgSort = department.order();
 if (orgSort == null) {
 orgSort = 0;
 }
 create.setOrgSort(orgSort);
 create.setOrgStatus("1");
 NoticeRemoteResult<Long> response = orgGateway.create(create);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "创建组织失败" : response.getMsg());
 }
 return response.getData();
 }

 private void ensureWecomUserOrgRelation(Long userId, Long orgId) {
 if (userId == null || orgId == null || identityGateway == null || orgGateway == null) {
 return;
 }
 NoticeRemoteResult<IdentityUserVO> detailResponse = identityGateway.detail(userId);
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
 NoticeRemoteResult<Void> response = orgGateway.addMember(orgId, addMember);
 if (response == null || !response.isSuccess()) {
 String message = response == null ? "加入组织失败" : response.getMsg();
 if (!alreadyExistsMessage(message)) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, message);
 }
 }
 } catch (RuntimeException ex) {
 if (!alreadyExistsMessage(ex.getMessage())) {
 Require.rethrow(ex);
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
 Integer orgType = existing.getOrgType();
 if (orgType == null) {
 orgType = 3;
 }
 Integer orgSort = department.order();
 if (orgSort == null) {
 orgSort = existing.getOrgSort();
 }
 update.setOrgType(orgType);
 update.setOrgSort(orgSort);
 update.setOrgStatus(firstText(existing.getOrgStatus(), "1"));
 NoticeRemoteResult<Void> response = orgGateway.update(update);
 if (response == null || !response.isSuccess()) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "更新组织失败" : response.getMsg());
 }
 }

 private SysOrg getOrg(Long orgId) {
 if (orgId == null) {
 return null;
 }
 try {
 NoticeRemoteResult<SysOrg> response = orgGateway.getById(orgId);
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
 NoticeRemoteResult<?> response = identityGateway.bindExternalIdentity(bind);
 if (response == null || !response.isSuccess()) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "绑定企微登录身份失败" : response.getMsg());
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
 NoticeRemoteResult<PageResult<IdentityUserVO>> response = identityGateway.page(query);
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
 NoticeRemoteResult<IdentityUserVO> detailResponse = identityGateway.detail(userId);
 if (detailResponse != null && detailResponse.isSuccess() && detailResponse.getData() != null) {
 return detailResponse.getData();
 }
 NoticeRemoteResult<IdentityUserInfoVO> response = identityGateway.getUserInfoById(userId);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return null;
 }
 IdentityUserInfoVO info = response.getData();
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
 NoticeRemoteResult<Long> response = identityGateway.create(create);
 if (response == null || !response.isSuccess() || response.getData() == null) {
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "创建成员失败" : response.getMsg());
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
 NoticeRemoteResult<Boolean> response = identityGateway.update(update);
 if (response == null || !response.isSuccess()) {
 Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR,
 response == null ? "更新成员失败" : response.getMsg());
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
 return Require.fail(NoticeCode.NOTICE_BUSINESS_ERROR, "同步数据指纹计算失败", ex);
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
 private void clearDefaultAccount(Long userId, NoticeRecipientAccountType accountType) {
 NoticeRecipientAccountEntity update = new NoticeRecipientAccountEntity();
 update.setDefaultAccount(false);
 recipientAccountMapper.update(update, new LambdaQueryWrapper<NoticeRecipientAccountEntity>()
 .eq(NoticeRecipientAccountEntity::getUserId, userId)
 .eq(NoticeRecipientAccountEntity::getAccountType, accountType)
 .eq(NoticeRecipientAccountEntity::getDefaultAccount, true));
 }

 private String firstText(String first, String second) {
 return StringUtils.hasText(first) ? first : second;
 }
}
