package io.mango.workflow.api.request;

import io.mango.workflow.api.query.WorkflowActivityTimelineQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;

/**
 * POST 请求体承载的活动时间轴读取条件。
 *
 * <p>字段与约束继承 {@link WorkflowActivityTimelineQuery}，只表达 HTTP 请求体绑定。
 */
@EqualsAndHashCode(callSuper = true)
@Schema(description = "按业务键读取活动时间轴请求")
public class WorkflowActivityTimelineRequest extends WorkflowActivityTimelineQuery {
}
