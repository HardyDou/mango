package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.authorization.api.vo.SysMenuGroupVO;
import io.mango.authorization.api.vo.SysMenuVO;
import io.mango.authorization.core.entity.SysMenu;
import io.mango.authorization.core.entity.SysMenuGroup;
import io.mango.authorization.core.mapper.SysMenuGroupMapper;
import io.mango.authorization.core.mapper.SysMenuMapper;
import io.mango.authorization.core.service.ISysMenuGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SysMenuGroupServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysMenuGroupServiceImpl Tests")
class SysMenuGroupServiceImplTest {

    @Mock
    private SysMenuGroupMapper menuGroupMapper;

    @Mock
    private SysMenuMapper menuMapper;

    private SysMenuGroupServiceImpl sysMenuGroupService;

    @BeforeEach
    void setUp() {
        sysMenuGroupService = new SysMenuGroupServiceImpl(menuGroupMapper, menuMapper);
    }

    @Test
    @DisplayName("getMenuGroup should return null when group not found")
    void getMenuGroup_nonExistingGroup_returnsNull() {
        when(menuGroupMapper.selectById(999L)).thenReturn(null);

        SysMenuGroupVO result = sysMenuGroupService.getMenuGroup(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("createMenuGroup should insert and return groupId")
    void createMenuGroup_validVO_insertsAndReturnsId() {
        SysMenuGroupVO vo = new SysMenuGroupVO();
        vo.setGroupName("Test Group");
        vo.setGroupCode("test:group");
        vo.setSort(1);
        vo.setStatus(1);
        when(menuGroupMapper.insert(any(SysMenuGroup.class))).thenAnswer(invocation -> {
            SysMenuGroup group = invocation.getArgument(0);
            group.setGroupId(1L); // Simulate ID generation
            return 1;
        });

        Long result = sysMenuGroupService.createMenuGroup(vo);

        assertNotNull(result);
        verify(menuGroupMapper).insert(any(SysMenuGroup.class));
    }

    @Test
    @DisplayName("updateMenuGroup should call updateById")
    void updateMenuGroup_validVO_updatesGroup() {
        SysMenuGroupVO vo = new SysMenuGroupVO();
        vo.setGroupId(1L);
        vo.setGroupName("Updated Group");
        when(menuGroupMapper.updateById(any(SysMenuGroup.class))).thenReturn(1);

        sysMenuGroupService.updateMenuGroup(vo);

        verify(menuGroupMapper).updateById(any(SysMenuGroup.class));
    }

    @Test
    @DisplayName("deleteMenuGroup should delete menus and group")
    void deleteMenuGroup_existingGroup_deletesMenusAndGroup() {
        when(menuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(menuGroupMapper.deleteById(1L)).thenReturn(1);

        sysMenuGroupService.deleteMenuGroup(1L);

        verify(menuMapper).delete(any(LambdaQueryWrapper.class));
        verify(menuGroupMapper).deleteById(1L);
    }

    @Test
    @DisplayName("SysMenuGroupServiceImpl implements ISysMenuGroupService")
    void implementsISysMenuGroupService() {
        assertTrue(sysMenuGroupService instanceof ISysMenuGroupService);
    }

    private SysMenuGroup createSysMenuGroup(Long groupId, String groupName, String groupCode) {
        SysMenuGroup group = new SysMenuGroup();
        group.setGroupId(groupId);
        group.setGroupName(groupName);
        group.setGroupCode(groupCode);
        group.setStatus(1);
        group.setSort(1);
        return group;
    }
}
