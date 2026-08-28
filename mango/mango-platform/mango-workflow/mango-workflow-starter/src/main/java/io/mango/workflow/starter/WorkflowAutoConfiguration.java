package io.mango.workflow.starter;

import io.mango.workflow.core.identity.IWorkflowAssigneeIdentityProvider;
import io.mango.workflow.core.identity.WorkflowAssigneeIdentityService;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
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
}
