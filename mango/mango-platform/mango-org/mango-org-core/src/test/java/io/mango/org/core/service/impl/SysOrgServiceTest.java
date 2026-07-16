package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.org.core.service.ISysOrgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 组织业务服务单元测试。
 */
@DisplayName("组织业务服务")
class SysOrgServiceTest {

    private SysOrgMapper orgMapper;

    private PostMapper postMapper;

    private TenantMemberProvider tenantMemberProvider;

    private SysOrgService sysOrgService;

    @BeforeEach
    void setUp() {
        orgMapper = mock(SysOrgMapper.class);
        postMapper = mock(PostMapper.class);
        tenantMemberProvider = mock(TenantMemberProvider.class);
        sysOrgService = new SysOrgService(postMapper, tenantMemberProvider);
        ReflectionTestUtils.setField(sysOrgService, "baseMapper", orgMapper);
    }

    @Test
    void treeBuildsCompleteHierarchy() {
        SysOrgEntity root = org(1L, "Root Org", 0L, 1);
        SysOrgEntity child = org(2L, "Child Org", 1L, 2);
        SysOrgEntity grandchild = org(3L, "Grandchild Org", 2L, 3);
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, child, grandchild));

        List<SysOrgVO> result = sysOrgService.tree(treeQuery(0L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getChildren()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(2L);
                    assertThat(item.getChildren()).singleElement()
                            .satisfies(grandChild -> assertThat(grandChild.getId()).isEqualTo(3L));
                });
    }

    @Test
    void treeReturnsEmptyListWithoutOrganizations() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(sysOrgService.tree(treeQuery(null))).isEmpty();
    }

    @Test
    void treeKeepsAncestorsWhenFilteringByType() {
        SysOrgEntity root = org(1L, "Root Org", 0L, 1);
        SysOrgEntity department = org(2L, "Department", 1L, 3);
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, department));
        SysOrgTreeQuery query = treeQuery(0L);
        query.setType(3);

        List<SysOrgVO> result = sysOrgService.tree(query);

        assertThat(result).singleElement()
                .satisfies(item -> assertThat(item.getChildren()).singleElement()
                        .satisfies(child -> assertThat(child.getOrgType()).isEqualTo(3)));
    }

    @Test
    void childrenReturnsDirectChildren() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(org(2L, "Child Org", 1L, 2)));

        assertThat(sysOrgService.children(1L)).singleElement()
                .satisfies(item -> assertThat(item.getPid()).isEqualTo(1L));
    }

    @Test
    void childrenReturnsEmptyListWhenMissing() {
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(sysOrgService.children(999L)).isEmpty();
    }

    @Test
    void detailReturnsProtocolView() {
        SysOrgEntity entity = org(1L, "Test Org", 0L, 1);
        entity.setTenantId(2L);
        when(orgMapper.selectById(1L)).thenReturn(entity);

        SysOrgVO result = sysOrgService.detail(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantId()).isEqualTo(2L);
    }

    @Test
    void detailRejectsMissingOrganization() {
        when(orgMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> sysOrgService.detail(999L)).isInstanceOf(BizException.class);
    }

    @Test
    void implementsTypedServiceContract() {
        assertThat(sysOrgService).isInstanceOf(ISysOrgService.class);
    }

    private SysOrgTreeQuery treeQuery(Long parentId) {
        SysOrgTreeQuery query = new SysOrgTreeQuery();
        query.setParentId(parentId);
        return query;
    }

    private SysOrgEntity org(Long id, String name, Long pid, Integer type) {
        SysOrgEntity entity = new SysOrgEntity();
        entity.setId(id);
        entity.setOrgName(name);
        entity.setPid(pid);
        entity.setOrgType(type);
        entity.setOrgCode("CODE_" + id);
        entity.setOrgStatus("1");
        entity.setOrgSort(1);
        entity.setTenantId(1L);
        return entity;
    }
}
