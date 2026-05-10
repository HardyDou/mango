package io.mango.workflow.core.engine;

import io.mango.common.result.Require;
import io.mango.workflow.api.WorkflowCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring Bean 节点执行器。
 */
@Component
@RequiredArgsConstructor
public class SpringBeanWorkflowNodeExecutor implements WorkflowNodeExecutor {

    private final ApplicationContext applicationContext;

    @Override
    public String executionType() {
        return "SPRING_BEAN";
    }

    @Override
    public void execute(WorkflowNodeExecutionContext context) {
        Object beanName = context.getProperties().get("beanName");
        Require.isTrue(beanName instanceof String && StringUtils.hasText((String) beanName),
                WorkflowCode.DESIGNER_INVALID.getCode(), "Spring Bean 节点必须配置 beanName");
        Object bean = applicationContext.getBean(((String) beanName).trim());
        Require.isTrue(bean instanceof WorkflowNodeExecutable,
                WorkflowCode.DESIGNER_INVALID.getCode(), "Spring Bean 节点只能调用 WorkflowNodeExecutable 白名单接口");
        ((WorkflowNodeExecutable) bean).execute(context);
    }
}
