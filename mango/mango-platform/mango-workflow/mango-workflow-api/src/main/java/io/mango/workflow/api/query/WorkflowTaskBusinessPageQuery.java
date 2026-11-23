package io.mango.workflow.api.query;

import io.mango.workflow.api.enums.WorkflowApplyStatus;
import io.mango.workflow.api.enums.WorkflowTaskClaimStatus;
import io.mango.workflow.api.enums.WorkflowTaskQueryScope;
import io.mango.workflow.api.enums.WorkflowTaskQueryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务类型查询任务，业务主键为可选过滤条件。
 *
 * <p>查询范围由 {@link WorkflowTaskQueryScope} 决定：默认仅返回当前用户相关任务；管理员范围需要
 * {@code workflow:task:all} 权限码。{@code businessKeys} 不传或为空表示不限业务主键，
 * 传入时按业务主键集合进一步收窄。{@code applyKeyword} 匹配申请单的编号、标题和摘要，
 * {@code keyword} 匹配任务名称和任务节点定义 Key，两者互不替代。
 * {@code applyAttributeFilters} 按业务登记在申请上的可筛属性精确收窄，多个条件为且、
 * 同一条件的多个值为或；业务不需要先枚举 {@code businessKeys}。
 *
 * <p>业务类型有两种传法：{@code businessType} 单值（历史用法）与 {@code businessTypes} 集合，
 * 集合非空时以集合为准、单值字段被忽略，两者都为空时查询被拒绝。
 * {@code processDefinitionKeys} 按流程定义收窄，与业务类型、节点定义 Key、业务主键是四个不同粒度。
 */
@Data
@Schema(description = "按业务类型查询任务，业务主键可选")
public class WorkflowTaskBusinessPageQuery {

    @Schema(description = "业务类型，按业务申请表 business_type 精确匹配；与 businessTypes 至少传一个，"
            + "两者都传时以 businessTypes 为准", example = "BAOHAN_RC_PROJECT_REVIEW")
    @Size(max = 128, message = "业务类型长度不能超过128")
    private String businessType;

    @Schema(description = "业务类型集合，可选；非空时按集合匹配并忽略 businessType，"
            + "用于一个列表同时展示多个业务类型的任务")
    @Size(max = 20, message = "业务类型数量不能超过20")
    private List<@NotBlank(message = "业务类型不能为空")
                 @Size(max = 128, message = "业务类型长度不能超过128") String> businessTypes;

    // 字段级 @Size 满足协议模型校验契约；Integer.MAX_VALUE 表示不设置实际数量上限，
    // 元素级注解负责逐项校验业务主键。
    @Size(max = Integer.MAX_VALUE, message = "业务主键数量超出支持范围")
    @Schema(description = "业务主键集合，可选；不传或为空表示不限业务主键，单个长度不超过128")
    private List<@NotBlank(message = "业务主键不能为空")
                 @Size(max = 128, message = "业务主键长度不能超过128") String> businessKeys;

    @Schema(description = "数据范围：SELF=本人，ALL=全量（需要 workflow:task:all）；"
            + "部门档由平台数据范围配置决定，不再通过入参传部门",
            defaultValue = "SELF")
    private WorkflowTaskQueryScope scope = WorkflowTaskQueryScope.SELF;

    /**
     * 申请人部门 ID 集合。
     *
     * @deprecated 部门档改由平台数据范围配置（资源码 {@code workflow:task:list}）按登录人处理，
     *             业务不再传部门参数；该字段保留只为避免调用方编译面突变，传入会被拒绝。下一个大版本移除。
     */
    @Deprecated(since = "1.16.0", forRemoval = true)
    @Schema(description = "【已废弃】申请人部门 ID 集合，按 coalesce(applicant_dept_id, org_id) 精确匹配；"
            + "部门档已改由平台数据范围配置接管，传入会被拒绝",
            example = "[1001,1002]")
    @Size(max = 500, message = "组织数量不能超过500")
    private List<Long> deptIds;

    @Schema(description = "任务节点定义 Key 集合")
    @Size(max = 100, message = "节点数量不能超过100")
    private List<@NotBlank(message = "节点定义 Key 不能为空") String> taskDefinitionKeys;

    @Schema(description = "流程定义 Key 集合，可选；不传或为空表示不限流程定义，"
            + "按申请表的 process_definition_key 精确匹配")
    @Size(max = 100, message = "流程定义数量不能超过100")
    private List<@NotBlank(message = "流程定义 Key 不能为空")
                 @Size(max = 128, message = "流程定义 Key 长度不能超过128") String> processDefinitionKeys;

    @Schema(description = "办理人用户 ID 集合，按引擎办理人标识匹配")
    @Size(max = 200, message = "办理人数量不能超过200")
    private List<Long> assigneeIds;

    @Schema(description = "任务状态：TODO=待办，DONE=已办（含通过/本人驳回/本人退回），"
            + "CANCELED=已终止（含撤回/流程终止）；也可直接传细状态 "
            + "COMPLETED/REJECTED/RETURNED/WITHDRAWN/TERMINATED，多值取并集",
            example = "[\"DONE\",\"CANCELED\"]")
    @Size(max = 9, message = "任务状态数量不能超过9")
    private List<WorkflowTaskQueryStatus> statuses;

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

    @Schema(description = "申请关键字，模糊匹配申请编号、申请标题或申请摘要；% 和 _ 按字面匹配")
    @Size(max = 128, message = "申请关键字长度不能超过128")
    private String applyKeyword;

    @Schema(description = "申请可筛属性过滤条件：条件之间为且，同一条件的多个值为或；最多10个条件")
    @Size(max = 10, message = "可筛属性条件数量不能超过10")
    @Valid
    private List<WorkflowApplyAttributeQuery> applyAttributeFilters;

    @Schema(description = "申请创建时间起，闭区间")
    private LocalDateTime applyTimeBegin;

    @Schema(description = "申请创建时间止，闭区间")
    private LocalDateTime applyTimeEnd;

    @Schema(description = "排序字段：createTime=任务创建时间，endTime=处理时间，claimTime=领取时间，applyTime=申请创建时间",
            defaultValue = "createTime")
    @Pattern(regexp = "createTime|endTime|claimTime|applyTime",
            message = "排序字段只支持 createTime、endTime、claimTime、applyTime")
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
