package io.mango.workflow.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作流任务查询配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mango.workflow.task-query")
public class WorkflowTaskQueryProperties {

    /**
     * 不传分页参数时的默认单次返回上限。
     */
    private static final int DEFAULT_UNPAGED_MAX_ROWS = 2000;

    /**
     * 不传分页参数时单次返回的最大任务行数；超出部分调用方应改用分页参数拉取。
     */
    private int unpagedMaxRows = DEFAULT_UNPAGED_MAX_ROWS;
}
