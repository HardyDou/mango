package io.mango.workflow.api.query;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.enums.WorkflowTaskQueryScope;
import io.mango.workflow.api.enums.WorkflowTaskRuntimeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务主键批量查询任务。
 *
 * <p>查询范围由 {@link WorkflowTaskQueryScope} 决定：默认仅返回当前用户相关任务；管理员范围需要
 * {@code workflow:task:all} 权限码。业务主键同时决定租户和业务申请可见性的收敛边界。
 */
@Data
@Schema(description = "按业务主键批量查询任务")
public class WorkflowTaskBusinessPageQuery {

    @Schema(description = "业务类型，按业务申请表 business_type 精确匹配", example = "BAOHAN_RC_PROJECT_REVIEW")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型长度不能超过128")
    private String businessType;

    @Schema(description = "业务主键集合，数量不设上限，单个长度不超过128")
    @NotEmpty(message = "业务主键不能为空")
    private List<@NotBlank(message = "业务主键不能为空")
                 @Size(max = 128, message = "业务主键长度不能超过128") String> businessKeys;

    @Schema(description = "数据范围：SELF=本人，ALL=全量（需要 workflow:task:all），ORG=指定组织（当前版本不支持）",
            defaultValue = "SELF")
    private WorkflowTaskQueryScope scope = WorkflowTaskQueryScope.SELF;

    @Schema(description = "组织 ID 集合，仅 scope=ORG 时允许传入；当前版本传入将被拒绝")
    @Size(max = 500, message = "组织数量不能超过500")
    private List<Long> deptIds;

    @Schema(description = "任务节点定义 Key 集合")
    @Size(max = 100, message = "节点数量不能超过100")
    private List<@NotBlank(message = "节点定义 Key 不能为空") String> taskDefinitionKeys;

    @Schema(description = "办理人用户 ID 集合，按引擎办理人标识匹配")
    @Size(max = 200, message = "办理人数量不能超过200")
    private List<Long> assigneeIds;

    @Schema(description = "任务状态：TODO=在途，DONE=已办理，CANCELED=已终止")
    @Size(max = 3, message = "任务状态数量不能超过3")
    private List<WorkflowTaskRuntimeStatus> statuses;

    @Schema(description = "认领状态：UNCLAIMED=待领取，ASSIGNED=待处理")
    @Size(max = 3, message = "认领状态数量不能超过3")
    private List<WorkflowTaskClaimStatus> claimStatuses;

    @Schema(description = "业务申请状态集合")
    @Size(max = 8, message = "业务申请状态数量不能超过8")
    private List<WorkflowApplyStatus> applyStatuses;

    @Schema(description = "是否发生过退回：true=只看退回过的申请，false=只看从未退回的申请，不传=不过滤")
    private Boolean returned;

    @Schema(description = "任务创建时间起，闭区间")
    private LocalDateTime createTimeBegin;

    @Schema(description = "任务创建时间止，闭区间")
    private LocalDateTime createTimeEnd;

    @Schema(description = "任务完成时间起，闭区间")
    private LocalDateTime endTimeBegin;

    @Schema(description = "任务完成时间止，闭区间")
    private LocalDateTime endTimeEnd;

    @Schema(description = "关键字，匹配任务名称或节点定义 Key")
    @Size(max = 128, message = "关键字长度不能超过128")
    private String keyword;

    @Schema(description = "排序字段：createTime=任务创建时间，endTime=处理时间，claimTime=领取时间",
            defaultValue = "createTime")
    @Pattern(regexp = "createTime|endTime|claimTime", message = "排序字段只支持 createTime、endTime、claimTime")
    private String sortBy = "createTime";

    @Schema(description = "排序方向：asc=升序，desc=降序", defaultValue = "desc")
    @Pattern(regexp = "asc|desc", message = "排序方向只支持 asc、desc")
    private String sortOrder = "desc";

    @Schema(description = "页码，从1开始；不传时返回全部结果")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page;

    @Schema(description = "每页数量，最大500；与页码成对使用")
    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 500, message = "每页数量不能超过500")
    private Integer size;
}
