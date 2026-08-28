package io.mango.workflow.starter;

import io.mango.authorization.api.RoleApi;
import io.mango.identity.api.IdentityUserApi;
import io.mango.org.api.PostApi;
import io.mango.org.api.SysOrgApi;
import io.mango.system.api.DictApi;
import io.mango.workflow.api.WorkflowDesignerOptionProvider;
import io.mango.workflow.core.identity.IWorkflowAssigneeIdentityProvider;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.starter.provider.WorkflowPlatformApiDesignerOptionProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 工作流自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(WorkflowDefinitionMapper.class)
@ConditionalOnProperty(prefix = "mango.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.workflow.core.mapper")
@ComponentScan({
        "io.mango.workflow.core",
        "io.mango.workflow.starter"
})
public class WorkflowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WorkflowAssigneeIdentityService workflowAssigneeIdentityService(
            ObjectProvider<IWorkflowAssigneeIdentityProvider> identityProviders) {
        return new WorkflowAssigneeIdentityService(identityProviders);
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowDesignerOptionProvider.class)
    public WorkflowDesignerOptionProvider workflowDesignerOptionProvider(
            ObjectProvider<IdentityUserApi> identityUserApiProvider,
            ObjectProvider<RoleApi> roleApiProvider,
            ObjectProvider<PostApi> postApiProvider,
            ObjectProvider<SysOrgApi> sysOrgApiProvider,
            ObjectProvider<DictApi> dictApiProvider) {
        return new WorkflowPlatformApiDesignerOptionProvider(
                identityUserApiProvider,
                roleApiProvider,
                postApiProvider,
                sysOrgApiProvider,
                dictApiProvider);
    }
}
