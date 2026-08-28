package io.mango.workflow.starter.provider;

import io.mango.authorization.api.RoleApi;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.org.api.PostApi;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.vo.PostVO;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.system.api.DictApi;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.workflow.api.WorkflowDesignerOptionProvider;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.vo.WorkflowDesignerOptionsVO;
import io.mango.workflow.starter.WorkflowAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowPlatformApiDesignerOptionProviderTest {

    @Test
    void optionsMapsAllPlatformCandidateTypes() {
        IdentityUserApi identityUserApi = mock(IdentityUserApi.class);
        RoleApi roleApi = mock(RoleApi.class);
        PostApi postApi = mock(PostApi.class);
        SysOrgApi sysOrgApi = mock(SysOrgApi.class);
        DictApi dictApi = mock(DictApi.class);
        when(identityUserApi.page(any())).thenReturn(R.ok(PageResult.of(List.of(user()), 1, 1, 100)));
        when(roleApi.list()).thenReturn(R.ok(List.of(role())));
        when(postApi.page(any())).thenReturn(R.ok(PageResult.of(List.of(post()), 1, 1, 100)));
        when(sysOrgApi.tree(any())).thenReturn(R.ok(List.of(organization())));
        when(dictApi.listTypes(null)).thenReturn(R.ok(List.of(dictType())));

        WorkflowDesignerOptionsVO options = provider(
                identityUserApi, roleApi, postApi, sysOrgApi, dictApi).options();

        assertThat(options.getUsers()).singleElement()
                .satisfies(option -> assertThat(option)
                        .extracting("value", "label")
                        .containsExactly("11", "张三 / zhangsan"));
        assertThat(options.getRoles()).singleElement()
                .satisfies(option -> assertThat(option)
                        .extracting("value", "label")
                        .containsExactly("21", "审批员 / APPROVER"));
        assertThat(options.getPosts()).singleElement()
                .satisfies(option -> assertThat(option)
                        .extracting("value", "label")
                        .containsExactly("31", "财务岗 / FINANCE"));
        assertThat(options.getOrganizations()).singleElement().satisfies(option -> {
            assertThat(option).extracting("value", "label").containsExactly("41", "总部");
            assertThat(option.getChildren()).singleElement()
                    .satisfies(child -> assertThat(child)
                            .extracting("value", "label")
                            .containsExactly("42", "财务部"));
        });
        assertThat(options.getDictTypes()).singleElement()
                .satisfies(option -> assertThat(option)
                        .extracting("value", "label")
                        .containsExactly("expense_type", "费用类型"));
    }

    @Test
    void optionsFailsExplicitlyWhenDownstreamApiIsMissing() {
        WorkflowPlatformApiDesignerOptionProvider provider = provider(
                null, mock(RoleApi.class), mock(PostApi.class), mock(SysOrgApi.class), mock(DictApi.class));

        assertThatThrownBy(provider::options)
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(WorkflowCode.DESIGNER_OPTION_PROVIDER_MISSING.getCode());
    }

    @Test
    void optionsFailsExplicitlyWhenDownstreamResponseFails() {
        IdentityUserApi identityUserApi = mock(IdentityUserApi.class);
        when(identityUserApi.page(any())).thenReturn(R.fail(500, "identity unavailable"));
        WorkflowPlatformApiDesignerOptionProvider provider = provider(
                identityUserApi, mock(RoleApi.class), mock(PostApi.class), mock(SysOrgApi.class), mock(DictApi.class));

        assertThatThrownBy(provider::options)
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(WorkflowCode.DESIGNER_OPTION_LOAD_FAILED.getCode());
    }

    @Test
    void autoConfigurationAllowsApplicationsToReplaceDefaultProvider() throws NoSuchMethodException {
        Method factory = WorkflowAutoConfiguration.class.getDeclaredMethod(
                "workflowDesignerOptionProvider",
                ObjectProvider.class,
                ObjectProvider.class,
                ObjectProvider.class,
                ObjectProvider.class,
                ObjectProvider.class);

        ConditionalOnMissingBean condition = factory.getAnnotation(ConditionalOnMissingBean.class);

        assertThat(condition).isNotNull();
        assertThat(condition.value()).containsExactly(WorkflowDesignerOptionProvider.class);
    }

    private WorkflowPlatformApiDesignerOptionProvider provider(
            IdentityUserApi identityUserApi,
            RoleApi roleApi,
            PostApi postApi,
            SysOrgApi sysOrgApi,
            DictApi dictApi) {
        return new WorkflowPlatformApiDesignerOptionProvider(
                objectProvider(identityUserApi),
                objectProvider(roleApi),
                objectProvider(postApi),
                objectProvider(sysOrgApi),
                objectProvider(dictApi));
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> objectProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private IdentityUserVO user() {
        IdentityUserVO user = new IdentityUserVO();
        user.setUserId(11L);
        user.setUsername("zhangsan");
        user.setNickname("张三");
        return user;
    }

    private RoleVO role() {
        RoleVO role = new RoleVO();
        role.setRoleId(21L);
        role.setRoleCode("APPROVER");
        role.setRoleName("审批员");
        return role;
    }

    private PostVO post() {
        PostVO post = new PostVO();
        post.setId(31L);
        post.setPostCode("FINANCE");
        post.setPostName("财务岗");
        return post;
    }

    private SysOrgVO organization() {
        SysOrgVO child = new SysOrgVO();
        child.setId(42L);
        child.setOrgName("财务部");
        SysOrgVO root = new SysOrgVO();
        root.setId(41L);
        root.setOrgName("总部");
        root.setChildren(List.of(child));
        return root;
    }

    private DictTypeVO dictType() {
        DictTypeVO dictType = new DictTypeVO();
        dictType.setId(51L);
        dictType.setDictType("expense_type");
        dictType.setDictName("费用类型");
        return dictType;
    }
}
