package io.mango.auth.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.mango.auth.api.po.SysRolePo;
import io.mango.auth.api.vo.SysRoleVO;
import io.mango.auth.core.service.ISysRoleService;
import io.mango.common.exception.BizException;
import io.mango.permission.core.entity.SysRole;
import io.mango.permission.core.entity.SysRoleMenu;
import io.mango.permission.core.entity.SysUserRole;
import io.mango.permission.core.mapper.SysRoleMapper;
import io.mango.permission.core.mapper.SysRoleMenuMapper;
import io.mango.permission.core.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
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
        if (role == null) {
            throw new BizException("角色不存在");
        }
        return toVO(role);
    }

    @Override
    @Transactional
    public Long create(SysRolePo po) {
        SysRole role = new SysRole();
        role.setRoleId(System.currentTimeMillis());
        role.setRoleCode(po.getRoleCode());
        role.setRoleName(po.getRoleName());
        role.setRoleType(po.getRoleType() != null ? po.getRoleType() : 1);
        role.setStatus(po.getStatus() != null ? po.getStatus() : 1);
        role.setSort(po.getSort() != null ? po.getSort() : 0);
        role.setRemark(po.getRemark());
        roleMapper.insert(role);
        return role.getRoleId();
    }

    @Override
    @Transactional
    public Boolean update(SysRolePo po) {
        if (po.getRoleId() == null) {
            throw new BizException("角色ID不能为空");
        }
        SysRole role = roleMapper.selectById(po.getRoleId());
        if (role == null) {
            throw new BizException("角色不存在");
        }
        if (po.getRoleCode() != null) role.setRoleCode(po.getRoleCode());
        if (po.getRoleName() != null) role.setRoleName(po.getRoleName());
        if (po.getRoleType() != null) role.setRoleType(po.getRoleType());
        if (po.getStatus() != null) role.setStatus(po.getStatus());
        if (po.getSort() != null) role.setSort(po.getSort());
        if (po.getRemark() != null) role.setRemark(po.getRemark());
        roleMapper.updateById(role);
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        // 逻辑删除
        LambdaUpdateWrapper<SysRole> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysRole::getRoleId, id)
               .set(SysRole::getStatus, 0);
        roleMapper.update(null, wrapper);
        return true;
    }

    @Override
    public List<SysRoleVO> getUserRoles(Long userId) {
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, userId);
        List<SysUserRole> userRoles = userRoleMapper.selectList(urWrapper);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds);
        List<SysRole> roles = roleMapper.selectList(roleWrapper);
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Boolean assignRoles(Long userId, List<Long> roleIds) {
        // 删除现有用户-角色关联
        LambdaQueryWrapper<SysUserRole> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysUserRole::getUserId, userId);
        userRoleMapper.delete(delWrapper);
        // 插入新关联
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
        return true;
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
    public Boolean assignMenus(Long roleId, List<Long> menuIds) {
        // 删除现有角色-菜单关联
        LambdaQueryWrapper<SysRoleMenu> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(SysRoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(delWrapper);
        // 插入新关联
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
        return true;
    }

    private SysRoleVO toVO(SysRole role) {
        SysRoleVO vo = new SysRoleVO();
        vo.setRoleId(role.getRoleId());
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
}
