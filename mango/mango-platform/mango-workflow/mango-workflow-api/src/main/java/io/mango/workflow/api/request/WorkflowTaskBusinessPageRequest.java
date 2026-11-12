package io.mango.workflow.api.request;

import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * POST 请求体承载的按业务主键查询任务条件。
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "按业务主键批量查询任务请求")
public class WorkflowTaskBusinessPageRequest extends WorkflowTaskBusinessPageQuery {
}
