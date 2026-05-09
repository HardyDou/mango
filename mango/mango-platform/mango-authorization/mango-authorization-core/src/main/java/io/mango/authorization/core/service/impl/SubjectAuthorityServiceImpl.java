package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.core.entity.Menu;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.MenuMapper;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于授权关系表查询主体角色和权限。
 */
@Service
@RequiredArgsConstructor
public class SubjectAuthorityServiceImpl implements ISubjectAuthorityService {

    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMapper roleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;

    @Override
    public List<String> listSubjectRoles(AuthorizationQuery query) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(Role::getRoleId, roleIds)
                .eq(StringUtils.hasText(query.systemCode()), Role::getAppCode, query.systemCode())
                .eq(Role::getStatus, 1);
        return roleMapper.selectList(roleWrapper)
                .stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listSubjectPermissions(AuthorizationQuery query) {
        List<Long> roleIds = listSubjectRoleIds(query);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenu::getRoleId, roleIds);
        List<Long> menuIds = roleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<Menu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(Menu::getMenuId, menuIds)
                .eq(StringUtils.hasText(query.systemCode()), Menu::getAppCode, query.systemCode())
                .eq(Menu::getMenuType, 3)
                .eq(Menu::getStatus, 1);
        return menuMapper.selectList(menuWrapper)
                .stream()
                .map(Menu::getMenuCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList());
    }

    private List<Long> listSubjectRoleIds(AuthorizationQuery query) {
        Long tenantId = parseTenantId(query.tenantId());
        if (StringUtils.hasText(query.tenantId()) && tenantId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SubjectRoleBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBinding::getSubjectId, query.subjectId())
                .eq(SubjectRoleBinding::getSubjectType, query.subjectType())
                .eq(tenantId != null, SubjectRoleBinding::getTenantId, tenantId)
                .eq(StringUtils.hasText(query.systemCode()), SubjectRoleBinding::getAppCode, query.systemCode())
                .eq(StringUtils.hasText(query.realm()), SubjectRoleBinding::getRealm, query.realm())
                .eq(StringUtils.hasText(query.actorType()), SubjectRoleBinding::getActorType, query.actorType())
                .eq(StringUtils.hasText(query.partyType()), SubjectRoleBinding::getPartyType, query.partyType())
                .eq(query.partyId() != null, SubjectRoleBinding::getPartyId, query.partyId());
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBinding::getRoleId)
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
