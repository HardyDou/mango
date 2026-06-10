package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "保存通道签约配置命令")
public class SavePaymentChannelContractCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "签约配置 ID。新增时为空，修改时必填")
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

    @Size(max = 32, message = "接入场景不能超过32个字符")
    @Schema(description = "路由域，服务端按支付通道派生，保存时无需传入")
    private String environment;

    @NotBlank(message = "商户号不能为空")
    @Size(max = 64, message = "商户号不能超过64个字符")
    @Schema(description = "商户号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String merchantNo;

    @Schema(description = "通道 AppId")
    private String appId;

    @Schema(description = "按通道字段模板填写的配置值 JSON")
    private String configValuesJson;

    @Schema(description = "已开通标准支付方式编码，逗号分隔")
    private String enabledMethodCodes;

    @Schema(description = "签约能力列表")
    private List<SavePaymentChannelContractCapabilityCommand> capabilities;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
