package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.vo.ButtonDisplayRuleVO;
import io.mango.authorization.core.entity.MenuEntity;
import io.mango.authorization.core.entity.RoleEntity;
import io.mango.authorization.core.entity.RoleMenuEntity;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于授权关系表查询主体角色和权限。
 */
@Service
@RequiredArgsConstructor
public class SubjectAuthorityService implements ISubjectAuthorityService {

    public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
    public static final String ROLE_LOGIN = "ROLE_LOGIN";

    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    @Override
    public List<String> listSubjectRoles(Long subjectId) {
        return listSubjectRoles(subjectId, null);
    }

    @Override
    public List<String> listSubjectRoles(Long subjectId, String appCode) {
        return listSubjectRoles(new AuthorizationQuery(
                subjectId, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER, null, appCode));
    }

    @Override
    public List<String> listSubjectRoles(AuthorizationQuery query) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<RoleEntity> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(RoleEntity::getId, roleIds)
                .eq(StringUtils.hasText(query.systemCode()), RoleEntity::getAppCode, query.systemCode())
                .eq(RoleEntity::getStatus, 1);
        return roleMapper.selectList(roleWrapper)
                .stream()
                .map(RoleEntity::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listSubjectPermissions(Long subjectId) {
        return listSubjectPermissions(subjectId, null);
    }

    @Override
    public List<String> listSubjectPermissions(Long subjectId, String appCode) {
        return listSubjectPermissions(new AuthorizationQuery(
                subjectId, AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER, null, appCode));
    }

    @Override
    public List<String> listSubjectPermissions(AuthorizationQuery query) {
        List<Long> menuIds = listSubjectMenuIds(query);
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<MenuEntity> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuEntity::getId, menuIds)
                .eq(StringUtils.hasText(query.systemCode()), MenuEntity::getAppCode, query.systemCode())
                .eq(MenuEntity::getStatus, 1);
        return menuMapper.selectList(menuWrapper)
                .stream()
                .flatMap(menu -> permissionCodes(menu).stream())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<ButtonDisplayRuleVO> listSubjectButtonRules(AuthorizationQuery query) {
        List<Long> menuIds = listSubjectMenuIds(query);
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<MenuEntity> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(MenuEntity::getId, menuIds)
                .eq(StringUtils.hasText(query.systemCode()), MenuEntity::getAppCode, query.systemCode())
                .eq(MenuEntity::getMenuType, 3)
                .eq(MenuEntity::getStatus, 1);
        return menuMapper.selectList(menuWrapper)
                .stream()
                .filter(menu -> StringUtils.hasText(menu.getMenuCode()))
                .map(this::toButtonDisplayRule)
                .collect(Collectors.toList());
    }

    private List<Long> listSubjectMenuIds(AuthorizationQuery query) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<RoleMenuEntity> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenuEntity::getRoleId, roleIds);
        return roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenuEntity::getMenuId)
                .distinct()
                .collect(Collectors.toList());
    }

    private ButtonDisplayRuleVO toButtonDisplayRule(MenuEntity menu) {
        ButtonDisplayRuleVO rule = new ButtonDisplayRuleVO();
        rule.setCode(menu.getMenuCode());
        rule.setButtonType(menu.getButtonType());
        rule.setDisplayRule(menu.getButtonDisplayRule());
        return rule;
    }

    private List<String> permissionCodes(MenuEntity menu) {
        if (menu == null) {
            return new ArrayList<>();
        }
        if (StringUtils.hasText(menu.getApiCodes())) {
            return Arrays.stream(menu.getApiCodes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private List<Long> listSubjectRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        if (StringUtils.hasText(query.tenantId()) && tenantId == null) {
            return new ArrayList<>();
        }
        Set<Long> roleIds = new LinkedHashSet<>();
        if (!AuthorizationQuery.SUBJECT_TYPE_ANONYMOUS.equals(query.subjectType())) {
            LambdaQueryWrapper<SubjectRoleBindingEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SubjectRoleBindingEntity::getSubjectId, query.subjectId())
                    .eq(SubjectRoleBindingEntity::getSubjectType, query.subjectType())
                    .eq(tenantId != null, SubjectRoleBindingEntity::getTenantId, tenantId)
                    .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBindingEntity::getAppCode, query.systemCode())
                    .eq(StringUtils.hasText(query.realm()), SubjectRoleBindingEntity::getRealm, query.realm())
                    .eq(StringUtils.hasText(query.actorType()), SubjectRoleBindingEntity::getActorType, query.actorType())
                    .eq(StringUtils.hasText(query.partyType()), SubjectRoleBindingEntity::getPartyType, query.partyType())
                    .eq(query.partyId() != null, SubjectRoleBindingEntity::getPartyId, query.partyId());
            roleIds.addAll(subjectRoleBindingMapper.selectList(wrapper)
                    .stream()
                    .map(SubjectRoleBindingEntity::getRoleId)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        roleIds.addAll(listDefaultRoleIds(query));
        return new ArrayList<>(roleIds);
    }

    private List<Long> listDefaultRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        String roleCode = AuthorizationQuery.SUBJECT_TYPE_ANONYMOUS.equals(query.subjectType())
                ? ROLE_ANONYMOUS
                : ROLE_LOGIN;
        wrapper.eq(RoleEntity::getRoleCode, roleCode)
                .eq(RoleEntity::getStatus, 1)
                .eq(tenantId != null, RoleEntity::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), RoleEntity::getAppCode, query.systemCode());
        return roleMapper.selectList(wrapper)
                .stream()
                .map(RoleEntity::getRoleId)
                .collect(Collectors.toList());
    }

    private Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
