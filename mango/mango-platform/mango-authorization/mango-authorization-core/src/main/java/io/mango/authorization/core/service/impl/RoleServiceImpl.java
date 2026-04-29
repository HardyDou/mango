package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.command.AssignSubjectRolesCommand;
import io.mango.authorization.api.command.RoleCommand;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.authorization.core.entity.Role;
import io.mango.authorization.core.entity.RoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.RoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.IRoleService;
import io.mango.infra.context.core.MangoContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {

    private final RoleMapper roleMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final RoleMenuMapper roleMenuMapper;

    @Override
    public List<RoleVO> list() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1)
               .orderByAsc(Role::getSort);
        List<Role> roles = roleMapper.selectList(wrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RoleVO get(Long id) {
        Role role = roleMapper.selectById(id);
        return role != null ? toVO(role) : null;
    }

    @Override
    public List<RoleVO> getSubjectRoles(Long subjectId) {
        LambdaQueryWrapper<SubjectRoleBinding> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBinding::getSubjectId, subjectId);
        List<SubjectRoleBinding> userRoles = subjectRoleBindingMapper.selectList(urWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SubjectRoleBinding::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<Role> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(Role::getRoleId, roleIds);
        List<Role> roles = roleMapper.selectList(roleWrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(RoleCommand po) {
        Role role = toEntity(po);
        role.setTenantId(getTenantIdLong());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role.getRoleId();
    }

    @Override
    @Transactional
    public Boolean update(RoleCommand po) {
        if (po.getRoleId() == null) {
            return false;
        }
        Role existing = roleMapper.selectById(po.getRoleId());
        if (existing == null) {
            return false;
        }
        // 租户隔离检查。
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            log.warn("Tenant isolation violation: attempt to update role {} by tenant {}", po.getRoleId(), currentTenantId);
            return false;
        }
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
        return roleMapper.updateById(existing) > 0;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        // 租户隔离检查。
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            log.warn("Tenant isolation violation: attempt to delete role {} by tenant {}", id, currentTenantId);
            return false;
        }
        // 先删除角色菜单关系。
        LambdaQueryWrapper<RoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(RoleMenu::getRoleId, id);
        roleMenuMapper.delete(rmWrapper);
        // 再删除主体角色关系。
        LambdaQueryWrapper<SubjectRoleBinding> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBinding::getRoleId, id);
        subjectRoleBindingMapper.delete(urWrapper);
        // 最后删除角色。
        return roleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public Boolean assignRoles(AssignSubjectRolesCommand command) {
        Long subjectId = command.getSubjectId();
        List<Long> roleIds = command.getRoleIds();
        // 删除同一主体在同一上下文下的旧角色。
        LambdaQueryWrapper<SubjectRoleBinding> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SubjectRoleBinding::getSubjectId, subjectId)
                .eq(hasText(command.getAppCode()), SubjectRoleBinding::getAppCode, command.getAppCode())
                .eq(hasText(command.getRealm()), SubjectRoleBinding::getRealm, command.getRealm())
                .eq(hasText(command.getActorType()), SubjectRoleBinding::getActorType, command.getActorType())
                .eq(hasText(command.getPartyType()), SubjectRoleBinding::getPartyType, command.getPartyType())
                .eq(command.getPartyId() != null, SubjectRoleBinding::getPartyId, command.getPartyId());
        subjectRoleBindingMapper.delete(delWrapper);
        // 写入新的角色关系。
        if (roleIds != null && !roleIds.isEmpty()) {
            Long tenantId = getTenantIdLong();
            for (Long roleId : roleIds) {
                SubjectRoleBinding ur = new SubjectRoleBinding();
                ur.setTenantId(tenantId);
                ur.setSubjectId(subjectId);
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
    @Transactional
    public Boolean assignMenus(Long roleId, List<Long> menuIds) {
        // 校验角色归属当前租户。
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null) {
            Role role = roleMapper.selectById(roleId);
            if (role == null || !currentTenantId.equals(role.getTenantId())) {
                log.warn("Tenant isolation violation: attempt to assign menus to role {} by tenant {}", roleId, currentTenantId);
                return false;
            }
        }
        // 删除角色旧菜单关系。
        LambdaQueryWrapper<RoleMenu> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(delWrapper);
        // 写入新的菜单关系。
        if (menuIds != null && !menuIds.isEmpty()) {
            Long tenantId = getTenantIdLong();
            for (Long menuId : menuIds) {
                RoleMenu rm = new RoleMenu();
                rm.setTenantId(tenantId);
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        return true;
    }

    private RoleVO toVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setTenantId(role.getTenantId());
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

    private Role toEntity(RoleCommand po) {
        Role role = new Role();
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
