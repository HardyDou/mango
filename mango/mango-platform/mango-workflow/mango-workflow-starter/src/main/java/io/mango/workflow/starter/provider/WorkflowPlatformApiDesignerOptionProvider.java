package io.mango.workflow.starter.provider;

import io.mango.authorization.api.RoleApi;
import io.mango.authorization.api.vo.RoleVO;
import io.mango.common.result.R;
import io.mango.common.result.Require;
import io.mango.common.vo.PageResult;
import io.mango.identity.api.IdentityUserApi;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.vo.IdentityUserVO;
import io.mango.org.api.PostApi;
import io.mango.org.api.SysOrgApi;
import io.mango.org.api.query.PostPageQuery;
import io.mango.org.api.query.SysOrgTreeQuery;
import io.mango.org.api.vo.PostVO;
import io.mango.org.api.vo.SysOrgVO;
import io.mango.system.api.DictApi;
import io.mango.system.api.vo.DictTypeVO;
import io.mango.workflow.api.WorkflowDesignerOptionProvider;
import io.mango.workflow.api.enums.WorkflowCode;
import io.mango.workflow.api.vo.WorkflowDesignerOptionVO;
import io.mango.workflow.api.vo.WorkflowDesignerOptionsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通过 Mango 平台公共 API 提供流程设计器候选数据。
 */
@RequiredArgsConstructor
public class WorkflowPlatformApiDesignerOptionProvider implements WorkflowDesignerOptionProvider {

    private static final long PAGE_SIZE = 100L;

    private final ObjectProvider<IdentityUserApi> identityUserApiProvider;
    private final ObjectProvider<RoleApi> roleApiProvider;
    private final ObjectProvider<PostApi> postApiProvider;
    private final ObjectProvider<SysOrgApi> sysOrgApiProvider;
    private final ObjectProvider<DictApi> dictApiProvider;

    @Override
    public WorkflowDesignerOptionsVO options() {
        WorkflowDesignerOptionsVO options = new WorkflowDesignerOptionsVO();
        options.setUsers(users());
        options.setRoles(roles());
        options.setPosts(posts());
        options.setOrganizations(organizations());
        options.setDictTypes(dictTypes());
        return options;
    }

    private List<WorkflowDesignerOptionVO> users() {
        IdentityUserPageQuery query = new IdentityUserPageQuery();
        query.setPage(1L);
        query.setSize(PAGE_SIZE);
        R<PageResult<IdentityUserVO>> response = requiredApi(
                identityUserApiProvider, "IdentityUserApi").page(query);
        List<WorkflowDesignerOptionVO> options = new ArrayList<>();
        for (IdentityUserVO user : requiredData(response, "用户").getList()) {
            String value = text(user.getUserId());
            String name = firstText(user.getNickname(), user.getMemberName(), user.getUsername(), value);
            if (!StringUtils.hasText(value) || !StringUtils.hasText(name)) {
                continue;
            }
            String suffix = StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(name)
                    ? " / " + user.getUsername()
                    : "";
            options.add(option(value, name + suffix));
        }
        return options;
    }

    private List<WorkflowDesignerOptionVO> roles() {
        R<List<RoleVO>> response = requiredApi(roleApiProvider, "RoleApi").list();
        List<WorkflowDesignerOptionVO> options = new ArrayList<>();
        for (RoleVO role : requiredData(response, "角色")) {
            String value = text(role.getRoleId());
            String name = firstText(role.getRoleName(), role.getRoleCode(), value);
            if (!StringUtils.hasText(value) || !StringUtils.hasText(name)) {
                continue;
            }
            String suffix = StringUtils.hasText(role.getRoleCode()) && !role.getRoleCode().equals(name)
                    ? " / " + role.getRoleCode()
                    : "";
            options.add(option(value, name + suffix));
        }
        return options;
    }

    private List<WorkflowDesignerOptionVO> posts() {
        PostPageQuery query = new PostPageQuery();
        query.setPage(1L);
        query.setSize(PAGE_SIZE);
        R<PageResult<PostVO>> response = requiredApi(postApiProvider, "PostApi").page(query);
        List<WorkflowDesignerOptionVO> options = new ArrayList<>();
        for (PostVO post : requiredData(response, "岗位").getList()) {
            String value = text(post.getId());
            String name = firstText(post.getPostName(), post.getPostCode(), value);
            if (!StringUtils.hasText(value) || !StringUtils.hasText(name)) {
                continue;
            }
            String suffix = StringUtils.hasText(post.getPostCode()) && !post.getPostCode().equals(name)
                    ? " / " + post.getPostCode()
                    : "";
            options.add(option(value, name + suffix));
        }
        return options;
    }

    private List<WorkflowDesignerOptionVO> organizations() {
        SysOrgTreeQuery query = new SysOrgTreeQuery();
        query.setParentId(0L);
        query.setIncludeDisabled(Boolean.TRUE);
        R<List<SysOrgVO>> response = requiredApi(sysOrgApiProvider, "SysOrgApi").tree(query);
        return orgOptions(requiredData(response, "组织"));
    }

    private List<WorkflowDesignerOptionVO> orgOptions(List<SysOrgVO> organizations) {
        List<WorkflowDesignerOptionVO> options = new ArrayList<>();
        for (SysOrgVO organization : organizations) {
            String value = text(organization.getId());
            String label = firstText(organization.getOrgName(), organization.getOrgCode(), value);
            if (!StringUtils.hasText(value) || !StringUtils.hasText(label)) {
                continue;
            }
            WorkflowDesignerOptionVO option = option(value, label);
            option.setChildren(orgOptions(organization.getChildren()));
            options.add(option);
        }
        return options;
    }

    private List<WorkflowDesignerOptionVO> dictTypes() {
        R<List<DictTypeVO>> response = requiredApi(dictApiProvider, "DictApi").listTypes(null);
        List<WorkflowDesignerOptionVO> options = new ArrayList<>();
        for (DictTypeVO type : requiredData(response, "字典类型")) {
            String value = firstText(type.getDictType(), text(type.getId()));
            String label = firstText(type.getDictName(), value);
            if (StringUtils.hasText(value) && StringUtils.hasText(label)) {
                options.add(option(value, label));
            }
        }
        return options;
    }

    private <T> T requiredApi(ObjectProvider<T> provider, String apiName) {
        return Require.nonNull(provider.getIfAvailable(), WorkflowCode.DESIGNER_OPTION_PROVIDER_MISSING,
                "流程设计器候选数据 Provider 缺少 " + apiName);
    }

    private <T> T requiredData(R<T> response, String sourceName) {
        Require.isTrue(response != null && response.isSuccess() && response.getData() != null,
                WorkflowCode.DESIGNER_OPTION_LOAD_FAILED, sourceName + "候选数据加载失败");
        return Objects.requireNonNull(response).getData();
    }

    private WorkflowDesignerOptionVO option(String value, String label) {
        WorkflowDesignerOptionVO option = new WorkflowDesignerOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
