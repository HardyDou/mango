package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.org.api.entity.SysOrg;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.org.core.service.ISysOrgService;
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
 * Unit tests for SysOrgServiceImpl
 *
 * @author Mango
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysOrgServiceImpl Tests")
class SysOrgServiceImplTest {

    @Mock
    private SysOrgMapper orgMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private TenantMemberProvider tenantMemberProvider;

    private SysOrgServiceImpl sysOrgService;

    @BeforeEach
    void setUp() {
        sysOrgService = new SysOrgServiceImpl(
                orgMapper,
                postMapper,
                tenantMemberProvider);
    }

    @Test
    @DisplayName("tree should return complete tree for given parentId")
    void tree_withParentId_returnsCompleteTree() {
        SysOrg root = createSysOrg(1L, "Root Org", 0L, 1);
        SysOrg child = createSysOrg(2L, "Child Org", 1L, 2);
        SysOrg grandchild = createSysOrg(3L, "Grandchild Org", 2L, 3);
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(root, child, grandchild));

        List<SysOrg> result = sysOrgService.tree(0L, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals(2L, result.get(0).getChildren().get(0).getId());
        assertEquals(1, result.get(0).getChildren().get(0).getChildren().size());
        assertEquals(3L, result.get(0).getChildren().get(0).getChildren().get(0).getId());
    }

    @Test
    @DisplayName("tree should return empty list when no organizations")
    void tree_noOrganizations_returnsEmptyList() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysOrg> result = sysOrgService.tree(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("children should return child organizations")
    void children_withParentId_returnsChildren() {
        SysOrg child = createSysOrg(2L, "Child Org", 1L, 1);
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(child));

        List<SysOrg> result = sysOrgService.children(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPid());
    }

    @Test
    @DisplayName("children should return empty list when no children")
    void children_noChildren_returnsEmptyList() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<SysOrg> result = sysOrgService.children(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getById should return organization when exists")
    void getById_existingOrg_returnsOrg() {
        SysOrg org = createSysOrg(1L, "Test Org", 0L, 1);
        when(orgMapper.selectById(1L)).thenReturn(org);

        SysOrg result = sysOrgService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getById should throw BizException when organization not found")
    void getById_nonExistingOrg_throwsBizException() {
        when(orgMapper.selectById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> sysOrgService.getById(999L));
    }

    @Test
    @DisplayName("SysOrgServiceImpl implements ISysOrgService")
    void implementsISysOrgService() {
        assertTrue(sysOrgService instanceof ISysOrgService);
    }

    private SysOrg createSysOrg(Long id, String orgName, Long pid, Integer orgType) {
        SysOrg org = new SysOrg();
        org.setId(id);
        org.setOrgName(orgName);
        org.setPid(pid);
        org.setOrgType(orgType);
        org.setOrgCode("CODE_" + id);
        org.setOrgStatus("1");
        org.setOrgSort(1);
        return org;
    }
}
