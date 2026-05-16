package io.mango.guarantee.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 保函业务单视图。
 */
@Data
@Schema(description = "保函业务单视图")
public class GuaranteeCaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务单ID")
    private Long caseId;

    @Schema(description = "业务单编号")
    private String caseNo;

    @Schema(description = "来源机构ID")
    private Long sourceTenantId;

    @Schema(description = "来源机构名称")
    private String sourceTenantName;

    @Schema(description = "业务单标题")
    private String title;

    @Schema(description = "申请人名称")
    private String applicantName;

    @Schema(description = "受益人名称")
    private String beneficiaryName;

    @Schema(description = "保函类型编码")
    private String guaranteeType;

    @Schema(description = "保函金额")
    private BigDecimal amount;

    @Schema(description = "币种编码")
    private String currency;

    @Schema(description = "期望出函日期")
    private LocalDate expectedIssueDate;

    @Schema(description = "状态：0-草稿，1-处理中，2-已完成，9-已取消")
    private Integer status;

    @Schema(description = "最新流程实例ID")
    private String processInstanceId;

    @Schema(description = "最新流程名称")
    private String processName;

    @Schema(description = "最新流程状态")
    private String processStatus;

    @Schema(description = "当前执行节点名称")
    private String currentTaskName;

    @Schema(description = "当前执行节点定义键")
    private String currentTaskDefinitionKey;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
