package io.mango.permission.core.service.impl;

import io.mango.permission.core.entity.SysMenu;
import io.mango.permission.core.entity.SysMenuGroup;
import io.mango.permission.api.vo.SysMenuGroupVO;
import io.mango.permission.api.vo.SysMenuVO;
import io.mango.permission.core.mapper.SysMenuGroupMapper;
import io.mango.permission.core.mapper.SysMenuMapper;
import io.mango.permission.core.service.ISysMenuGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Menu Group Service implementation
 *
 * @author Mango
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuGroupServiceImpl implements ISysMenuGroupService {

    private final SysMenuGroupMapper menuGroupMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public List<SysMenuGroupVO> listMenuGroups() {
        // Get all enabled groups
        List<SysMenuGroup> groups = menuGroupMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenuGroup>()
                        .eq(SysMenuGroup::getStatus, 1)
                        .orderByAsc(SysMenuGroup::getSort)
        );

        // Get all menus for enabled groups
        List<SysMenu> allMenus = menuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSort)
        );

        // Group menus by groupId
        Map<Long, List<SysMenu>> menusByGroup = allMenus.stream()
                .filter(m -> m.getGroupId() != null)
                .collect(Collectors.groupingBy(SysMenu::getGroupId));

        // Build tree for each group
        return groups.stream().map(group -> {
            SysMenuGroupVO vo = convertToGroupVO(group);
            List<SysMenu> groupMenus = menusByGroup.getOrDefault(group.getGroupId(), new ArrayList<>());
            vo.setChildren(buildMenuTree(groupMenus, 0L));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SysMenuGroupVO getMenuGroup(Long groupId) {
        SysMenuGroup group = menuGroupMapper.selectById(groupId);
        if (group == null) {
            return null;
        }
        SysMenuGroupVO vo = convertToGroupVO(group);

        // Get menus for this group
        List<SysMenu> groupMenus = menuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getGroupId, groupId)
                        .eq(SysMenu::getStatus, 1)
                        .orderByAsc(SysMenu::getSort)
        );
        vo.setChildren(buildMenuTree(groupMenus, 0L));
        return vo;
    }

    @Override
    public Long createMenuGroup(SysMenuGroupVO menuGroupVO) {
        SysMenuGroup group = convertToGroupEntity(menuGroupVO);
        menuGroupMapper.insert(group);
        return group.getGroupId();
    }

    @Override
    public void updateMenuGroup(SysMenuGroupVO menuGroupVO) {
        SysMenuGroup group = convertToGroupEntity(menuGroupVO);
        menuGroupMapper.updateById(group);
    }

    @Override
    public void deleteMenuGroup(Long groupId) {
        // 删除分组下的所有菜单
        menuMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getGroupId, groupId)
        );
        // 删除分组
        menuGroupMapper.deleteById(groupId);
    }

    /**
     * Build menu tree structure
     */
    private List<SysMenuVO> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    SysMenuVO vo = convertToMenuVO(m);
                    vo.setChildren(buildMenuTree(menus, m.getMenuId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private SysMenuGroupVO convertToGroupVO(SysMenuGroup group) {
        SysMenuGroupVO vo = new SysMenuGroupVO();
        vo.setGroupId(group.getGroupId());
        vo.setGroupName(group.getGroupName());
        vo.setGroupCode(group.getGroupCode());
        vo.setIcon(group.getIcon());
        vo.setSort(group.getSort());
        vo.setStatus(group.getStatus());
        vo.setCreateBy(group.getCreateBy());
        vo.setUpdateBy(group.getUpdateBy());
        vo.setCreateTime(group.getCreateTime());
        vo.setUpdateTime(group.getUpdateTime());
        vo.setRemark(group.getRemark());
        return vo;
    }

    private SysMenuGroup convertToGroupEntity(SysMenuGroupVO vo) {
        SysMenuGroup group = new SysMenuGroup();
        group.setGroupId(vo.getGroupId());
        group.setGroupName(vo.getGroupName());
        group.setGroupCode(vo.getGroupCode());
        group.setIcon(vo.getIcon());
        group.setSort(vo.getSort());
        group.setStatus(vo.getStatus());
        group.setRemark(vo.getRemark());
        return group;
    }

    private SysMenuVO convertToMenuVO(SysMenu menu) {
        SysMenuVO vo = new SysMenuVO();
        vo.setMenuId(menu.getMenuId());
        vo.setGroupId(menu.getGroupId());
        vo.setParentId(menu.getParentId());
        vo.setMenuType(menu.getMenuType());
        vo.setMenuName(menu.getMenuName());
        vo.setMenuCode(menu.getMenuCode());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setStatus(menu.getStatus());
        vo.setVisible(menu.getVisible());
        vo.setKeepAlive(menu.getKeepAlive());
        vo.setEmbedded(menu.getEmbedded());
        vo.setRedirect(menu.getRedirect());
        vo.setPermissions(menu.getPermissions());
        vo.setCreateBy(menu.getCreateBy());
        vo.setUpdateBy(menu.getUpdateBy());
        vo.setCreateTime(menu.getCreateTime());
        vo.setUpdateTime(menu.getUpdateTime());
        vo.setRemark(menu.getRemark());
        return vo;
    }
}
