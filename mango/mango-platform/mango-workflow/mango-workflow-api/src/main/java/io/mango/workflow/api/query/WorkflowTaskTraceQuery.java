package io.mango.workflow.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 按业务键读取审批流转记录的条件。
 *
 * <p>只做精确查询，不包含数据范围语义：处理人、申请人部门、动作和时间区间用于收窄结果，
 * 接口权限和业务侧数据权限由调用方入口负责。业务键可以对应多个流程实例（重新申请），
 * 返回结果是这些实例记录的并集，调用方按 {@code processInstanceId} 分组。
 */
@Data
@Schema(description = "按业务键读取审批流转记录条件")
public class WorkflowTaskTraceQuery {

    @Schema(description = "业务类型，按业务申请表的 business_type 精确匹配", example = "BAOHAN_RC_PROJECT_REVIEW")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 128, message = "业务类型长度不能超过128")
    private String businessType;

    @Schema(description = "业务主键，按业务申请表的 business_key 精确匹配", example = "RC202611130001")
    @NotBlank(message = "业务主键不能为空")
    @Size(max = 128, message = "业务主键长度不能超过128")
    private String businessKey;

    @Schema(description = "处理人用户 ID 集合，可选；按记录处理人过滤，不传或为空表示不过滤")
    @Size(max = Integer.MAX_VALUE, message = "处理人数量超出支持范围")
    private List<Long> operatorIds;

    @Schema(description = "申请人部门 ID 集合，可选；命中业务申请的申请人部门时整条流程实例参与返回，不传或为空表示不过滤")
    @Size(max = 500, message = "部门数量不能超过500")
    private List<Long> deptIds;

    @Schema(description = "动作编码集合，可选；取值见审批动作枚举，例如 START、CLAIM、COMPLETE、RETURN，不传或为空表示不过滤")
    @Size(max = Integer.MAX_VALUE, message = "动作数量超出支持范围")
    private List<@NotBlank(message = "动作编码不能为空") @Size(max = 32, message = "动作编码长度不能超过32") String> actions;

    @Schema(description = "处理时间起，闭区间")
    private LocalDateTime beginTime;

    @Schema(description = "处理时间止，闭区间")
    private LocalDateTime endTime;
}
