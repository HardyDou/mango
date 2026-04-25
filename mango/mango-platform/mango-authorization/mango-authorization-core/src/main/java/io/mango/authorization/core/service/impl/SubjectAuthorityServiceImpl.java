package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.core.entity.SysMenu;
import io.mango.authorization.core.entity.SysRole;
import io.mango.authorization.core.entity.SysRoleMenu;
import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.authorization.core.mapper.SysMenuMapper;
import io.mango.authorization.core.mapper.SysRoleMapper;
import io.mango.authorization.core.mapper.SysRoleMenuMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.authorization.core.service.ISubjectAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Role and permission lookup backed by authorization bindings.
 */
@Service
@RequiredArgsConstructor
public class SubjectAuthorityServiceImpl implements ISubjectAuthorityService {

    private final SubjectRoleBindingMapper subjectRoleBindingMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<String> listSubjectRoles(Long subjectId) {
        List<Long> roleIds = listSubjectRoleIds(subjectId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(SysRole::getRoleId, roleIds)
                .eq(SysRole::getStatus, 1);
        return sysRoleMapper.selectList(roleWrapper)
                .stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listSubjectPermissions(Long subjectId) {
        List<Long> roleIds = listSubjectRoleIds(subjectId);
        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(SysRoleMenu::getRoleId, roleIds);
        List<Long> menuIds = sysRoleMenuMapper.selectList(roleMenuWrapper)
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SysMenu::getMenuId, menuIds)
                .eq(SysMenu::getMenuType, 3)
                .eq(SysMenu::getStatus, 1);
        return sysMenuMapper.selectList(menuWrapper)
                .stream()
                .map(SysMenu::getMenuCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toList());
    }

    private List<Long> listSubjectRoleIds(Long subjectId) {
        LambdaQueryWrapper<SubjectRoleBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SubjectRoleBinding::getSubjectId, subjectId);
        return subjectRoleBindingMapper.selectList(wrapper)
                .stream()
                .map(SubjectRoleBinding::getRoleId)
                .collect(Collectors.toList());
    }
}
