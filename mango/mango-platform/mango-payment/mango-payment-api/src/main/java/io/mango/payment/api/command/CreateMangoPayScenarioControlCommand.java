package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建芒果支付异常场景控制命令")
public class CreateMangoPayScenarioControlCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "通道编码不能为空")
    @Size(max = 32, message = "通道编码不能超过 32 个字符")
    @Schema(description = "通道编码，仅支持 MANGO_PAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelCode;

    @Schema(description = "签约配置 ID。为空时对芒果支付全局生效；不为空时仅对该签约下一笔交易生效")
    private Long contractId;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型不能超过 32 个字符")
    @Schema(description = "场景类型：PAYMENT/PAYMENT_QUERY/REFUND/REFUND_QUERY/BILL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String scenarioType;

    @Size(max = 64, message = "场景码不能超过 64 个字符")
    @Schema(description = "场景码。交易类支持 SUCCESS/FAIL/PROCESSING/TIMEOUT 等统一映射码")
    private String scenarioCode;

    @Size(max = 32, message = "账单差异类型不能超过 32 个字符")
    @Schema(description = "账单差异类型：AMOUNT_PLUS/AMOUNT_MINUS，仅 BILL 场景使用")
    private String billDifferenceType;

    @Schema(description = "账单差异金额，单位分，仅 BILL 场景使用")
    private Long differenceAmount;

    @Schema(description = "回调延迟分钟数，仅 CALLBACK_DELAY 场景使用")
    private Integer callbackDelayMinutes;

    @NotNull(message = "生效次数不能为空")
    @Schema(description = "生效次数，默认用于控制下一笔交易时填 1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer effectiveCount;

    @Size(max = 255, message = "备注不能超过 255 个字符")
    @Schema(description = "备注")
    private String remark;
}
