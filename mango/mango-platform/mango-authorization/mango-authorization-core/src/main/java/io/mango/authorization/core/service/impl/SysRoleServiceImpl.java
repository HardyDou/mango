package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.infra.context.core.TenantContextHolder;
import io.mango.authorization.api.po.SysRolePo;
import io.mango.authorization.api.vo.SysRoleVO;
import io.mango.authorization.core.entity.SysRole;
import io.mango.authorization.core.entity.SysRoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.SysRoleMapper;
import io.mango.authorization.core.mapper.SysRoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * System role service implementation.
 * Uses mappers from permission-core directly.
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    @Override
    public List<SysRoleVO> list() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1)
               .orderByAsc(SysRole::getSort);
        List<SysRole> roles = roleMapper.selectList(wrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SysRoleVO get(Long id) {
        SysRole role = roleMapper.selectById(id);
        return role != null ? toVO(role) : null;
    }

    @Override
    public List<SysRoleVO> getSubjectRoles(Long subjectId) {
        LambdaQueryWrapper<SubjectRoleBinding> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBinding::getSubjectId, subjectId);
        List<SubjectRoleBinding> userRoles = subjectRoleBindingMapper.selectList(urWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream()
                .map(SubjectRoleBinding::getRoleId)
                .collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds);
        List<SysRole> roles = roleMapper.selectList(roleWrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(SysRolePo po) {
        SysRole role = toEntity(po);
        role.setTenantId(getTenantIdLong());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role.getRoleId();
    }

    @Override
    @Transactional
    public Boolean update(SysRolePo po) {
        if (po.getRoleId() == null) {
            return false;
        }
        SysRole existing = roleMapper.selectById(po.getRoleId());
        if (existing == null) {
            return false;
        }
        // Tenant isolation check
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            log.warn("Tenant isolation violation: attempt to update role {} by tenant {}", po.getRoleId(), currentTenantId);
            return false;
        }
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
        // Tenant isolation check
        SysRole existing = roleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            log.warn("Tenant isolation violation: attempt to delete role {} by tenant {}", id, currentTenantId);
            return false;
        }
        // Delete role-menu relationships first
        LambdaQueryWrapper<SysRoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(SysRoleMenu::getRoleId, id);
        roleMenuMapper.delete(rmWrapper);
        // Delete subject-role relationships
        LambdaQueryWrapper<SubjectRoleBinding> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SubjectRoleBinding::getRoleId, id);
        subjectRoleBindingMapper.delete(urWrapper);
        // Delete role
        return roleMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public Boolean assignRoles(Long subjectId, List<Long> roleIds) {
        // Remove existing roles for subject
        LambdaQueryWrapper<SubjectRoleBinding> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SubjectRoleBinding::getSubjectId, subjectId);
        subjectRoleBindingMapper.delete(delWrapper);
        // Add new role assignments
        if (roleIds != null && !roleIds.isEmpty()) {
            Long tenantId = getTenantIdLong();
            for (Long roleId : roleIds) {
                SubjectRoleBinding ur = new SubjectRoleBinding();
                ur.setTenantId(tenantId);
                ur.setSubjectId(subjectId);
                ur.setRoleId(roleId);
                subjectRoleBindingMapper.insert(ur);
            }
        }
        return true;
    }

    @Override
    @Transactional
    public Boolean assignMenus(Long roleId, List<Long> menuIds) {
        // Tenant isolation: verify role belongs to current tenant
        Long currentTenantId = getTenantIdLong();
        if (currentTenantId != null) {
            SysRole role = roleMapper.selectById(roleId);
            if (role == null || !currentTenantId.equals(role.getTenantId())) {
                log.warn("Tenant isolation violation: attempt to assign menus to role {} by tenant {}", roleId, currentTenantId);
                return false;
            }
        }
        // Remove existing menus for role
        LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(delWrapper);
        // Add new menu assignments
        if (menuIds != null && !menuIds.isEmpty()) {
            Long tenantId = getTenantIdLong();
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setTenantId(tenantId);
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
        return true;
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setRoleId(role.getRoleId());
        vo.setTenantId(role.getTenantId());
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

    private SysRole toEntity(SysRolePo po) {
        SysRole role = new SysRole();
        role.setRoleId(po.getRoleId());
        role.setRoleCode(po.getRoleCode());
        role.setRoleName(po.getRoleName());
        role.setRoleType(po.getRoleType());
        role.setStatus(po.getStatus() != null ? po.getStatus() : 1);
        role.setSort(po.getSort() != null ? po.getSort() : 0);
        role.setRemark(po.getRemark());
        return role;
    }

    private Long getTenantIdLong() {
        String tenantIdStr = TenantContextHolder.getTenantId();
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
