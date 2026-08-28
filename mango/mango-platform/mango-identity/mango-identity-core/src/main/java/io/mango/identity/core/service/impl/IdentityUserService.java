package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.identity.core.adapter.AuthorizationRoleBindingAdapter;
import io.mango.common.vo.PageResult;
import io.mango.common.result.Require;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.RequireIdentityUserPasswordResetCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.command.UnlockIdentityUserCommand;
import io.mango.identity.api.command.SendContactCaptchaCommand;
import io.mango.identity.api.command.UpdateCurrentUserContactCommand;
import io.mango.identity.api.command.UpdateCurrentUserProfileCommand;
import io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand;
import io.mango.identity.api.enums.IdentityCode;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserBatchQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.api.vo.ContactCaptchaTicketVO;
import io.mango.identity.api.vo.CurrentUserProfileVO;
import io.mango.identity.core.entity.ExternalIdentityBindingEntity;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.ExternalIdentityBindingMapper;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.identity.core.service.IIdentityUserService;
import io.mango.identity.core.service.IIdentityPasswordPolicyService;
import io.mango.identity.core.service.IIdentitySecurityPolicyService;
import io.mango.identity.core.service.IIdentityUserSecurityService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudServiceImpl;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.notice.api.command.NoticeSiteMessageActionCommand;
import io.mango.notice.api.command.NoticeSiteMessageSubjectCommand;
import io.mango.notice.api.command.NoticeSiteMessageTargetCommand;
import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.command.NoticeJsonRequest;
import io.mango.notice.api.command.NoticeSendEventCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 身份用户服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserService extends MangoCrudServiceImpl<IdentityUserMapper, IdentityUserEntity>
        implements IIdentityUserService {

    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_PARTY_TYPE = "INTERNAL_ORG";
    private static final String DEFAULT_INITIAL_PASSWORD = "Mango@123456";
    private static final String STATUS_BOUND = "BOUND";
    private static final String VERIFICATION_UNVERIFIED = "UNVERIFIED";
    private static final String DEFAULT_APP_CODE = "internal-admin";
    private static final long CONTACT_CAPTCHA_TTL_SECONDS = 300L;
    private static final int MASK_SUFFIX_LENGTH = 4;
    private static final int MASK_PHONE_PREFIX_LENGTH = 3;

    private final IdentityUserMapper identityUserMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberOrgMapper tenantMemberOrgMapper;
    private final AuthorizationRoleBindingAdapter roleBindingAdapter;
    private final ExternalIdentityBindingMapper externalIdentityBindingMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final IIdentityPasswordPolicyService passwordPolicyService;
    private final IIdentitySecurityPolicyService securityPolicyService;
    private final IIdentityUserSecurityService identityUserSecurityService;
    private final ObjectProvider<CaptchaApi> captchaApiProvider;

    @Override
    public CurrentUserProfileVO currentProfile() {
        return toCurrentProfile(currentUser());
    }

    @Override
    @Transactional
    public CurrentUserProfileVO updateCurrentProfile(UpdateCurrentUserProfileCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "当前用户资料命令不能为空");
        IdentityUserEntity user = currentUser();
        String documentType = trimToNull(command.getDocumentType());
        String documentNumber = trimToNull(command.getDocumentNumber());
        if (documentType != null && documentNumber == null
                && documentType.equals(user.getDocumentType())
                && StringUtils.hasText(user.getDocumentNumber())) {
            documentNumber = user.getDocumentNumber();
        }
        Require.isTrue((documentType == null) == (documentNumber == null), IdentityCode.VALIDATION_ERROR,
                "证件类型和证件号码必须同时填写或同时清空");
        user.setNickname(trimToNull(command.getNickname()));
        user.setAvatar(trimToNull(command.getAvatar()));
        user.setRealName(trimToNull(command.getRealName()));
        user.setDocumentType(documentType);
        user.setDocumentNumber(documentNumber);
        if (!StringUtils.hasText(user.getVerificationStatus())) {
            user.setVerificationStatus(VERIFICATION_UNVERIFIED);
        }
        user.setUpdateTime(LocalDateTime.now());
        LambdaUpdateWrapper<IdentityUserEntity> update = new LambdaUpdateWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getId, user.getId())
                .set(IdentityUserEntity::getNickname, user.getNickname())
                .set(IdentityUserEntity::getAvatar, user.getAvatar())
                .set(IdentityUserEntity::getRealName, user.getRealName())
                .set(IdentityUserEntity::getDocumentType, user.getDocumentType())
                .set(IdentityUserEntity::getDocumentNumber, user.getDocumentNumber())
                .set(IdentityUserEntity::getVerificationStatus, user.getVerificationStatus())
                .set(IdentityUserEntity::getUpdateTime, user.getUpdateTime());
        Require.isTrue(identityUserMapper.update(null, update) > 0, IdentityCode.CONFLICT,
                "当前用户资料更新失败");
        return toCurrentProfile(user);
    }

    @Override
    public ContactCaptchaTicketVO sendCurrentContactCaptcha(SendContactCaptchaCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "联系方式验证码命令不能为空");
        IdentityUserEntity user = currentUser();
        Contact contact = validateContact(command.getContactType(), command.getTarget());
        Require.isTrue(!contact.matches(user), IdentityCode.CONFLICT, "新联系方式不能与当前值相同");
        requireContactAvailable(user.getUserId(), contact);
        CaptchaApi captchaApi = Require.nonNull(captchaApiProvider.getIfAvailable(),
                IdentityCode.CONTACT_CAPTCHA_UNAVAILABLE);
        CaptchaSendRequest request = new CaptchaSendRequest();
        request.setType(contact.captchaType());
        request.setTarget(contact.value());
        request.setBusinessType(contact.businessType());
        request.setExpireSeconds(CONTACT_CAPTCHA_TTL_SECONDS);
        String key = IdentityCaptchaResponse.requireSentKey(captchaApi.send(request));
        if (key.startsWith("captcha:")) {
            key = key.substring("captcha:".length());
        }
        return new ContactCaptchaTicketVO(key, maskContact(contact), CONTACT_CAPTCHA_TTL_SECONDS);
    }

    @Override
    @Transactional
    public CurrentUserProfileVO updateCurrentContact(UpdateCurrentUserContactCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "联系方式更新命令不能为空");
        IdentityUserEntity user = currentUser();
        Require.isTrue(passwordEncoder.matches(command.getCurrentPassword(), user.getPassword()),
                IdentityCode.CURRENT_PASSWORD_INVALID);
        Contact contact = validateContact(command.getContactType(), command.getTarget());
        Require.isTrue(!contact.matches(user), IdentityCode.CONFLICT, "新联系方式不能与当前值相同");
        requireContactAvailable(user.getUserId(), contact);
        String expectedKey = contact.businessType() + ":" + contact.value();
        Require.isTrue(expectedKey.equals(command.getCaptchaKey()), IdentityCode.CONTACT_CAPTCHA_INVALID);
        CaptchaApi captchaApi = Require.nonNull(captchaApiProvider.getIfAvailable(),
                IdentityCode.CONTACT_CAPTCHA_UNAVAILABLE);
        CaptchaVerifyRequest request = new CaptchaVerifyRequest();
        request.setKey(expectedKey);
        request.setType(contact.captchaType());
        request.setCode(command.getCaptchaCode());
        IdentityCaptchaResponse.requireVerified(captchaApi.verify(request));
        if (contact.type() == ContactType.PHONE) {
            user.setPhone(contact.value());
        } else {
            user.setEmail(contact.value());
        }
        user.setUpdateTime(LocalDateTime.now());
        Require.isTrue(identityUserMapper.updateById(user) > 0, IdentityCode.CONFLICT, "联系方式更新失败");
        return toCurrentProfile(user);
    }

    @Override
    public PageResult<IdentityUserVO> pageResult(IdentityUserPageQuery query) {
        LambdaQueryWrapper<IdentityUserEntity> wrapper = buildManageableUserWrapper(query);
        IPage<IdentityUserEntity> page = identityUserMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        List<IdentityUserVO> list = page.getRecords().stream()
                .map(user -> toVO(user, query.getOrgId()))
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PersistencePageResult<IdentityUserVO> page(IdentityUserPageQuery query) {
        PageResult<IdentityUserVO> result = pageResult(query);
        return PersistencePageResult.of(result.getList(), result.getTotal(), result.getPage(), result.getSize());
    }

    @Override
    public IdentityUserVO detail(Long userId) {
        IdentityUserEntity user = getManageableUser(userId);
        if (user == null) {
            return null;
        }
        return toVO(user, null);
    }

    @Override
    @Transactional
    public Long create(CreateIdentityUserCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "身份用户创建命令不能为空");
        String realm = firstText(command.getRealm(), DEFAULT_REALM);
        IdentityUserEntity existing = getByUsername(command.getUsername(), realm);
        Require.isTrue(existing == null, IdentityCode.CONFLICT, "用户名已存在");
        IdentityUserEntity user = new IdentityUserEntity();
        String plainPassword = firstText(command.getPassword(), DEFAULT_INITIAL_PASSWORD);
        passwordPolicyService.validatePlainPassword(plainPassword);
        LocalDateTime now = LocalDateTime.now();
        user.setUsername(command.getUsername().trim());
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setPasswordResetRequired(securityPolicyService.resetRequiredAfterCreate());
        user.setPasswordUpdatedAt(now);
        user.setNickname(command.getNickname());
        user.setRealm(realm);
        user.setActorType(firstText(command.getActorType(), DEFAULT_ACTOR_TYPE));
        user.setPartyType(firstText(command.getPartyType(), DEFAULT_PARTY_TYPE));
        user.setPartyId(command.getPartyId());
        user.setEmail(command.getEmail());
        user.setPhone(command.getPhone());
        user.setAvatar(command.getAvatar());
        if (command.getStatus() == null) {
            user.setStatus(1);
        } else {
            user.setStatus(command.getStatus());
        }
        user.setTenantId(currentTenantId());
        user.setRemark(command.getRemark());
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        identityUserMapper.insert(user);
        createTenantMember(user, command.getNickname());
        publishUserCreatedNotice(user);
        return user.getUserId();
    }

    @Override
    @Transactional
    public boolean update(UpdateIdentityUserCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "身份用户修改命令不能为空");
        IdentityUserEntity user = getManageableUser(command.getUserId());
        if (user == null) {
            return false;
        }
        user.setNickname(command.getNickname());
        user.setPartyType(command.getPartyType());
        user.setPartyId(command.getPartyId());
        user.setEmail(command.getEmail());
        user.setPhone(command.getPhone());
        user.setAvatar(command.getAvatar());
        user.setRemark(command.getRemark());
        user.setUpdateTime(LocalDateTime.now());
        TenantMemberEntity member = currentTenantMember(command.getUserId());
        if (member != null) {
            member.setDisplayName(firstText(command.getNickname(), user.getUsername()));
            if (command.getStatus() != null) {
                member.setStatus(command.getStatus());
            }
            member.setRemark(command.getRemark());
            tenantMemberMapper.updateById(member);
        }
        return identityUserMapper.updateById(user) > 0;
    }

    @Override
    @Transactional
    public Boolean deleteUser(Long userId) {
        Require.notNull(userId, IdentityCode.VALIDATION_ERROR, "身份用户ID不能为空");
        BatchDeleteIdentityUserCommand command = new BatchDeleteIdentityUserCommand();
        command.setUserIds(List.of(userId));
        return deleteBatch(command) > 0;
    }

    @Override
    public boolean delete(DeleteCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "身份用户删除命令不能为空");
        return deleteUser(command.getId());
    }

    @Override
    @Transactional
    public Integer deleteBatch(BatchDeleteIdentityUserCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "身份用户批量删除命令不能为空");
        List<Long> userIds = command.getUserIds();
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || userIds == null || userIds.isEmpty()) {
            return 0;
        }
        Long currentUserId = MangoContextHolder.userId();
        Set<Long> targetUserIds = userIds.stream()
                .filter(id -> id != null && !id.equals(currentUserId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (targetUserIds.isEmpty()) {
            return 0;
        }

        List<TenantMemberEntity> members = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .in(TenantMemberEntity::getUserId, targetUserIds)
                .isNull(TenantMemberEntity::getLeftAt));
        if (members == null || members.isEmpty()) {
            return 0;
        }
        Set<Long> memberIds = members.stream()
                .map(TenantMemberEntity::getId)
                .collect(Collectors.toSet());
        roleBindingAdapter.deleteSubjectRoleBindings(currentTenantSubjectRoleDeleteCommand(memberIds));
        tenantMemberOrgMapper.delete(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .in(TenantMemberOrgEntity::getMemberId, memberIds));
        return tenantMemberMapper.delete(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .in(TenantMemberEntity::getId, memberIds));
    }

    @Override
    @Transactional
    public Boolean updateStatus(UpdateIdentityUserStatusCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "身份用户状态命令不能为空");
        IdentityUserEntity user = getManageableUser(command.getUserId());
        if (user == null || command.getUserId().equals(MangoContextHolder.userId())) {
            return false;
        }
        TenantMemberEntity member = currentTenantMember(command.getUserId());
        if (member == null) {
            return false;
        }
        member.setStatus(command.getStatus());
        return tenantMemberMapper.updateById(member) > 0;
    }

    @Override
    @Transactional
    public Boolean resetPassword(ResetIdentityUserPasswordCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "重置密码命令不能为空");
        IdentityUserEntity user = getManageableUser(command.getUserId());
        if (user == null) {
            return false;
        }
        passwordPolicyService.validatePlainPassword(command.getPassword());
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(command.getPassword()));
        user.setPasswordUpdatedAt(now);
        user.setPasswordResetRequired(securityPolicyService.resetRequiredAfterAdminReset());
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
        user.setLockedUntil(null);
        user.setLockedReason(null);
        user.setUpdateTime(now);
        boolean updated = identityUserMapper.updateById(user) > 0;
        if (updated) {
            publishPasswordResetNotice(user);
        }
        return updated;
    }

    @Override
    @Transactional
    public Boolean unlock(UnlockIdentityUserCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "解锁命令不能为空");
        IdentityUserEntity user = getManageableUser(command.getUserId());
        if (user == null) {
            return false;
        }
        return identityUserSecurityService.unlock(user.getUserId());
    }

    @Override
    @Transactional
    public Boolean requirePasswordReset(RequireIdentityUserPasswordResetCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "强制修改密码命令不能为空");
        IdentityUserEntity user = getManageableUser(command.getUserId());
        if (user == null) {
            return false;
        }
        return identityUserSecurityService.requirePasswordReset(user.getUserId());
    }

    @Override
    public IdentityUserInfoVO getUserInfo(String username) {
        IdentityUserEntity user = getByUsername(username);
        if (user == null) {
            log.warn("Identity user not found: {}", username);
            return null;
        }
        return buildIdentityUserInfoVO(user);
    }

    @Override
    public IdentityUserInfoVO getUserInfoById(Long userId) {
        IdentityUserEntity user = getById(userId);
        if (user == null) {
            log.warn("Identity user not found by id: {}", userId);
            return null;
        }
        return buildIdentityUserInfoVO(user);
    }

    @Override
    public List<IdentityUserInfoVO> listUserInfos(IdentityUserBatchQuery query) {
        if (query == null) {
            return List.of();
        }
        LinkedHashSet<Long> userIds = query.getUserIds() == null
                ? new LinkedHashSet<>()
                : query.getUserIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<String> usernames = query.getUsernames() == null
                ? new LinkedHashSet<>()
                : query.getUsernames().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Require.isTrue(userIds.size() + usernames.size() <= IdentityUserBatchQuery.MAX_IDENTIFIERS,
                IdentityCode.VALIDATION_ERROR, "批量查询用户标识不能超过200个");
        if (userIds.isEmpty() && usernames.isEmpty()) {
            return List.of();
        }
        Long tenantId = currentTenantIdLong();
        if (tenantId == null) {
            return List.of();
        }
        LambdaQueryWrapper<IdentityUserEntity> wrapper = new LambdaQueryWrapper<IdentityUserEntity>()
                .eq(IdentityUserEntity::getTenantId, tenantId)
                .and(keys -> {
                    if (!userIds.isEmpty()) {
                        keys.in(IdentityUserEntity::getId, userIds);
                    }
                    if (!userIds.isEmpty() && !usernames.isEmpty()) {
                        keys.or();
                    }
                    if (!usernames.isEmpty()) {
                        keys.in(IdentityUserEntity::getUsername, usernames);
                    }
                });
        List<IdentityUserEntity> users = identityUserMapper.selectList(wrapper);
        if (users.isEmpty()) {
            return List.of();
        }
        Set<Long> matchedUserIds = users.stream()
                .map(IdentityUserEntity::getUserId)
                .collect(Collectors.toSet());
        Set<Long> tenantUserIds = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMemberEntity>()
                        .eq(TenantMemberEntity::getTenantId, tenantId)
                        .in(TenantMemberEntity::getUserId, matchedUserIds)
                        .isNull(TenantMemberEntity::getLeftAt))
                .stream()
                .map(TenantMemberEntity::getUserId)
                .collect(Collectors.toSet());
        return users.stream()
                .filter(user -> tenantUserIds.contains(user.getUserId()))
                .map(this::buildIdentityUserInfoVO)
                .toList();
    }

    @Override
    public List<IdentityUserInfoVO> listUserInfosByTarget(IdentityUserTargetQuery query) {
        if (query == null || query.getTargetType() == null || query.getTargetId() == null) {
            return List.of();
        }
        Set<Long> userIds = switch (query.getTargetType()) {
            case USER -> {
                if (currentTenantSubjectIds(query.getStatus()).contains(query.getTargetId())) {
                    yield Set.of(query.getTargetId());
                }
                yield Set.of();
            }
            case ORG -> currentTenantOrgUserIds(query.getTargetId(), query.getStatus());
            case POST -> currentTenantPostUserIds(query.getTargetId(), query.getStatus());
            case ROLE -> currentTenantRoleUserIds(query.getTargetId(), query.getStatus());
        };
        return listIdentityUserInfoVOs(userIds);
    }

    @Override
    public IdentityUserEntity getByUsername(String username) {
        return getByUsername(username, DEFAULT_REALM);
    }

    @Override
    public IdentityUserEntity getByUsername(String username, String realm) {
        LambdaQueryWrapper<IdentityUserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdentityUserEntity::getUsername, username)
                .eq(IdentityUserEntity::getRealm, normalizeRealm(realm));
        return identityUserMapper.selectOne(wrapper);
    }

    @Override
    public IdentityUserEntity getById(Long userId) {
        return identityUserMapper.selectById(userId);
    }

    @Override
    @Transactional
    public ExternalIdentityBindingVO bindExternalIdentity(BindExternalIdentityCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "外部身份绑定命令不能为空");
        IdentityUserEntity user = getCurrentTenantUser(command.getUserId());
        Require.notNull(user, IdentityCode.NOT_FOUND, "成员不存在或不属于当前租户");
        Long tenantId = currentTenantIdLong();
        String appCode = firstText(command.getAppCode(), DEFAULT_APP_CODE);
        ExternalIdentityBindingEntity existing = findExternalBinding(appCode, command.getProvider(), command.getCorpId(),
                command.getExternalUserId(), tenantId);
        Require.isTrue(existing == null || Objects.equals(existing.getUserId(), command.getUserId()),
                IdentityCode.CONFLICT, "该企业微信用户已绑定其他成员");
        ExternalIdentityBindingEntity entity = existing;
        if (entity == null) {
            entity = new ExternalIdentityBindingEntity();
        }
        entity.setTenantId(String.valueOf(tenantId));
        entity.setAppCode(appCode);
        entity.setUserId(command.getUserId());
        entity.setProvider(normalizeProvider(command.getProvider()));
        entity.setCorpId(command.getCorpId().trim());
        entity.setExternalUserId(command.getExternalUserId().trim());
        entity.setDisplayName(normalizeOptionalText(command.getDisplayName()));
        if (Boolean.TRUE.equals(command.getReplaceAvatarFile())) {
            entity.setAvatarFileId(command.getAvatarFileId());
        } else if (command.getAvatarFileId() != null) {
            entity.setAvatarFileId(command.getAvatarFileId());
        }
        entity.setBindSource(firstText(command.getBindSource(), "SYNC"));
        entity.setBindStatus(STATUS_BOUND);
        if (entity.getBindTime() == null) {
            entity.setBindTime(LocalDateTime.now());
        }
        if (entity.getId() == null) {
            externalIdentityBindingMapper.insert(entity);
        } else {
            externalIdentityBindingMapper.updateById(entity);
        }
        publishExternalIdentityNotice(user, entity, "auth.external.login.bound", "bindTime");
        return toExternalIdentityVO(entity);
    }

    @Override
    @Transactional
    public Boolean unbindExternalIdentity(UnbindExternalIdentityCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "外部身份解绑命令不能为空");
        Long tenantId = currentTenantIdLong();
        String appCode = firstText(command.getAppCode(), DEFAULT_APP_CODE);
        ExternalIdentityBindingEntity existing = findExternalBinding(appCode, command.getProvider(), command.getCorpId(),
                command.getExternalUserId(), tenantId);
        if (existing == null || !Objects.equals(existing.getUserId(), command.getUserId())) {
            return false;
        }
        boolean deleted = externalIdentityBindingMapper.deleteById(existing.getId()) > 0;
        if (deleted) {
            IdentityUserEntity user = getManageableUser(command.getUserId());
            if (user != null) {
                publishExternalIdentityNotice(user, existing, "auth.wecom.login.unbound", "unbindTime");
            }
        }
        return deleted;
    }

    @Override
    public ExternalIdentityBindingVO findExternalIdentity(ExternalIdentityQuery query) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || query == null) {
            return null;
        }
        LambdaQueryWrapper<ExternalIdentityBindingEntity> wrapper = new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, tenantId);
        wrapper.eq(StringUtils.hasText(query.getAppCode()), ExternalIdentityBindingEntity::getAppCode,
                normalizeOptionalText(query.getAppCode()));
        wrapper.eq(StringUtils.hasText(query.getProvider()), ExternalIdentityBindingEntity::getProvider,
                normalizeProvider(query.getProvider()));
        wrapper.eq(StringUtils.hasText(query.getCorpId()), ExternalIdentityBindingEntity::getCorpId,
                normalizeOptionalText(query.getCorpId()));
        wrapper.eq(StringUtils.hasText(query.getExternalUserId()), ExternalIdentityBindingEntity::getExternalUserId,
                normalizeOptionalText(query.getExternalUserId()));
        wrapper.eq(query.getUserId() != null, ExternalIdentityBindingEntity::getUserId, query.getUserId());
        wrapper.eq(ExternalIdentityBindingEntity::getBindStatus, STATUS_BOUND);
        wrapper.last("LIMIT 1");
        return toExternalIdentityVO(externalIdentityBindingMapper.selectOne(wrapper));
    }

    @Override
    public List<ExternalIdentityBindingVO> listExternalIdentities(Long userId) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || userId == null) {
            return List.of();
        }
        return externalIdentityBindingMapper.selectList(new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, tenantId)
                .eq(ExternalIdentityBindingEntity::getUserId, userId)
                .eq(ExternalIdentityBindingEntity::getBindStatus, STATUS_BOUND)
                .orderByDesc(ExternalIdentityBindingEntity::getBindTime))
                .stream()
                .map(this::toExternalIdentityVO)
                .toList();
    }

    @Override
    public List<ExternalIdentityBindingVO> listCurrentExternalIdentities() {
        IdentityUserEntity user = currentUser();
        String appCode = firstText(MangoContextHolder.appCode(), DEFAULT_APP_CODE);
        return externalIdentityBindingMapper.selectList(new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, currentTenantIdLong())
                .eq(ExternalIdentityBindingEntity::getUserId, user.getUserId())
                .eq(ExternalIdentityBindingEntity::getAppCode, appCode)
                .eq(ExternalIdentityBindingEntity::getBindStatus, STATUS_BOUND)
                .orderByAsc(ExternalIdentityBindingEntity::getProvider)
                .orderByDesc(ExternalIdentityBindingEntity::getBindTime))
                .stream()
                .map(this::toMaskedExternalIdentityVO)
                .toList();
    }

    @Override
    @Transactional
    public Boolean unbindCurrentExternalIdentity(UnbindCurrentExternalIdentityCommand command) {
        Require.notNull(command, IdentityCode.VALIDATION_ERROR, "当前用户解绑命令不能为空");
        IdentityUserEntity user = currentUser();
        Require.isTrue(passwordEncoder.matches(command.getCurrentPassword(), user.getPassword()),
                IdentityCode.CURRENT_PASSWORD_INVALID);
        ExternalIdentityBindingEntity binding = Require.nonNull(
                externalIdentityBindingMapper.selectById(command.getBindingId()),
                IdentityCode.NOT_FOUND, "第三方授权不存在");
        String appCode = firstText(MangoContextHolder.appCode(), DEFAULT_APP_CODE);
        Require.isTrue(Objects.equals(binding.getUserId(), user.getUserId())
                        && Objects.equals(binding.getTenantId(), currentTenantId())
                        && appCode.equals(binding.getAppCode()),
                IdentityCode.NOT_FOUND, "第三方授权不存在");
        boolean deleted = externalIdentityBindingMapper.deleteById(binding.getId()) > 0;
        Require.isTrue(deleted, IdentityCode.CONFLICT, "第三方授权解绑失败");
        publishExternalIdentityNotice(user, binding, "auth.external.login.unbound", "unbindTime");
        return true;
    }

    @Override
    protected Class<IdentityUserEntity> entityType() {
        return IdentityUserEntity.class;
    }

    @Override
    protected QueryWrapper<IdentityUserEntity> buildQueryWrapper(Object queryObject) {
        IdentityUserPageQuery query = new IdentityUserPageQuery();
        if (queryObject instanceof IdentityUserPageQuery pageQuery) {
            query = pageQuery;
        }
        QueryWrapper<IdentityUserEntity> wrapper = new QueryWrapper<>();
        LambdaQueryWrapper<IdentityUserEntity> lambda = wrapper.lambda();
        Long tenantId = currentTenantIdLong();
        Set<Long> subjectIds = currentTenantSubjectIds(query.getStatus());
        lambda.eq(tenantId != null, IdentityUserEntity::getTenantId, tenantId);
        if (subjectIds.isEmpty()) {
            lambda.eq(IdentityUserEntity::getId, -1L);
        } else {
            lambda.in(IdentityUserEntity::getId, subjectIds);
        }
        lambda.like(StringUtils.hasText(query.getUsername()), IdentityUserEntity::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), IdentityUserEntity::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getPhone()), IdentityUserEntity::getPhone, query.getPhone())
                .like(StringUtils.hasText(query.getEmail()), IdentityUserEntity::getEmail, query.getEmail())
                .eq(StringUtils.hasText(query.getRealm()), IdentityUserEntity::getRealm, query.getRealm())
                .eq(StringUtils.hasText(query.getActorType()), IdentityUserEntity::getActorType, query.getActorType())
                .eq(StringUtils.hasText(query.getPartyType()), IdentityUserEntity::getPartyType, query.getPartyType())
                .eq(query.getPartyId() != null, IdentityUserEntity::getPartyId, query.getPartyId())
                .orderByDesc(IdentityUserEntity::getCreateTime);
        return wrapper;
    }

    @Override
    protected IdentityUserVO toVO(IdentityUserEntity entity) {
        return toVO(entity, null);
    }

    /**
     * 从账号资料构造身份资料 VO。
     */
    private IdentityUserInfoVO buildIdentityUserInfoVO(IdentityUserEntity user) {
        IdentityUserInfoVO userInfo = new IdentityUserInfoVO();
        userInfo.setUserId(user.getUserId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setRealm(user.getRealm());
        userInfo.setActorType(user.getActorType());
        userInfo.setPartyType(user.getPartyType());
        userInfo.setPartyId(user.getPartyId());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setStatus(user.getStatus());

        return userInfo;
    }

    private void publishUserCreatedNotice(IdentityUserEntity user) {
        Map<String, Object> params = baseUserParams(user);
        String createdAt = null;
        if (user.getCreateTime() != null) {
            createdAt = user.getCreateTime().toString();
        }
        params.put("createdAt", createdAt);
        NoticeSiteMessageTargetCommand target = routeTarget("account:profile", params);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId(user.getTenantId());
        event.setBizType("identity.user.created");
        event.setBizId(String.valueOf(user.getUserId()));
        event.setUserId(user.getUserId());
        event.setParams(NoticeJsonRequest.of(params));
        event.setMessageScene("identity.user.created");
        event.setMessageSubject(userSubject(user));
        event.setMessageTarget(target);
        event.setMessageData(NoticeJsonRequest.of(params));
        event.setMessageActions(List.of(routeAction("VIEW_PROFILE", "查看资料", target)));
        event.setPriority(NoticePriority.NORMAL);
        event.setIdempotentKey("identity.user.created:" + user.getUserId());
        eventPublisher.publishEvent(event);
    }

    private void publishPasswordResetNotice(IdentityUserEntity user) {
        Map<String, Object> params = baseUserParams(user);
        params.put("resetAt", LocalDateTime.now().toString());
        NoticeSiteMessageTargetCommand target = routeTarget("account:password", params);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId(user.getTenantId());
        event.setBizType("identity.password.reset");
        event.setBizId(String.valueOf(user.getUserId()));
        event.setUserId(user.getUserId());
        event.setParams(NoticeJsonRequest.of(params));
        event.setMessageScene("identity.password.reset");
        event.setMessageSubject(userSubject(user));
        event.setMessageTarget(target);
        event.setMessageData(NoticeJsonRequest.of(params));
        event.setMessageActions(List.of(routeAction("CHANGE_PASSWORD", "修改密码", target)));
        event.setPriority(NoticePriority.HIGH);
        event.setIdempotentKey("identity.password.reset:" + user.getUserId() + ":" + System.currentTimeMillis());
        eventPublisher.publishEvent(event);
    }

    private void publishExternalIdentityNotice(IdentityUserEntity user, ExternalIdentityBindingEntity binding,
                                               String bizType, String timeParam) {
        Map<String, Object> params = baseUserParams(user);
        params.put("provider", binding.getProvider());
        params.put("externalIdentity", maskIdentifier(binding.getExternalUserId()));
        params.put(timeParam, LocalDateTime.now().toString());
        NoticeSiteMessageTargetCommand target = routeTarget("account:profile", params);
        NoticeSendEventCommand event = new NoticeSendEventCommand();
        event.setTenantId(user.getTenantId());
        event.setBizType(bizType);
        event.setBizId(user.getUserId() + ":" + binding.getProvider() + ":" + binding.getId());
        event.setUserId(user.getUserId());
        event.setParams(NoticeJsonRequest.of(params));
        event.setMessageScene(bizType);
        event.setMessageSubject(userSubject(user));
        event.setMessageTarget(target);
        event.setMessageData(NoticeJsonRequest.of(params));
        event.setMessageActions(List.of(routeAction("VIEW_PROFILE", "查看资料", target)));
        event.setPriority(NoticePriority.NORMAL);
        event.setIdempotentKey(bizType + ":" + user.getUserId() + ":" + binding.getId());
        eventPublisher.publishEvent(event);
    }

    private Map<String, Object> baseUserParams(IdentityUserEntity user) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", String.valueOf(user.getUserId()));
        params.put("username", user.getUsername());
        params.put("nickname", user.getNickname());
        params.put("tenantId", currentTenantId());
        return params;
    }

    private NoticeSiteMessageSubjectCommand userSubject(IdentityUserEntity user) {
        NoticeSiteMessageSubjectCommand subject = new NoticeSiteMessageSubjectCommand();
        subject.setSubjectType("IDENTITY_USER");
        subject.setSubjectId(String.valueOf(user.getUserId()));
        subject.setSubjectName(firstText(user.getNickname(), user.getUsername()));
        return subject;
    }

    private NoticeSiteMessageTargetCommand routeTarget(String targetKey, Map<String, Object> params) {
        NoticeSiteMessageTargetCommand target = new NoticeSiteMessageTargetCommand();
        target.setTargetType(NoticeSiteMessageTargetType.ROUTE);
        target.setTargetKey(targetKey);
        target.setParams(NoticeJsonRequest.of(params));
        return target;
    }

    private NoticeSiteMessageActionCommand routeAction(String actionCode, String actionLabel, NoticeSiteMessageTargetCommand target) {
        NoticeSiteMessageActionCommand action = new NoticeSiteMessageActionCommand();
        action.setActionCode(actionCode);
        action.setActionLabel(actionLabel);
        action.setInteractionType(NoticeSiteMessageActionInteractionType.ROUTE);
        action.setTarget(target);
        return action;
    }

    private LambdaQueryWrapper<IdentityUserEntity> buildManageableUserWrapper(IdentityUserPageQuery query) {
        LambdaQueryWrapper<IdentityUserEntity> wrapper = new LambdaQueryWrapper<>();
        Set<Long> subjectIds = currentTenantSubjectIds(query.getStatus());
        if (query.getOrgId() != null) {
            Set<Long> orgUserIds = currentTenantOrgUserIds(query.getOrgId(), query.getStatus());
            subjectIds = subjectIds.stream()
                    .filter(orgUserIds::contains)
                    .collect(Collectors.toSet());
        }
        if (subjectIds.isEmpty()) {
            wrapper.eq(IdentityUserEntity::getId, -1L);
        } else {
            wrapper.in(IdentityUserEntity::getId, subjectIds);
        }
        wrapper.like(StringUtils.hasText(query.getUsername()), IdentityUserEntity::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), IdentityUserEntity::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getPhone()), IdentityUserEntity::getPhone, query.getPhone())
                .like(StringUtils.hasText(query.getEmail()), IdentityUserEntity::getEmail, query.getEmail())
                .and(StringUtils.hasText(query.getKeyword()), keyword -> keyword
                        .like(IdentityUserEntity::getUsername, query.getKeyword())
                        .or()
                        .like(IdentityUserEntity::getNickname, query.getKeyword())
                        .or()
                        .like(IdentityUserEntity::getPhone, query.getKeyword())
                        .or()
                        .like(IdentityUserEntity::getEmail, query.getKeyword()))
                .eq(StringUtils.hasText(query.getRealm()), IdentityUserEntity::getRealm, query.getRealm())
                .eq(StringUtils.hasText(query.getActorType()), IdentityUserEntity::getActorType, query.getActorType())
                .eq(StringUtils.hasText(query.getPartyType()), IdentityUserEntity::getPartyType, query.getPartyType())
                .eq(query.getPartyId() != null, IdentityUserEntity::getPartyId, query.getPartyId())
                .orderByDesc(IdentityUserEntity::getCreateTime);
        return wrapper;
    }

    private IdentityUserEntity getManageableUser(Long userId) {
        if (userId == null) {
            return null;
        }
        IdentityUserEntity user = identityUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        if (currentTenantMember(userId) != null) {
            return user;
        }
        if (belongsToCurrentTenant(user)) {
            log.info("Repair missing tenant member relation: userId={}, tenantId={}", userId, currentTenantId());
            createTenantMember(user, firstText(user.getNickname(), user.getUsername()));
            return user;
        }
        log.warn("Tenant isolation violation: attempt to manage identity user {} by tenant {}", userId, currentTenantId());
        return null;
    }

    private IdentityUserEntity getCurrentTenantUser(Long userId) {
        if (userId == null || currentTenantMember(userId) == null) {
            return null;
        }
        return identityUserMapper.selectById(userId);
    }

    private boolean belongsToCurrentTenant(IdentityUserEntity user) {
        return user != null
                && StringUtils.hasText(user.getTenantId())
                && Objects.equals(user.getTenantId(), currentTenantId());
    }

    private Set<Long> currentTenantSubjectIds(Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null) {
            return Set.of();
        }
        LambdaQueryWrapper<TenantMemberEntity> wrapper = new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .isNull(TenantMemberEntity::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMemberEntity::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper)
                .stream()
                .map(TenantMemberEntity::getUserId)
                .collect(Collectors.toSet());
    }

    private Set<Long> currentTenantOrgUserIds(Long orgId, Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || orgId == null) {
            return Set.of();
        }
        List<TenantMemberOrgEntity> relations = tenantMemberOrgMapper.selectList(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                        .eq(TenantMemberOrgEntity::getOrgId, orgId));
        if (relations == null || relations.isEmpty()) {
            return Set.of();
        }
        Set<Long> memberIds = relations.stream()
                .map(TenantMemberOrgEntity::getMemberId)
                .collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<TenantMemberEntity> wrapper = new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .in(TenantMemberEntity::getId, memberIds)
                .isNull(TenantMemberEntity::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMemberEntity::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper).stream()
                .map(TenantMemberEntity::getUserId)
                .collect(Collectors.toSet());
    }

    private Set<Long> currentTenantPostUserIds(Long postId, Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || postId == null) {
            return Set.of();
        }
        List<TenantMemberOrgEntity> relations = tenantMemberOrgMapper.selectList(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                        .eq(TenantMemberOrgEntity::getPostId, postId));
        return currentTenantRelationUserIds(relations, memberStatus);
    }

    private Set<Long> currentTenantRoleUserIds(Long roleId, Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || roleId == null) {
            return Set.of();
        }
        SubjectRoleBindingQuery query = new SubjectRoleBindingQuery();
        query.setTenantId(tenantId);
        query.setAppCode(MangoContextHolder.appCode());
        query.setRealm(MangoContextHolder.get().realm());
        query.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        query.setRoleId(roleId);
        List<Long> subjectIds = roleBindingAdapter.listSubjectIdsByRole(query);
        if (subjectIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> memberIds = new LinkedHashSet<>(subjectIds);
        return currentTenantMemberUserIds(memberIds, memberStatus);
    }

    private Set<Long> currentTenantRelationUserIds(List<TenantMemberOrgEntity> relations, Integer memberStatus) {
        if (relations == null || relations.isEmpty()) {
            return Set.of();
        }
        Set<Long> memberIds = relations.stream()
                .map(TenantMemberOrgEntity::getMemberId)
                .collect(Collectors.toSet());
        return currentTenantMemberUserIds(memberIds, memberStatus);
    }

    private Set<Long> currentTenantMemberUserIds(Collection<Long> memberIds, Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || memberIds == null || memberIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<TenantMemberEntity> wrapper = new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .in(TenantMemberEntity::getId, memberIds)
                .isNull(TenantMemberEntity::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMemberEntity::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper).stream()
                .map(TenantMemberEntity::getUserId)
                .collect(Collectors.toSet());
    }

    private List<IdentityUserInfoVO> listIdentityUserInfoVOs(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return identityUserMapper.selectList(new LambdaQueryWrapper<IdentityUserEntity>()
                        .in(IdentityUserEntity::getId, userIds)
                        .eq(IdentityUserEntity::getStatus, 1))
                .stream()
                .map(this::buildIdentityUserInfoVO)
                .toList();
    }

    private DeleteSubjectRoleBindingsCommand currentTenantSubjectRoleDeleteCommand(Collection<Long> subjectIds) {
        DeleteSubjectRoleBindingsCommand command = new DeleteSubjectRoleBindingsCommand();
        command.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        if (subjectIds == null) {
            command.setSubjectIds(List.of());
        } else {
            command.setSubjectIds(List.copyOf(subjectIds));
        }
        command.setTenantId(currentTenantIdLong());
        return command;
    }

    private IdentityUserVO toVO(IdentityUserEntity user, Long queryOrgId) {
        IdentityUserVO vo = new IdentityUserVO();
        TenantMemberEntity member = currentTenantMember(user.getUserId());
        vo.setUserId(user.getUserId());
        if (member != null) {
            vo.setMemberId(member.getMemberId());
            vo.setMemberName(member.getDisplayName());
            vo.setMemberType(member.getMemberType());
            vo.setMemberStatus(member.getStatus());
            vo.setPrimaryOrgId(member.getPrimaryOrgId());
            fillOrgRelation(vo, member, queryOrgId);
        }
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRealm(user.getRealm());
        vo.setActorType(user.getActorType());
        vo.setPartyType(user.getPartyType());
        vo.setPartyId(user.getPartyId());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        if (member == null) {
            vo.setStatus(user.getStatus());
        } else {
            vo.setStatus(member.getStatus());
        }
        vo.setTenantId(user.getTenantId());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setPasswordResetRequired(Boolean.TRUE.equals(user.getPasswordResetRequired()));
        vo.setPasswordUpdatedAt(user.getPasswordUpdatedAt());
        if (user.getFailedLoginCount() == null) {
            vo.setFailedLoginCount(0);
        } else {
            vo.setFailedLoginCount(user.getFailedLoginCount());
        }
        vo.setLastFailedLoginAt(user.getLastFailedLoginAt());
        vo.setLockedUntil(user.getLockedUntil());
        vo.setLockedReason(user.getLockedReason());
        vo.setLocked(identityUserSecurityService.isLocked(user));
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    private void fillOrgRelation(IdentityUserVO vo, TenantMemberEntity member, Long queryOrgId) {
        if (queryOrgId == null) {
            return;
        }
        TenantMemberOrgEntity relation = tenantMemberOrgMapper.selectOne(
                new LambdaQueryWrapper<TenantMemberOrgEntity>()
                        .eq(TenantMemberOrgEntity::getTenantId, member.getTenantId())
                        .eq(TenantMemberOrgEntity::getMemberId, member.getMemberId())
                        .eq(TenantMemberOrgEntity::getOrgId, queryOrgId)
                        .orderByDesc(TenantMemberOrgEntity::getPrimaryFlag)
                        .orderByAsc(TenantMemberOrgEntity::getId)
                        .last("LIMIT 1"));
        if (relation == null) {
            return;
        }
        vo.setOrgRelationId(relation.getId());
        vo.setOrgId(relation.getOrgId());
        vo.setPostId(relation.getPostId());
        vo.setPrimaryOrgFlag(Integer.valueOf(1).equals(relation.getPrimaryFlag()));
        vo.setOrgLeaderFlag(Integer.valueOf(1).equals(relation.getLeaderFlag()));
    }

    private void createTenantMember(IdentityUserEntity user, String displayName) {
        Long tenantId = currentTenantIdLong();
        Require.notNull(tenantId, IdentityCode.VALIDATION_ERROR, "当前机构上下文无效");
        TenantMemberEntity member = new TenantMemberEntity();
        member.setTenantId(String.valueOf(tenantId));
        member.setUserId(user.getUserId());
        member.setMemberNo("USER-" + user.getUserId());
        member.setDisplayName(firstText(displayName, user.getUsername()));
        member.setMemberType("EMPLOYEE");
        member.setStatus(user.getStatus());
        member.setJoinedAt(LocalDateTime.now());
        member.setRemark(user.getRemark());
        tenantMemberMapper.insert(member);
    }

    private ExternalIdentityBindingEntity findExternalBinding(String appCode, String provider, String corpId,
                                                              String externalUserId, Long tenantId) {
        if (tenantId == null || !StringUtils.hasText(provider) || !StringUtils.hasText(corpId)
                || !StringUtils.hasText(externalUserId)) {
            return null;
        }
        return externalIdentityBindingMapper.selectOne(new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, tenantId)
                .eq(ExternalIdentityBindingEntity::getAppCode, firstText(appCode, DEFAULT_APP_CODE))
                .eq(ExternalIdentityBindingEntity::getProvider, normalizeProvider(provider))
                .eq(ExternalIdentityBindingEntity::getCorpId, corpId.trim())
                .eq(ExternalIdentityBindingEntity::getExternalUserId, externalUserId.trim())
                .last("LIMIT 1"));
    }

    private ExternalIdentityBindingVO toExternalIdentityVO(ExternalIdentityBindingEntity entity) {
        if (entity == null) {
            return null;
        }
        ExternalIdentityBindingVO vo = new ExternalIdentityBindingVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setAppCode(entity.getAppCode());
        vo.setProvider(entity.getProvider());
        vo.setCorpId(entity.getCorpId());
        vo.setExternalUserId(entity.getExternalUserId());
        vo.setDisplayName(entity.getDisplayName());
        vo.setAvatarFileId(entity.getAvatarFileId());
        vo.setBindSource(entity.getBindSource());
        vo.setBindStatus(entity.getBindStatus());
        vo.setBindTime(entity.getBindTime());
        vo.setLastLoginTime(entity.getLastLoginTime());
        return vo;
    }

    private ExternalIdentityBindingVO toMaskedExternalIdentityVO(ExternalIdentityBindingEntity entity) {
        ExternalIdentityBindingVO vo = toExternalIdentityVO(entity);
        if (vo == null) {
            return null;
        }
        vo.setCorpId(maskIdentifier(vo.getCorpId()));
        vo.setExternalUserId(maskIdentifier(vo.getExternalUserId()));
        return vo;
    }

    private IdentityUserEntity currentUser() {
        Long userId = Require.nonNull(MangoContextHolder.userId(), IdentityCode.NOT_FOUND,
                "当前登录用户不存在");
        IdentityUserEntity user = Require.nonNull(identityUserMapper.selectById(userId), IdentityCode.NOT_FOUND,
                "当前登录用户不存在");
        Require.notNull(currentTenantMember(userId), IdentityCode.NOT_FOUND, "当前用户不属于当前租户");
        return user;
    }

    private CurrentUserProfileVO toCurrentProfile(IdentityUserEntity user) {
        CurrentUserProfileVO vo = new CurrentUserProfileVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(maskPhone(user.getPhone()));
        vo.setEmail(maskEmail(user.getEmail()));
        vo.setRealName(user.getRealName());
        vo.setDocumentType(user.getDocumentType());
        vo.setDocumentNumber(maskIdentifier(user.getDocumentNumber()));
        vo.setVerificationStatus(firstText(user.getVerificationStatus(), VERIFICATION_UNVERIFIED));
        vo.setVerificationSource(user.getVerificationSource());
        return vo;
    }

    private Contact validateContact(String rawType, String rawTarget) {
        ContactType type;
        try {
            type = ContactType.valueOf(rawType == null ? "" : rawType.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return Require.fail(IdentityCode.VALIDATION_ERROR, "联系方式类型无效", exception);
        }
        String target = trimToNull(rawTarget);
        Require.notNull(target, IdentityCode.VALIDATION_ERROR, "新联系方式不能为空");
        if (type == ContactType.PHONE) {
            Require.isTrue(target.matches("^\\+?[0-9]{6,20}$"), IdentityCode.VALIDATION_ERROR,
                    "手机号格式无效");
        } else {
            Require.isTrue(target.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"), IdentityCode.VALIDATION_ERROR,
                    "邮箱格式无效");
        }
        return new Contact(type, target);
    }

    private void requireContactAvailable(Long userId, Contact contact) {
        LambdaQueryWrapper<IdentityUserEntity> wrapper = new LambdaQueryWrapper<IdentityUserEntity>()
                .ne(IdentityUserEntity::getId, userId);
        if (contact.type() == ContactType.PHONE) {
            wrapper.eq(IdentityUserEntity::getPhone, contact.value());
        } else {
            wrapper.eq(IdentityUserEntity::getEmail, contact.value());
        }
        Require.isTrue(identityUserMapper.selectCount(wrapper) == 0, IdentityCode.CONFLICT,
                "该联系方式已被其他账号使用");
    }

    private String maskContact(Contact contact) {
        return contact.type() == ContactType.PHONE ? maskPhone(contact.value()) : maskEmail(contact.value());
    }

    private String maskPhone(String value) {
        if (!StringUtils.hasText(value) || value.length() <= MASK_SUFFIX_LENGTH) {
            return value;
        }
        int visiblePrefix = Math.min(MASK_PHONE_PREFIX_LENGTH, value.length() - MASK_SUFFIX_LENGTH);
        return value.substring(0, visiblePrefix) + "****"
                + value.substring(value.length() - MASK_SUFFIX_LENGTH);
    }

    private String maskEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        int separator = value.indexOf('@');
        if (separator <= 0) {
            return maskIdentifier(value);
        }
        String local = value.substring(0, separator);
        return local.substring(0, 1) + "***" + value.substring(separator);
    }

    private String maskIdentifier(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        if (value.length() <= MASK_SUFFIX_LENGTH) {
            return "****";
        }
        return "****" + value.substring(value.length() - MASK_SUFFIX_LENGTH);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private enum ContactType {
        PHONE,
        EMAIL
    }

    private record Contact(ContactType type, String value) {
        CaptchaType captchaType() {
            return type == ContactType.PHONE ? CaptchaType.SMS : CaptchaType.EMAIL;
        }

        String businessType() {
            return type == ContactType.PHONE ? "CHANGE_PHONE" : "CHANGE_EMAIL";
        }

        boolean matches(IdentityUserEntity user) {
            return type == ContactType.PHONE
                    ? value.equals(user.getPhone())
                    : value.equalsIgnoreCase(user.getEmail());
        }
    }

    private TenantMemberEntity currentTenantMember(Long userId) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || userId == null) {
            return null;
        }
        return tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberEntity>()
                .eq(TenantMemberEntity::getTenantId, tenantId)
                .eq(TenantMemberEntity::getUserId, userId)
                .isNull(TenantMemberEntity::getLeftAt)
                .last("LIMIT 1"));
    }

    private String firstText(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            return preferred.trim();
        }
        return fallback;
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeRealm(String realm) {
        if (realm == null || realm.isBlank()) {
            return DEFAULT_REALM;
        }
        return realm.trim();
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return null;
        }
        return provider.trim().toUpperCase();
    }

    private String currentTenantId() {
        return MangoContextHolder.tenantId();
    }

    private Long currentTenantIdLong() {
        try {
            return Long.valueOf(currentTenantId());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
