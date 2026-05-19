package io.mango.workflow.core.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作流示例流程配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mango.workflow.samples")
public class WorkflowSampleProperties {

    /**
     * 是否自动补齐内置示例流程。
     */
    private boolean enabled = true;

    /**
     * 示例流程所属租户。
     */
    private Long tenantId = 1L;

    /**
     * 示例流程所属分组编码。
     */
    private String groupCode = "COMMON";

    /**
     * 示例流程所属分组名称。
     */
    private String groupName = "通用流程";
}
