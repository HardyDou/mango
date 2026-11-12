package io.mango.workflow.api.request;

import io.mango.workflow.api.query.WorkflowTaskBusinessPageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * POST 请求体承载的按业务类型查询任务条件，业务主键可选。
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "按业务类型查询任务请求，业务主键可选")
public class WorkflowTaskBusinessPageRequest extends WorkflowTaskBusinessPageQuery {
}
