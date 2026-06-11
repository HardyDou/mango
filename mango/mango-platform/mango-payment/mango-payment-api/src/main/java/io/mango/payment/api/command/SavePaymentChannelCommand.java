package io.mango.payment.api.command;

import io.mango.payment.api.enums.PaymentChannelCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "保存支付通道命令")
public class SavePaymentChannelCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通道 ID。新增时为空，修改时必填")
    private Long id;

    @NotNull(message = "通道编码不能为空")
    @Schema(description = "通道编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentChannelCode channelCode;

    @NotBlank(message = "通道名称不能为空")
    @Size(max = 128, message = "通道名称不能超过128个字符")
    @Schema(description = "通道名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelName;

    @NotBlank(message = "通道类型不能为空")
    @Size(max = 32, message = "通道类型不能超过32个字符")
    @Schema(description = "通道类型：BUILTIN_VIRTUAL/AGGREGATOR/BANK/DIRECT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelType;

    @NotBlank(message = "适配器类型不能为空")
    @Size(max = 64, message = "适配器类型不能超过64个字符")
    @Schema(description = "适配器类型，例如 MANGO_PAY、ALLINPAY、HUAXIA_BANK", requiredMode = Schema.RequiredMode.REQUIRED)
    private String adapterType;

    @Size(max = 512, message = "网关地址不能超过512个字符")
    @Schema(description = "通道基础网关地址")
    private String gatewayBaseUrl;

    @Schema(description = "签约字段模板 JSON")
    private String fieldTemplateJson;

    @Schema(description = "通道能力摘要")
    private String capabilitySummary;

    @Schema(description = "支持的账单获取方式：MANUAL、FTP、FTPS、HTTP")
    private List<String> billFetchModes;

    @Schema(description = "通道能力列表")
    private List<SavePaymentChannelCapabilityCommand> capabilities;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
