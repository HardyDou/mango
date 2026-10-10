package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.authorization.api.enums.AuthorizationCode;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.BatchRoleAssignmentCommand;
import io.mango.authorization.api.enums.BatchRoleTargetScope;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.authorization.api.vo.MenuVO;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.authorization.api.vo.SubjectRoleSummaryVO;
import io.mango.authorization.api.vo.BatchRoleAssignmentPreviewVO;
import io.mango.authorization.api.vo.BatchRoleAssignmentResultVO;
import io.mango.authorization.api.vo.BatchRoleMemberVO;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.vo.TenantMemberVO;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IMenuService;
import io.mango.authorization.core.service.IRoleService;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring singleton collaborators are intentionally injected and retained"))
public class RoleService implements IRoleService {

    private static final int MAX_SUBJECT_ROLE_BATCH_SIZE = 200;
    private static final int MAX_SUBJECT_ROLE_DELETE_SIZE = 10_000;

    private final RoleMapper roleMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final IMenuService menuService;
    private final ISubjectAuthorityService subjectAuthorityService;
    private final ObjectProvider<TenantMemberProvider> tenantMemberProvider;

    @Override
    public List<RoleVO> list() {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleEntity::getStatus, 1)
               .orderByAsc(RoleEntity::getSort);
        List<RoleEntity> roles = roleMapper.selectList(wrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RoleVO get(Long id) {
        RoleEntity role = roleMapper.selectById(id);
        Require.notNull(role, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "角色不存在");
        return toVO(role);
    }

    @Override
    public List<RoleVO> getSubjectRoles(Long subjectId) {
        LambdaQueryWrapper<SubjectRoleBindingEntity> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBindingEntity::getSubjectId, subjectId)
                .eq(SubjectRoleBindingEntity::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .eq(getTenantIdLong() != null, SubjectRoleBindingEntity::getTenantId, getTenantIdLong());
        List<SubjectRoleBindingEntity> userRoles = subjectRoleBindingMapper.selectList(urWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SubjectRoleBindingEntity::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<RoleEntity> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(RoleEntity::getId, roleIds);
        List<RoleEntity> roles = roleMapper.selectList(roleWrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SubjectRoleSummaryVO> getSubjectRolesBatch(List<Long> subjectIds) {
        Require.isTrue(subjectIds == null || subjectIds.size() <= MAX_SUBJECT_ROLE_BATCH_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "成员ID不能超过200个");
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        List<Long> requestedIds = subjectIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        Long tenantId = getTenantIdLong();
        String appCode = MangoContextHolder.appCode();
        LambdaQueryWrapper<SubjectRoleBindingEntity> bindingWrapper = new LambdaQueryWrapper<>();
        bindingWrapper.eq(SubjectRoleBindingEntity::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .in(SubjectRoleBindingEntity::getSubjectId, requestedIds)
                .eq(tenantId != null, SubjectRoleBindingEntity::getTenantId, tenantId)
                .eq(hasText(appCode), SubjectRoleBindingEntity::getAppCode, appCode);
        List<SubjectRoleBindingEntity> bindings = subjectRoleBindingMapper.selectList(bindingWrapper);

        Map<Long, RoleVO> rolesById = new LinkedHashMap<>();
        Set<Long> roleIds = bindings.stream()
                .map(SubjectRoleBindingEntity::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!roleIds.isEmpty()) {
            LambdaQueryWrapper<RoleEntity> roleWrapper = new LambdaQueryWrapper<RoleEntity>()
                    .in(RoleEntity::getId, roleIds)
                    .eq(tenantId != null, RoleEntity::getTenantId, tenantId)
                    .eq(hasText(appCode), RoleEntity::getAppCode, appCode)
                    .eq(RoleEntity::getStatus, 1)
                    .orderByAsc(RoleEntity::getSort)
                    .orderByAsc(RoleEntity::getId);
            roleMapper.selectList(roleWrapper).forEach(role -> rolesById.put(role.getRoleId(), toVO(role)));
        }

        Map<Long, Set<Long>> roleIdsBySubject = bindings.stream()
                .collect(Collectors.groupingBy(
                        SubjectRoleBindingEntity::getSubjectId,
                        LinkedHashMap::new,
                        Collectors.mapping(SubjectRoleBindingEntity::getRoleId,
                                Collectors.toCollection(LinkedHashSet::new))));
        return requestedIds.stream().map(subjectId -> {
            SubjectRoleSummaryVO summary = new SubjectRoleSummaryVO();
            summary.setSubjectId(subjectId);
            Set<Long> assignedRoleIds = roleIdsBySubject.getOrDefault(subjectId, Set.of());
            summary.setRoles(rolesById.entrySet().stream()
                    .filter(entry -> assignedRoleIds.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList());
            return summary;
        }).toList();
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        RoleEntity role = roleMapper.selectById(roleId);
        if (!isCurrentTenantRole(role)) {
            log.warn("Tenant isolation violation: attempt to query menus of role {} by tenant {}", roleId, getTenantIdLong());
            return new ArrayList<>();
        }
        LambdaQueryWrapper<RoleMenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuEntity::getRoleId, roleId);
        List<RoleMenuEntity> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(RoleMenuEntity::getMenuId).collect(Collectors.toList());
    }

    @Override
    public List<MenuVO> listAssignableMenus(String appCode) {
        List<MenuEntity> menus = listAssignableMenuEntities(appCode);
        return menuService.buildMenuTree(menus);
    }

    @Override
    @Transactional
    public Long create(RoleCommand po) {
        Require.notNull(po, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色命令不能为空");
        RoleEntity role = toEntity(po);
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role.getRoleId();
    }

    @Override
    @Transactional
    public Boolean update(RoleCommand po) {
        Require.notNull(po, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色命令不能为空");
        Require.notNull(po.getRoleId(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色ID不能为空");
        RoleEntity existing = roleMapper.selectById(po.getRoleId());
        Require.notNull(existing, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "角色不存在");
        // 租户隔离检查。
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantIdAsLong())) {
            log.warn("Tenant isolation violation: attempt to update role {} by tenant {}", po.getRoleId(), currentTenantId);
        }
        Require.isTrue(currentTenantId == null || currentTenantId.equals(existing.getTenantIdAsLong()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "无权修改该角色");
        existing.setAppCode(po.getAppCode());
        existing.setRealm(po.getRealm());
        existing.setActorType(po.getActorType());
        existing.setRoleCode(po.getRoleCode());
        existing.setRoleName(po.getRoleName());
        existing.setRoleType(po.getRoleType());
        existing.setStatus(po.getStatus());
        existing.setSort(po.getSort());
        existing.setRemark(po.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        boolean updated = roleMapper.updateById(existing) > 0;
        Require.isTrue(updated, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "角色不存在");
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Require.notNull(id, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色ID不能为空");
        // 租户隔离检查。
        RoleEntity existing = roleMapper.selectById(id);
        Require.notNull(existing, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "角色不存在");
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantIdAsLong())) {
            log.warn("Tenant isolation violation: attempt to delete role {} by tenant {}", id, currentTenantId);
        }
        Require.isTrue(currentTenantId == null || currentTenantId.equals(existing.getTenantIdAsLong()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "无权删除该角色");
        // 先删除角色菜单关系。
        LambdaQueryWrapper<RoleMenuEntity> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(RoleMenuEntity::getRoleId, id);
        roleMenuMapper.delete(rmWrapper);
        // 再删除主体角色关系。
        LambdaQueryWrapper<SubjectRoleBindingEntity> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBindingEntity::getRoleId, id);
        subjectRoleBindingMapper.delete(urWrapper);
        // 最后删除角色。
        boolean deleted = roleMapper.deleteById(id) > 0;
        Require.isTrue(deleted, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "角色不存在");
        return true;
    }

    @Override
    @Transactional
    public Boolean assignRoles(AssignSubjectRolesCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "成员角色分配命令不能为空");
        Long subjectId = command.getSubjectId();
        List<Long> roleIds = command.getRoleIds();
        boolean assignable = roleIds == null || roleIds.isEmpty() || areCurrentTenantRoles(roleIds);
        if (!assignable) {
            log.warn("Tenant isolation violation: attempt to assign roles {} by tenant {}", roleIds, getTenantIdLong());
            return false;
        }
        // 删除同一主体在同一上下文下的旧角色。
        LambdaQueryWrapper<SubjectRoleBindingEntity> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SubjectRoleBindingEntity::getSubjectId, subjectId)
                .eq(SubjectRoleBindingEntity::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .eq(hasText(command.getAppCode()), SubjectRoleBindingEntity::getAppCode, command.getAppCode())
                .eq(hasText(command.getRealm()), SubjectRoleBindingEntity::getRealm, command.getRealm())
                .eq(hasText(command.getActorType()), SubjectRoleBindingEntity::getActorType, command.getActorType())
                .eq(hasText(command.getPartyType()), SubjectRoleBindingEntity::getPartyType, command.getPartyType())
                .eq(command.getPartyId() != null, SubjectRoleBindingEntity::getPartyId, command.getPartyId());
        subjectRoleBindingMapper.delete(delWrapper);
        // 写入新的角色关系。
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SubjectRoleBindingEntity ur = new SubjectRoleBindingEntity();
                ur.setSubjectId(subjectId);
                ur.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
                ur.setAppCode(command.getAppCode());
                ur.setRealm(command.getRealm());
                ur.setActorType(command.getActorType());
                ur.setPartyType(command.getPartyType());
                ur.setPartyId(command.getPartyId());
                ur.setRoleId(roleId);
                subjectRoleBindingMapper.insert(ur);
            }
        }
        return true;
    }

    @Override
    public Boolean assignRolesRequired(AssignSubjectRolesCommand command) {
        boolean assigned = Boolean.TRUE.equals(assignRoles(command));
        Require.isTrue(assigned, AuthorizationCode.AUTHORIZATION_FORBIDDEN, "无权分配该角色");
        return true;
    }

    @Override
    public BatchRoleAssignmentPreviewVO previewBatchRoleAssignment(BatchRoleAssignmentCommand command) {
        BatchRoleContext context = resolveBatchRoleContext(command);
        List<TenantMemberVO> members = resolveBatchMembers(command);
        BatchRoleAssignmentPreviewVO preview = new BatchRoleAssignmentPreviewVO();
        preview.setTenantId(context.tenantId());
        preview.setRoleId(context.role().getRoleId());
        preview.setRoleCode(context.role().getRoleCode());
        preview.setRoleName(context.role().getRoleName());
        preview.setTargetScope(command.getTargetScope());
        preview.setTargetCount(members.size());
        preview.setMembers(members.stream().map(this::toBatchMember).toList());
        return preview;
    }

    @Override
    @Transactional
    public BatchRoleAssignmentResultVO assignBatchRole(BatchRoleAssignmentCommand command) {
        return changeBatchRole(command, true);
    }

    @Override
    @Transactional
    public BatchRoleAssignmentResultVO unassignBatchRole(BatchRoleAssignmentCommand command) {
        return changeBatchRole(command, false);
    }

    private BatchRoleAssignmentResultVO changeBatchRole(BatchRoleAssignmentCommand command, boolean assign) {
        BatchRoleContext context = resolveBatchRoleContext(command);
        ResolvedMembers resolved = resolveBatchMembersWithSkipped(command);
        int created = 0;
        int existing = 0;
        int removed = 0;
        for (TenantMemberVO member : resolved.members()) {
            LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = batchBindingWrapper(context, member.getMemberId());
            SubjectRoleBindingEntity binding = subjectRoleBindingMapper.selectOne(wrapper);
            if (assign) {
                if (binding != null) {
                    existing++;
                    continue;
                }
                SubjectRoleBindingEntity createdBinding = new SubjectRoleBindingEntity();
                createdBinding.setTenantId(context.tenantId());
                createdBinding.setSubjectId(member.getMemberId());
                createdBinding.setSubjectType(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
                createdBinding.setAppCode(context.appCode());
                createdBinding.setRealm(context.realm());
                createdBinding.setActorType(context.actorType());
                createdBinding.setPartyType(context.partyType());
                createdBinding.setPartyId(context.partyId());
                createdBinding.setRoleId(context.role().getRoleId());
                Require.isTrue(subjectRoleBindingMapper.insert(createdBinding) > 0,
                        AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "批量角色绑定写入失败");
                created++;
            } else if (binding != null) {
                removed += subjectRoleBindingMapper.deleteById(binding.getId());
            }
        }
        BatchRoleAssignmentResultVO result = new BatchRoleAssignmentResultVO();
        result.setRoleId(context.role().getRoleId());
        result.setRoleCode(context.role().getRoleCode());
        result.setTargetCount(resolved.members().size());
        result.setCreatedCount(created);
        result.setExistingCount(existing);
        result.setRemovedCount(removed);
        result.setSkippedCount(resolved.skippedSubjectIds().size());
        result.setSkippedSubjectIds(resolved.skippedSubjectIds());
        return result;
    }

    private BatchRoleContext resolveBatchRoleContext(BatchRoleAssignmentCommand command) {
        Require.notNull(command, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "批量角色绑定命令不能为空");
        Long tenantId = getTenantIdLong();
        Require.notNull(tenantId, AuthorizationCode.AUTHORIZATION_UNAUTHORIZED, "当前租户上下文无效");
        Require.notBlank(command.getRoleCode(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色编码不能为空");
        String appCode = textOr(command.getAppCode(), MangoContextHolder.appCode());
        String currentApp = MangoContextHolder.appCode();
        Require.isTrue(!hasText(currentApp) || !hasText(command.getAppCode()) || currentApp.equals(command.getAppCode().trim()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "不能跨应用执行批量角色绑定");
        Require.isTrue(!hasText(command.getRealm()) || command.getRealm().trim().equals(MangoContextHolder.get().realm()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "不能跨登录域执行批量角色绑定");
        Require.isTrue(!hasText(command.getActorType()) || command.getActorType().trim().equals(MangoContextHolder.get().actorType()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "不能跨操作者类型执行批量角色绑定");
        Require.isTrue(!hasText(command.getPartyType()) || command.getPartyType().trim().equals(MangoContextHolder.get().partyType()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "不能跨归属主体类型执行批量角色绑定");
        Require.isTrue(command.getPartyId() == null
                        || java.util.Objects.equals(command.getPartyId(), MangoContextHolder.get().partyId()),
                AuthorizationCode.AUTHORIZATION_FORBIDDEN, "不能跨归属主体执行批量角色绑定");
        String realm = textOr(command.getRealm(), MangoContextHolder.get().realm());
        String actorType = textOr(command.getActorType(), MangoContextHolder.get().actorType());
        String partyType = textOr(command.getPartyType(), MangoContextHolder.get().partyType());
        Long partyId = command.getPartyId() != null ? command.getPartyId() : MangoContextHolder.get().partyId();
        LambdaQueryWrapper<RoleEntity> roleWrapper = new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getTenantId, tenantId)
                .eq(RoleEntity::getRoleCode, command.getRoleCode().trim())
                .eq(hasText(appCode), RoleEntity::getAppCode, appCode)
                .eq(hasText(realm), RoleEntity::getRealm, realm)
                .eq(hasText(actorType), RoleEntity::getActorType, actorType)
                .eq(RoleEntity::getStatus, 1)
                .last("LIMIT 1");
        RoleEntity role = roleMapper.selectOne(roleWrapper);
        Require.notNull(role, AuthorizationCode.AUTHORIZATION_NOT_FOUND, "目标角色不存在或已停用");
        return new BatchRoleContext(tenantId, appCode, realm, actorType, partyType, partyId, role);
    }

    private List<TenantMemberVO> resolveBatchMembers(BatchRoleAssignmentCommand command) {
        return resolveBatchMembersWithSkipped(command).members();
    }

    private ResolvedMembers resolveBatchMembersWithSkipped(BatchRoleAssignmentCommand command) {
        Require.notNull(command.getTargetScope(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "目标范围不能为空");
        Long tenantId = getTenantIdLong();
        List<Long> requestedIds = command.getSubjectIds() == null ? List.of()
                : command.getSubjectIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        Require.isTrue(command.getTargetScope() != BatchRoleTargetScope.SUBJECT_IDS || requestedIds.size() <= 10_000,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "成员ID不能超过10000个");
        if (command.getTargetScope() == BatchRoleTargetScope.ORGANIZATION
                || command.getTargetScope() == BatchRoleTargetScope.ORGANIZATION_AND_DESCENDANTS) {
            Require.notNull(command.getOrgId(), AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "组织ID不能为空");
        }
        TenantMemberProvider provider = tenantMemberProvider.getIfAvailable();
        Require.notNull(provider, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "身份成员目录不可用");
        List<TenantMemberVO> members;
        switch (command.getTargetScope()) {
            case ALL_ENABLED_MEMBERS -> members = provider.listEnabledMembersByTenant(tenantId);
            case ORGANIZATION -> members = provider.listEnabledMembersByOrg(tenantId, command.getOrgId(), false);
            case ORGANIZATION_AND_DESCENDANTS -> members = provider.listEnabledMembersByOrg(tenantId, command.getOrgId(), true);
            case SUBJECT_IDS -> members = provider.listMembers(requestedIds);
            default -> members = List.of();
        }
        if (members == null) {
            members = List.of();
        }
        Map<Long, TenantMemberVO> validById = members.stream()
                .filter(member -> member != null && member.getMemberId() != null
                        && tenantId.equals(member.getTenantId())
                        && Integer.valueOf(1).equals(member.getStatus()))
                .collect(Collectors.toMap(TenantMemberVO::getMemberId, member -> member,
                        (first, ignored) -> first, LinkedHashMap::new));
        List<Long> skipped = requestedIds.stream().filter(id -> !validById.containsKey(id)).toList();
        return new ResolvedMembers(List.copyOf(validById.values()), skipped);
    }

    private LambdaQueryWrapper<SubjectRoleBindingEntity> batchBindingWrapper(BatchRoleContext context,
                                                                               Long subjectId) {
        return new LambdaQueryWrapper<SubjectRoleBindingEntity>()
                .eq(SubjectRoleBindingEntity::getTenantId, context.tenantId())
                .eq(SubjectRoleBindingEntity::getSubjectId, subjectId)
                .eq(SubjectRoleBindingEntity::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .eq(hasText(context.appCode()), SubjectRoleBindingEntity::getAppCode, context.appCode())
                .eq(hasText(context.realm()), SubjectRoleBindingEntity::getRealm, context.realm())
                .eq(hasText(context.actorType()), SubjectRoleBindingEntity::getActorType, context.actorType())
                .eq(hasText(context.partyType()), SubjectRoleBindingEntity::getPartyType, context.partyType())
                .eq(context.partyId() != null, SubjectRoleBindingEntity::getPartyId, context.partyId())
                .eq(SubjectRoleBindingEntity::getRoleId, context.role().getRoleId())
                .last("LIMIT 1");
    }

    private BatchRoleMemberVO toBatchMember(TenantMemberVO member) {
        BatchRoleMemberVO vo = new BatchRoleMemberVO();
        vo.setSubjectId(member.getMemberId());
        vo.setMemberNo(member.getMemberNo());
        vo.setDisplayName(member.getDisplayName());
        vo.setPrimaryOrgId(member.getPrimaryOrgId());
        return vo;
    }

    private String textOr(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private record BatchRoleContext(Long tenantId, String appCode, String realm, String actorType,
                                    String partyType, Long partyId, RoleEntity role) {
    }

    private record ResolvedMembers(List<TenantMemberVO> members, List<Long> skippedSubjectIds) {
    }

    @Override
    public Long findRoleId(RoleLookupQuery query) {
        if (query == null) {
            return null;
        }
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, RoleEntity::getTenantId, query.getTenantId())
                .eq(hasText(query.getAppCode()), RoleEntity::getAppCode, query.getAppCode())
                .eq(hasText(query.getRealm()), RoleEntity::getRealm, query.getRealm())
                .eq(hasText(query.getActorType()), RoleEntity::getActorType, query.getActorType())
                .eq(hasText(query.getRoleCode()), RoleEntity::getRoleCode, query.getRoleCode())
                .last("LIMIT 1");
        RoleEntity role = roleMapper.selectOne(wrapper);
        return role == null ? null : role.getRoleId();
    }

    @Override
    @Transactional
    public Boolean ensureSubjectRoleBinding(SubjectRoleBindingCommand command) {
        Require.isTrue(command == null || command.getSubjectId() == null || command.getSubjectId() > 0,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "主体ID必须为正数");
        if (command == null || command.getSubjectId() == null || command.getRoleId() == null
                || !hasText(command.getSubjectType())) {
            return false;
        }
        LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(command.getTenantId() != null, SubjectRoleBindingEntity::getTenantId, command.getTenantId())
                .eq(SubjectRoleBindingEntity::getSubjectType, command.getSubjectType())
                .eq(SubjectRoleBindingEntity::getSubjectId, command.getSubjectId())
                .eq(SubjectRoleBindingEntity::getRoleId, command.getRoleId())
                .eq(hasText(command.getAppCode()), SubjectRoleBindingEntity::getAppCode, command.getAppCode())
                .eq(hasText(command.getRealm()), SubjectRoleBindingEntity::getRealm, command.getRealm())
                .eq(hasText(command.getActorType()), SubjectRoleBindingEntity::getActorType, command.getActorType())
                .eq(hasText(command.getPartyType()), SubjectRoleBindingEntity::getPartyType, command.getPartyType())
                .eq(command.getPartyId() != null, SubjectRoleBindingEntity::getPartyId, command.getPartyId());
        Long count = subjectRoleBindingMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return true;
        }
        SubjectRoleBindingEntity binding = new SubjectRoleBindingEntity();
        binding.setTenantId(command.getTenantId());
        binding.setSubjectId(command.getSubjectId());
        binding.setSubjectType(command.getSubjectType());
        binding.setAppCode(command.getAppCode());
        binding.setRealm(command.getRealm());
        binding.setActorType(command.getActorType());
        binding.setPartyType(command.getPartyType());
        binding.setPartyId(command.getPartyId());
        binding.setRoleId(command.getRoleId());
        return subjectRoleBindingMapper.insert(binding) > 0;
    }

    @Override
    @Transactional
    public Integer deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command) {
        Require.isTrue(command == null || command.getSubjectIds() == null
                        || command.getSubjectIds().size() <= MAX_SUBJECT_ROLE_DELETE_SIZE,
                AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "一次删除的主体角色绑定不能超过10000条");
        if (command == null || !hasText(command.getSubjectType())
                || command.getSubjectIds() == null || command.getSubjectIds().isEmpty()) {
            return 0;
        }
        LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBindingEntity::getSubjectType, command.getSubjectType())
                .in(SubjectRoleBindingEntity::getSubjectId, command.getSubjectIds())
                .eq(command.getTenantId() != null, SubjectRoleBindingEntity::getTenantId, command.getTenantId());
        return subjectRoleBindingMapper.delete(wrapper);
    }

    @Override
    public List<Long> listSubjectIdsByRole(SubjectRoleBindingQuery query) {
        if (query == null || query.getRoleId() == null || !hasText(query.getSubjectType())) {
            return List.of();
        }
        LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getTenantId() != null, SubjectRoleBindingEntity::getTenantId, query.getTenantId())
                .eq(SubjectRoleBindingEntity::getSubjectType, query.getSubjectType())
                .eq(SubjectRoleBindingEntity::getRoleId, query.getRoleId())
                .eq(hasText(query.getAppCode()), SubjectRoleBindingEntity::getAppCode, query.getAppCode())
                .eq(hasText(query.getRealm()), SubjectRoleBindingEntity::getRealm, query.getRealm())
                .eq(hasText(query.getActorType()), SubjectRoleBindingEntity::getActorType, query.getActorType())
                .eq(hasText(query.getPartyType()), SubjectRoleBindingEntity::getPartyType, query.getPartyType())
                .eq(query.getPartyId() != null, SubjectRoleBindingEntity::getPartyId, query.getPartyId());
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBindingEntity::getSubjectId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean assignMenus(Long roleId, List<Long> menuIds) {
        Require.notNull(roleId, AuthorizationCode.AUTHORIZATION_BUSINESS_ERROR, "角色ID不能为空");
        // 校验角色归属当前租户。
        Long currentTenantId = getTenantIdLong();
        RoleEntity role = roleMapper.selectById(roleId);
        boolean currentTenantRole = isCurrentTenantRole(role);
        if (!currentTenantRole) {
            log.warn("Tenant isolation violation: attempt to assign menus to role {} by tenant {}", roleId, currentTenantId);
            return false;
        }

        Set<Long> requestedMenuIds = menuIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(menuIds);
        if (!requestedMenuIds.isEmpty()) {
            Set<Long> assignableMenuIds = listAssignableMenuEntities(role.getAppCode()).stream()
                    .map(MenuEntity::getMenuId)
                    .collect(Collectors.toCollection(HashSet::new));
            boolean assignable = assignableMenuIds.containsAll(requestedMenuIds);
            if (!assignable) {
                log.warn("Permission escalation denied: roleId={}, tenantId={}, requestedMenuIds={}",
                        roleId, currentTenantId, requestedMenuIds);
                return false;
            }
        }

        // 删除角色旧菜单关系。
        LambdaQueryWrapper<RoleMenuEntity> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RoleMenuEntity::getRoleId, roleId);
        roleMenuMapper.delete(delWrapper);
        // 写入新的菜单关系。
        if (!requestedMenuIds.isEmpty()) {
            for (Long menuId : requestedMenuIds) {
                RoleMenuEntity rm = new RoleMenuEntity();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        return true;
    }

    @Override
    public Boolean assignMenusRequired(Long roleId, List<Long> menuIds) {
        boolean assigned = Boolean.TRUE.equals(assignMenus(roleId, menuIds));
        Require.isTrue(assigned, AuthorizationCode.AUTHORIZATION_FORBIDDEN, "无权分配该菜单权限");
        return true;
    }

    private boolean isCurrentTenantRole(RoleEntity role) {
        if (role == null) {
            return false;
        }
        Long currentTenantId = getTenantIdLong();
        return currentTenantId == null || currentTenantId.equals(role.getTenantIdAsLong());
    }

    private boolean areCurrentTenantRoles(List<Long> roleIds) {
        Long currentTenantId = getTenantIdLong();
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(RoleEntity::getId, roleIds)
                .eq(currentTenantId != null, RoleEntity::getTenantId, currentTenantId);
        long matchedCount = roleMapper.selectList(wrapper)
                .stream()
                .map(RoleEntity::getRoleId)
                .collect(Collectors.toSet())
                .size();
        return matchedCount == new HashSet<>(roleIds).size();
    }

    private List<MenuEntity> listAssignableMenuEntities(String appCode) {
        String effectiveAppCode = StringUtils.hasText(appCode) ? appCode : MangoContextHolder.appCode();
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(effectiveAppCode), MenuEntity::getAppCode, effectiveAppCode)
                .eq(MenuEntity::getStatus, 1)
                .in(MenuEntity::getMenuType, List.of(1, 2))
                .orderByAsc(MenuEntity::getSort);
        List<MenuEntity> allMenus = menuMapper.selectList(wrapper);
        if (allMenus.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> allMenuIds = allMenus.stream()
                .map(MenuEntity::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
        allMenus = allMenus.stream()
                .filter(menu -> menu.getParentId() == null
                        || menu.getParentId() == 0
                        || allMenuIds.contains(menu.getParentId()))
                .collect(Collectors.toList());
        if (allMenus.isEmpty()) {
            return new ArrayList<>();
        }

        AuthorizationQuery query = currentAuthorizationQuery(effectiveAppCode);
        Set<String> permissions = subjectAuthorityService.listSubjectPermissions(query)
                .stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (permissions.contains("*:*")) {
            return allMenus;
        }

        Set<Long> assignedMenuIds = listCurrentSubjectRoleMenuIds(effectiveAppCode);
        if (assignedMenuIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, MenuEntity> menuById = allMenus.stream()
                .collect(Collectors.toMap(MenuEntity::getMenuId, menu -> menu));
        Set<Long> assignableIds = new LinkedHashSet<>();
        for (Long menuId : assignedMenuIds) {
            collectMenuWithAncestors(menuId, menuById, assignableIds);
        }
        return allMenus.stream()
                .filter(menu -> assignableIds.contains(menu.getMenuId()))
                .collect(Collectors.toList());
    }

    private AuthorizationQuery currentAuthorizationQuery(String appCode) {
        return AuthorizationQuery.member(MangoContextHolder.memberId())
                .withTenantId(MangoContextHolder.tenantId())
                .withSystemCode(appCode)
                .withRealm(MangoContextHolder.get().realm())
                .withActorType(MangoContextHolder.get().actorType())
                .withParty(MangoContextHolder.get().partyType(), MangoContextHolder.get().partyId());
    }

    private Set<Long> listCurrentSubjectRoleMenuIds(String appCode) {
        LambdaQueryWrapper<SubjectRoleBindingEntity> subjectRoleWrapper = new LambdaQueryWrapper<>();
        subjectRoleWrapper.eq(SubjectRoleBindingEntity::getSubjectType, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER)
                .eq(SubjectRoleBindingEntity::getSubjectId, MangoContextHolder.memberId())
                .eq(StringUtils.hasText(MangoContextHolder.tenantId()), SubjectRoleBindingEntity::getTenantId, getTenantIdLong())
                .eq(StringUtils.hasText(appCode), SubjectRoleBindingEntity::getAppCode, appCode)
                .eq(StringUtils.hasText(MangoContextHolder.get().realm()), SubjectRoleBindingEntity::getRealm, MangoContextHolder.get().realm())
                .eq(StringUtils.hasText(MangoContextHolder.get().actorType()), SubjectRoleBindingEntity::getActorType, MangoContextHolder.get().actorType())
                .eq(StringUtils.hasText(MangoContextHolder.get().partyType()), SubjectRoleBindingEntity::getPartyType, MangoContextHolder.get().partyType())
                .eq(MangoContextHolder.get().partyId() != null, SubjectRoleBindingEntity::getPartyId, MangoContextHolder.get().partyId());
        List<Long> roleIds = subjectRoleBindingMapper.selectList(subjectRoleWrapper)
                .stream()
                .map(SubjectRoleBindingEntity::getRoleId)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }

        LambdaQueryWrapper<RoleMenuEntity> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenuEntity::getRoleId, roleIds);
        return roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void collectMenuWithAncestors(Long menuId, Map<Long, MenuEntity> menuById, Set<Long> collector) {
        Long currentId = menuId;
        while (currentId != null && currentId > 0 && collector.add(currentId)) {
            MenuEntity menu = menuById.get(currentId);
            if (menu == null) {
                break;
            }
            currentId = menu.getParentId();
        }
    }

    private RoleVO toVO(RoleEntity role) {
        RoleVO vo = new RoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setTenantId(role.getTenantIdAsLong());
        vo.setAppCode(role.getAppCode());
        vo.setRealm(role.getRealm());
        vo.setActorType(role.getActorType());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleType(role.getRoleType());
        vo.setStatus(role.getStatus());
        vo.setSort(role.getSort());
        vo.setRemark(role.getRemark());
        vo.setCreateTime(role.getCreateTime());
        vo.setUpdateTime(role.getUpdateTime());
        return vo;
    }

    private RoleEntity toEntity(RoleCommand po) {
        RoleEntity role = new RoleEntity();
        role.setRoleId(po.getRoleId());
        role.setAppCode(po.getAppCode());
        role.setRealm(po.getRealm());
        role.setActorType(po.getActorType());
        role.setRoleCode(po.getRoleCode());
        role.setRoleName(po.getRoleName());
        role.setRoleType(po.getRoleType());
        role.setStatus(po.getStatus() != null ? po.getStatus() : 1);
        role.setSort(po.getSort() != null ? po.getSort() : 0);
        role.setRemark(po.getRemark());
        return role;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long getTenantIdLong() {
        String tenantIdStr = MangoContextHolder.tenantId();
        if (tenantIdStr == null || tenantIdStr.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
