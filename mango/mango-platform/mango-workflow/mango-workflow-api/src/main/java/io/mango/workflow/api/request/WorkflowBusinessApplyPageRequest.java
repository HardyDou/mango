package io.mango.workflow.api.request;

import io.mango.workflow.api.query.WorkflowBusinessApplyPageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/** POST 请求体承载的业务申请分页条件。 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务工作流申请分页请求")
public class WorkflowBusinessApplyPageRequest extends WorkflowBusinessApplyPageQuery {
}
