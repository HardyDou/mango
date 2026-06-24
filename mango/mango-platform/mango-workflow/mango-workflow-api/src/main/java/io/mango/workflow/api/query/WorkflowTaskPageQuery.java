package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批中心任务分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "审批中心任务分页查询")
public class WorkflowTaskPageQuery extends PageQuery {

    @Schema(description = "关键字，支持按流程名称、任务名称搜索")
    private String keyword;

    @Schema(description = "待办类型：ASSIGNED=待处理，CLAIMABLE=待领取，ALL=全部")
    private String todoType;

    @Schema(description = "是否只查询未读抄送")
    private Boolean unread;

    @Schema(description = "是否只查询已超时待办")
    private Boolean overdue;
}
