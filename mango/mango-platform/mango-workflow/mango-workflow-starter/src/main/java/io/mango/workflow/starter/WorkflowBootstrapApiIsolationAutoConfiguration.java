package io.mango.workflow.starter;

import io.mango.workflow.api.WorkflowBusinessApplyApi;
import io.mango.workflow.api.WorkflowBusinessProcessApi;
import io.mango.workflow.api.WorkflowCategoryApi;
import io.mango.workflow.api.WorkflowDefinitionApi;
import io.mango.workflow.api.WorkflowProcessApi;
import io.mango.workflow.api.WorkflowTaskRuntimeApi;
import io.mango.workflow.api.WorkflowTemplateApi;
import io.mango.workflow.api.WorkflowTemplateCategoryApi;
import io.mango.workflow.core.mapper.WorkflowDefinitionMapper;
import io.mango.workflow.starter.controller.WorkflowBusinessApplyController;
import io.mango.workflow.starter.controller.WorkflowBusinessProcessController;
import io.mango.workflow.starter.controller.WorkflowCategoryController;
import io.mango.workflow.starter.controller.WorkflowDefinitionController;
import io.mango.workflow.starter.controller.WorkflowProcessController;
import io.mango.workflow.starter.controller.WorkflowTaskController;
import io.mango.workflow.starter.controller.WorkflowTemplateCategoryController;
import io.mango.workflow.starter.controller.WorkflowTemplateController;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Keeps public Workflow API injection from creating Flowable while Bootstrap is still migrating.
 *
 * <p>The proxy resolves the existing controller only when an API operation is invoked. Runtime mode keeps using the
 * controller directly, while Bootstrap can construct business runtime beans without crossing the migration boundary.
 */
@AutoConfiguration(before = WorkflowAutoConfiguration.class)
@ConditionalOnClass(WorkflowDefinitionMapper.class)
@ConditionalOnProperty(prefix = "mango.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "mango.bootstrap", name = "mode", havingValue = "bootstrap")
public class WorkflowBootstrapApiIsolationAutoConfiguration {

    @Bean
    @Primary
    WorkflowTaskRuntimeApi bootstrapWorkflowTaskRuntimeApi(
            ObjectProvider<WorkflowTaskController> controller) {
        return deferred(WorkflowTaskRuntimeApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowProcessApi bootstrapWorkflowProcessApi(
            ObjectProvider<WorkflowProcessController> controller) {
        return deferred(WorkflowProcessApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowBusinessApplyApi bootstrapWorkflowBusinessApplyApi(
            ObjectProvider<WorkflowBusinessApplyController> controller) {
        return deferred(WorkflowBusinessApplyApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowBusinessProcessApi bootstrapWorkflowBusinessProcessApi(
            ObjectProvider<WorkflowBusinessProcessController> controller) {
        return deferred(WorkflowBusinessProcessApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowDefinitionApi bootstrapWorkflowDefinitionApi(
            ObjectProvider<WorkflowDefinitionController> controller) {
        return deferred(WorkflowDefinitionApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowCategoryApi bootstrapWorkflowCategoryApi(
            ObjectProvider<WorkflowCategoryController> controller) {
        return deferred(WorkflowCategoryApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowTemplateApi bootstrapWorkflowTemplateApi(
            ObjectProvider<WorkflowTemplateController> controller) {
        return deferred(WorkflowTemplateApi.class, controller);
    }

    @Bean
    @Primary
    WorkflowTemplateCategoryApi bootstrapWorkflowTemplateCategoryApi(
            ObjectProvider<WorkflowTemplateCategoryController> controller) {
        return deferred(WorkflowTemplateCategoryApi.class, controller);
    }

    private static <T> T deferred(Class<T> apiType, ObjectProvider<? extends T> targetProvider) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setInterfaces(apiType);
        proxyFactory.setTargetSource(new ProviderTargetSource<>(apiType, targetProvider));
        return apiType.cast(proxyFactory.getProxy(apiType.getClassLoader()));
    }

    private static final class ProviderTargetSource<T> implements TargetSource {

        private final Class<T> targetClass;
        private final ObjectProvider<? extends T> targetProvider;

        private ProviderTargetSource(Class<T> targetClass, ObjectProvider<? extends T> targetProvider) {
            this.targetClass = targetClass;
            this.targetProvider = targetProvider;
        }

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public Object getTarget() {
            return targetProvider.getObject();
        }

        @Override
        public void releaseTarget(Object target) {
            // Singleton controllers are owned by the BeanFactory.
        }
    }
}
