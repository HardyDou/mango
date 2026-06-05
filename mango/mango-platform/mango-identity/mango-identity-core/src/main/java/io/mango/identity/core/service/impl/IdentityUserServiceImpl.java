package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.CreateIdentityUserCommand;
import io.mango.identity.api.command.ResetIdentityUserPasswordCommand;
import io.mango.identity.api.command.UnbindExternalIdentityCommand;
import io.mango.identity.api.command.UpdateIdentityUserCommand;
import io.mango.identity.api.command.UpdateIdentityUserStatusCommand;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.vo.ExternalIdentityBindingVO;
import io.mango.identity.api.vo.IdentityUserInfo;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.identity.core.entity.ExternalIdentityBindingEntity;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.ExternalIdentityBindingMapper;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.identity.core.service.IIdentityUserService;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 身份用户服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityUserServiceImpl implements IIdentityUserService {

    private static final String DEFAULT_REALM = "INTERNAL";
    private static final String DEFAULT_ACTOR_TYPE = "INTERNAL_USER";
    private static final String DEFAULT_PARTY_TYPE = "INTERNAL_ORG";
    private static final String DEFAULT_INITIAL_PASSWORD = "admin123";
    private static final String STATUS_BOUND = "BOUND";

    private final IdentityUserMapper identityUserMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final TenantMemberOrgMapper tenantMemberOrgMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final ExternalIdentityBindingMapper externalIdentityBindingMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<IdentityUserVO> page(IdentityUserPageQuery query) {
        LambdaQueryWrapper<IdentityUser> wrapper = buildManageableUserWrapper(query);
        IPage<IdentityUser> page = identityUserMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);
        List<IdentityUserVO> list = page.getRecords().stream()
                .map(user -> toVO(user, query.getOrgId()))
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public IdentityUserVO detail(Long userId) {
        IdentityUser user = getManageableUser(userId);
        return user == null ? null : toVO(user, null);
    }

    @Override
    @Transactional
    public Long create(CreateIdentityUserCommand command) {
        String realm = firstText(command.getRealm(), DEFAULT_REALM);
        IdentityUser existing = getByUsername(command.getUsername(), realm);
        if (existing != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        IdentityUser user = new IdentityUser();
        user.setUsername(command.getUsername().trim());
        user.setPassword(passwordEncoder.encode(firstText(command.getPassword(), DEFAULT_INITIAL_PASSWORD)));
        user.setNickname(command.getNickname());
        user.setRealm(realm);
        user.setActorType(firstText(command.getActorType(), DEFAULT_ACTOR_TYPE));
        user.setPartyType(firstText(command.getPartyType(), DEFAULT_PARTY_TYPE));
        user.setPartyId(command.getPartyId());
        user.setEmail(command.getEmail());
        user.setPhone(command.getPhone());
        user.setAvatar(command.getAvatar());
        user.setStatus(command.getStatus() == null ? 1 : command.getStatus());
        user.setTenantId(currentTenantId());
        user.setRemark(command.getRemark());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        identityUserMapper.insert(user);
        createTenantMember(user, command.getNickname());
        return user.getUserId();
    }

    @Override
    @Transactional
    public Boolean update(UpdateIdentityUserCommand command) {
        IdentityUser user = getManageableUser(command.getUserId());
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
        TenantMember member = currentTenantMember(command.getUserId());
        if (member != null) {
            member.setDisplayName(firstText(command.getNickname(), user.getUsername()));
            member.setStatus(command.getStatus() == null ? member.getStatus() : command.getStatus());
            member.setRemark(command.getRemark());
            tenantMemberMapper.updateById(member);
        }
        return identityUserMapper.updateById(user) > 0;
    }

    @Override
    @Transactional
    public Boolean delete(Long userId) {
        if (userId == null) {
            return false;
        }
        return deleteBatch(List.of(userId)) > 0;
    }

    @Override
    @Transactional
    public Integer deleteBatch(List<Long> userIds) {
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

        List<TenantMember> members = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .in(TenantMember::getUserId, targetUserIds)
                .isNull(TenantMember::getLeftAt));
        if (members == null || members.isEmpty()) {
            return 0;
        }
        Set<Long> memberIds = members.stream()
                .map(TenantMember::getMemberId)
                .collect(Collectors.toSet());
        subjectRoleBindingMapper.delete(currentTenantSubjectRoleWrapper(memberIds));
        tenantMemberOrgMapper.delete(new LambdaQueryWrapper<TenantMemberOrgEntity>()
                .eq(TenantMemberOrgEntity::getTenantId, tenantId)
                .in(TenantMemberOrgEntity::getMemberId, memberIds));
        return tenantMemberMapper.delete(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .in(TenantMember::getMemberId, memberIds));
    }

    @Override
    @Transactional
    public Boolean updateStatus(UpdateIdentityUserStatusCommand command) {
        IdentityUser user = getManageableUser(command.getUserId());
        if (user == null || command.getUserId().equals(MangoContextHolder.userId())) {
            return false;
        }
        TenantMember member = currentTenantMember(command.getUserId());
        if (member == null) {
            return false;
        }
        member.setStatus(command.getStatus());
        return tenantMemberMapper.updateById(member) > 0;
    }

    @Override
    @Transactional
    public Boolean resetPassword(ResetIdentityUserPasswordCommand command) {
        IdentityUser user = getManageableUser(command.getUserId());
        if (user == null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(command.getPassword()));
        user.setUpdateTime(LocalDateTime.now());
        return identityUserMapper.updateById(user) > 0;
    }

    @Override
    public IdentityUserInfo getUserInfo(String username) {
        IdentityUser user = getByUsername(username);
        if (user == null) {
            log.warn("Identity user not found: {}", username);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public IdentityUserInfo getUserInfoById(Long userId) {
        IdentityUser user = getById(userId);
        if (user == null) {
            log.warn("Identity user not found by id: {}", userId);
            return null;
        }
        return buildIdentityUserInfo(user);
    }

    @Override
    public List<IdentityUserInfo> listUserInfosByTarget(IdentityUserTargetQuery query) {
        if (query == null || query.getTargetType() == null || query.getTargetId() == null) {
            return List.of();
        }
        Set<Long> userIds = switch (query.getTargetType()) {
            case USER -> currentTenantSubjectIds(query.getStatus()).contains(query.getTargetId())
                    ? Set.of(query.getTargetId()) : Set.of();
            case ORG -> currentTenantOrgUserIds(query.getTargetId(), query.getStatus());
            case POST -> currentTenantPostUserIds(query.getTargetId(), query.getStatus());
            case ROLE -> currentTenantRoleUserIds(query.getTargetId(), query.getStatus());
        };
        return listIdentityUserInfos(userIds);
    }

    @Override
    public IdentityUser getByUsername(String username) {
        return getByUsername(username, DEFAULT_REALM);
    }

    @Override
    public IdentityUser getByUsername(String username, String realm) {
        LambdaQueryWrapper<IdentityUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdentityUser::getUsername, username)
                .eq(IdentityUser::getRealm, normalizeRealm(realm));
        return identityUserMapper.selectOne(wrapper);
    }

    @Override
    public IdentityUser getById(Long userId) {
        return identityUserMapper.selectById(userId);
    }

    @Override
    @Transactional
    public ExternalIdentityBindingVO bindExternalIdentity(BindExternalIdentityCommand command) {
        IdentityUser user = getManageableUser(command.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("成员不存在或不可管理");
        }
        Long tenantId = currentTenantIdLong();
        ExternalIdentityBindingEntity existing = findExternalBinding(command.getProvider(), command.getCorpId(),
                command.getExternalUserId(), tenantId);
        if (existing != null && !Objects.equals(existing.getUserId(), command.getUserId())) {
            throw new IllegalArgumentException("该企业微信用户已绑定其他成员");
        }
        ExternalIdentityBindingEntity entity = existing == null ? new ExternalIdentityBindingEntity() : existing;
        entity.setTenantId(tenantId);
        entity.setUserId(command.getUserId());
        entity.setProvider(normalizeProvider(command.getProvider()));
        entity.setCorpId(command.getCorpId().trim());
        entity.setExternalUserId(command.getExternalUserId().trim());
        entity.setDisplayName(firstText(command.getDisplayName(), user.getNickname()));
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
        return toExternalIdentityVO(entity);
    }

    @Override
    @Transactional
    public Boolean unbindExternalIdentity(UnbindExternalIdentityCommand command) {
        Long tenantId = currentTenantIdLong();
        ExternalIdentityBindingEntity existing = findExternalBinding(command.getProvider(), command.getCorpId(),
                command.getExternalUserId(), tenantId);
        if (existing == null || !Objects.equals(existing.getUserId(), command.getUserId())) {
            return false;
        }
        return externalIdentityBindingMapper.deleteById(existing.getId()) > 0;
    }

    @Override
    public ExternalIdentityBindingVO findExternalIdentity(ExternalIdentityQuery query) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || query == null) {
            return null;
        }
        LambdaQueryWrapper<ExternalIdentityBindingEntity> wrapper = new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, tenantId);
        wrapper.eq(StringUtils.hasText(query.getProvider()), ExternalIdentityBindingEntity::getProvider,
                normalizeProvider(query.getProvider()));
        wrapper.eq(StringUtils.hasText(query.getCorpId()), ExternalIdentityBindingEntity::getCorpId, query.getCorpId());
        wrapper.eq(StringUtils.hasText(query.getExternalUserId()), ExternalIdentityBindingEntity::getExternalUserId,
                query.getExternalUserId());
        wrapper.eq(query.getUserId() != null, ExternalIdentityBindingEntity::getUserId, query.getUserId());
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
                .orderByDesc(ExternalIdentityBindingEntity::getBindTime))
                .stream()
                .map(this::toExternalIdentityVO)
                .toList();
    }

    /**
     * 从账号资料构造身份资料 VO。
     */
    private IdentityUserInfo buildIdentityUserInfo(IdentityUser user) {
        IdentityUserInfo userInfo = new IdentityUserInfo();
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

    private LambdaQueryWrapper<IdentityUser> buildManageableUserWrapper(IdentityUserPageQuery query) {
        LambdaQueryWrapper<IdentityUser> wrapper = new LambdaQueryWrapper<>();
        Set<Long> subjectIds = currentTenantSubjectIds(query.getStatus());
        if (query.getOrgId() != null) {
            Set<Long> orgUserIds = currentTenantOrgUserIds(query.getOrgId(), query.getStatus());
            subjectIds = subjectIds.stream()
                    .filter(orgUserIds::contains)
                    .collect(Collectors.toSet());
        }
        if (subjectIds.isEmpty()) {
            wrapper.eq(IdentityUser::getUserId, -1L);
        } else {
            wrapper.in(IdentityUser::getUserId, subjectIds);
        }
        wrapper.like(StringUtils.hasText(query.getUsername()), IdentityUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), IdentityUser::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getPhone()), IdentityUser::getPhone, query.getPhone())
                .like(StringUtils.hasText(query.getEmail()), IdentityUser::getEmail, query.getEmail())
                .and(StringUtils.hasText(query.getKeyword()), keyword -> keyword
                        .like(IdentityUser::getUsername, query.getKeyword())
                        .or()
                        .like(IdentityUser::getNickname, query.getKeyword())
                        .or()
                        .like(IdentityUser::getPhone, query.getKeyword())
                        .or()
                        .like(IdentityUser::getEmail, query.getKeyword()))
                .eq(StringUtils.hasText(query.getRealm()), IdentityUser::getRealm, query.getRealm())
                .eq(StringUtils.hasText(query.getActorType()), IdentityUser::getActorType, query.getActorType())
                .eq(StringUtils.hasText(query.getPartyType()), IdentityUser::getPartyType, query.getPartyType())
                .eq(query.getPartyId() != null, IdentityUser::getPartyId, query.getPartyId())
                .orderByDesc(IdentityUser::getCreateTime);
        return wrapper;
    }

    private IdentityUser getManageableUser(Long userId) {
        if (userId == null) {
            return null;
        }
        IdentityUser user = identityUserMapper.selectById(userId);
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

    private boolean belongsToCurrentTenant(IdentityUser user) {
        return user != null
                && StringUtils.hasText(user.getTenantId())
                && Objects.equals(user.getTenantId(), currentTenantId());
    }

    private Set<Long> currentTenantSubjectIds(Integer memberStatus) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null) {
            return Set.of();
        }
        LambdaQueryWrapper<TenantMember> wrapper = new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .isNull(TenantMember::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMember::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper)
                .stream()
                .map(TenantMember::getUserId)
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
        LambdaQueryWrapper<TenantMember> wrapper = new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .in(TenantMember::getMemberId, memberIds)
                .isNull(TenantMember::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMember::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper).stream()
                .map(TenantMember::getUserId)
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
        List<SubjectRoleBinding> bindings = subjectRoleBindingMapper.selectList(
                new LambdaQueryWrapper<SubjectRoleBinding>()
                        .eq(SubjectRoleBinding::getTenantId, tenantId)
                        .eq(SubjectRoleBinding::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                        .eq(SubjectRoleBinding::getRoleId, roleId));
        if (bindings == null || bindings.isEmpty()) {
            return Set.of();
        }
        Set<Long> memberIds = bindings.stream()
                .map(SubjectRoleBinding::getSubjectId)
                .collect(Collectors.toSet());
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
        LambdaQueryWrapper<TenantMember> wrapper = new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .in(TenantMember::getMemberId, memberIds)
                .isNull(TenantMember::getLeftAt);
        wrapper.eq(memberStatus != null, TenantMember::getStatus, memberStatus);
        return tenantMemberMapper.selectList(wrapper).stream()
                .map(TenantMember::getUserId)
                .collect(Collectors.toSet());
    }

    private List<IdentityUserInfo> listIdentityUserInfos(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return identityUserMapper.selectList(new LambdaQueryWrapper<IdentityUser>()
                        .in(IdentityUser::getUserId, userIds)
                        .eq(IdentityUser::getStatus, 1))
                .stream()
                .map(this::buildIdentityUserInfo)
                .toList();
    }

    private LambdaQueryWrapper<SubjectRoleBinding> currentTenantSubjectRoleWrapper(Collection<Long> subjectIds) {
        LambdaQueryWrapper<SubjectRoleBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBinding::getSubjectType, "TENANT_MEMBER")
                .in(SubjectRoleBinding::getSubjectId, subjectIds);
        Long tenantId = currentTenantIdLong();
        wrapper.eq(tenantId != null, SubjectRoleBinding::getTenantId, tenantId);
        return wrapper;
    }

    private IdentityUserVO toVO(IdentityUser user, Long queryOrgId) {
        IdentityUserVO vo = new IdentityUserVO();
        TenantMember member = currentTenantMember(user.getUserId());
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
        vo.setStatus(member != null ? member.getStatus() : user.getStatus());
        vo.setTenantId(user.getTenantId());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setRemark(user.getRemark());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    private void fillOrgRelation(IdentityUserVO vo, TenantMember member, Long queryOrgId) {
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

    private void createTenantMember(IdentityUser user, String displayName) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null) {
            throw new IllegalStateException("当前机构上下文无效");
        }
        TenantMember member = new TenantMember();
        member.setTenantId(tenantId);
        member.setUserId(user.getUserId());
        member.setMemberNo("USER-" + user.getUserId());
        member.setDisplayName(firstText(displayName, user.getUsername()));
        member.setMemberType("EMPLOYEE");
        member.setStatus(user.getStatus());
        member.setJoinedAt(LocalDateTime.now());
        member.setRemark(user.getRemark());
        tenantMemberMapper.insert(member);
    }

    private ExternalIdentityBindingEntity findExternalBinding(String provider, String corpId, String externalUserId,
                                                              Long tenantId) {
        if (tenantId == null || !StringUtils.hasText(provider) || !StringUtils.hasText(corpId)
                || !StringUtils.hasText(externalUserId)) {
            return null;
        }
        return externalIdentityBindingMapper.selectOne(new LambdaQueryWrapper<ExternalIdentityBindingEntity>()
                .eq(ExternalIdentityBindingEntity::getTenantId, tenantId)
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
        vo.setProvider(entity.getProvider());
        vo.setCorpId(entity.getCorpId());
        vo.setExternalUserId(entity.getExternalUserId());
        vo.setDisplayName(entity.getDisplayName());
        vo.setBindSource(entity.getBindSource());
        vo.setBindStatus(entity.getBindStatus());
        vo.setBindTime(entity.getBindTime());
        vo.setLastLoginTime(entity.getLastLoginTime());
        return vo;
    }

    private TenantMember currentTenantMember(Long userId) {
        Long tenantId = currentTenantIdLong();
        if (tenantId == null || userId == null) {
            return null;
        }
        return tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMember>()
                .eq(TenantMember::getTenantId, tenantId)
                .eq(TenantMember::getUserId, userId)
                .isNull(TenantMember::getLeftAt)
                .last("LIMIT 1"));
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : fallback;
    }

    private String normalizeRealm(String realm) {
        return realm == null || realm.isBlank() ? DEFAULT_REALM : realm.trim();
    }

    private String normalizeProvider(String provider) {
        return provider == null ? null : provider.trim().toUpperCase();
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
