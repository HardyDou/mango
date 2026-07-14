package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "保存通道签约配置命令")
public class SavePaymentChannelContractCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约配置 ID。新增时为空，修改时必填")
    @Positive(message = "签约配置 ID 必须大于 0")
    private Long id;

    @Size(max = 64, message = "签约编码不能超过64个字符")
    @Schema(description = "系统签约编码，服务端生成，保存时无需传入")
    private String contractCode;

    @NotBlank(message = "签约名称不能为空")
    @Size(max = 128, message = "签约名称不能超过128个字符")
    @Schema(description = "签约名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contractName;

    @NotNull(message = "企业主体不能为空")
    @Schema(description = "企业主体 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long subjectId;

    @NotNull(message = "支付通道不能为空")
    @Schema(description = "支付通道 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long channelId;

    @Size(max = 32, message = "内部路由域不能超过32个字符")
    @Schema(description = "路由域，服务端按支付通道派生，保存时无需传入")
    private String environment;

    @NotBlank(message = "商户号不能为空")
    @Size(max = 64, message = "商户号不能超过64个字符")
    @Schema(description = "商户号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String merchantNo;

    @Schema(description = "通道 AppId")
    @Size(max = 128, message = "通道 AppId 不能超过 128 个字符")
    private String appId;

    @Schema(description = "按通道字段模板填写的配置值 JSON")
    @Size(max = 65535, message = "通道配置值不能超过 65535 个字符")
    private String configValuesJson;

    @Schema(description = "已开通标准支付方式编码，逗号分隔")
    @Size(max = 2048, message = "标准支付方式编码不能超过 2048 个字符")
    private String enabledMethodCodes;

    @Schema(description = "签约能力列表")
    @Valid
    @Size(max = 100, message = "签约能力不能超过 100 项")
    private List<SavePaymentChannelContractCapabilityCommand> capabilities;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
