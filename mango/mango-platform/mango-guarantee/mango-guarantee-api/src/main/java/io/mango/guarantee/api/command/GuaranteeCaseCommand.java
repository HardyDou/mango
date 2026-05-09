package io.mango.guarantee.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 保函业务单创建或修改命令。
 */
@Data
@Schema(description = "保函业务单创建或修改命令")
public class GuaranteeCaseCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务单ID，创建时为空，修改时必填")
    private Long caseId;

    @Schema(description = "业务单标题")
    @NotBlank(message = "业务单标题不能为空")
    @Size(max = 200, message = "业务单标题最多200个字符")
    private String title;

    @Schema(description = "申请人名称")
    @NotBlank(message = "申请人名称不能为空")
    @Size(max = 100, message = "申请人名称最多100个字符")
    private String applicantName;

    @Schema(description = "受益人名称")
    @Size(max = 100, message = "受益人名称最多100个字符")
    private String beneficiaryName;

    @Schema(description = "保函类型编码，例如 BID、PERFORMANCE")
    @Size(max = 64, message = "保函类型编码最多64个字符")
    private String guaranteeType;

    @Schema(description = "保函金额")
    @NotNull(message = "保函金额不能为空")
    private BigDecimal amount;

    @Schema(description = "币种编码，例如 CNY")
    @Size(max = 16, message = "币种编码最多16个字符")
    private String currency;

    @Schema(description = "期望出函日期")
    private LocalDate expectedIssueDate;

    @Schema(description = "状态：0-草稿，1-处理中，2-已完成，9-已取消")
    @Min(value = 0, message = "状态最小值为0")
    @Max(value = 9, message = "状态最大值为9")
    private Integer status;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注最多500个字符")
    private String remark;
}
