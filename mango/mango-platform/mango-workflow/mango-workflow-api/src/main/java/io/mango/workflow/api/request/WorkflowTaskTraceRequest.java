package io.mango.workflow.api.request;

import io.mango.workflow.api.query.WorkflowTaskTraceQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * POST 请求体承载的审批流转记录读取条件。
 *
 * <p>字段与约束继承 {@link WorkflowTaskTraceQuery}，只表达 HTTP 请求体绑定。
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "按业务键读取审批流转记录请求")
public class WorkflowTaskTraceRequest extends WorkflowTaskTraceQuery {
}
