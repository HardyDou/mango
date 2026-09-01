package io.mango.org.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.exception.BizException;
import io.mango.identity.api.TenantMemberProvider;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.org.api.command.CreateOrgMemberAccountCommand;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.org.core.entity.SysOrgEntity;
import io.mango.org.core.mapper.PostMapper;
import io.mango.org.core.mapper.SysOrgMapper;
import io.mango.org.core.service.ISysOrgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

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
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                9001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
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

    @Test
    void memberScopeIncludesSelectedOrganizationAndEnabledDescendants() {
        SysOrgEntity root = org(1L, "集团", 0L, 1);
        SysOrgEntity company = org(2L, "公司", 1L, 2);
        SysOrgEntity department = org(3L, "部门", 2L, 3);
        when(orgMapper.selectById(1L)).thenReturn(root);
        when(orgMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(root, company, department));

        assertThat(sysOrgService.memberScope(1L)).containsExactly(1L, 2L, 3L);
    }

    @Test
    void memberScopeRejectsMissingDisabledAndOtherTenantOrganizations() {
        SysOrgEntity disabled = org(1L, "停用部门", 0L, 3);
        disabled.setOrgStatus("0");
        SysOrgEntity otherTenant = org(2L, "其它机构", 0L, 1);
        otherTenant.setTenantId(2L);
        when(orgMapper.selectById(1L)).thenReturn(disabled);
        when(orgMapper.selectById(2L)).thenReturn(otherTenant);
        when(orgMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> sysOrgService.memberScope(1L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> sysOrgService.memberScope(2L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> sysOrgService.memberScope(999L)).isInstanceOf(BizException.class);
    }

    @Test
    void createMemberAccountValidatesOrganizationAndMapsTrustedIdentityCommand() {
        SysOrgEntity department = org(3L, "研发部", 2L, 3);
        when(orgMapper.selectById(3L)).thenReturn(department);
        when(tenantMemberProvider.createMemberInOrg(any(CreateTenantMemberInOrgCommand.class))).thenReturn(1001L);
        CreateOrgMemberAccountCommand command = new CreateOrgMemberAccountCommand();
        command.setOrgId(3L);
        command.setUsername("new-user");
        command.setPassword("Mango@123456");
        command.setNickname("新用户");
        command.setEmail("new-user@example.com");
        command.setPhone("13900000000");
        command.setStatus(1);

        assertThat(sysOrgService.createMemberAccount(command)).isEqualTo(1001L);
        ArgumentCaptor<CreateTenantMemberInOrgCommand> captor =
                ArgumentCaptor.forClass(CreateTenantMemberInOrgCommand.class);
        verify(tenantMemberProvider).createMemberInOrg(captor.capture());
        CreateTenantMemberInOrgCommand mapped = captor.getValue();
        assertThat(mapped.getTenantId()).isEqualTo(1L);
        assertThat(mapped.getOrgId()).isEqualTo(3L);
        assertThat(mapped.getOperatorUserId()).isEqualTo(9001L);
        assertThat(mapped.getUsername()).isEqualTo("new-user");
        assertThat(mapped.getPrimaryFlag()).isTrue();
        assertThat(mapped.getLeaderFlag()).isFalse();
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
