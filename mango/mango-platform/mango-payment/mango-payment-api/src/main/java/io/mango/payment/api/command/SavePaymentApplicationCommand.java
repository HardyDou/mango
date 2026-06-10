package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存支付接入应用命令")
public class SavePaymentApplicationCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "应用 ID。新增时为空，修改时必填")
    private Long id;

    @NotBlank(message = "应用名称不能为空")
    @Size(max = 128, message = "应用名称不能超过128个字符")
    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appName;

    @NotNull(message = "IP 白名单开关不能为空")
    @Schema(description = "IP 白名单开关：1-开启，0-关闭", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer ipWhitelistEnabled;

    @Size(max = 1024, message = "IP 白名单不能超过1024个字符")
    @Schema(description = "IP 白名单，多个值用逗号或换行分隔")
    private String ipWhitelist;

    @NotNull(message = "报文加密开关不能为空")
    @Schema(description = "请求报文加密开关", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer payloadEncryptEnabled;

    @Size(max = 32, message = "签名算法不能超过32个字符")
    @Schema(description = "签名算法。报文加密开启时必填")
    private String signAlgorithm;

    @Size(max = 512, message = "通知重试策略不能超过512个字符")
    @Schema(description = "通知重试策略")
    private String notifyRetryPolicy;

    @NotNull(message = "示例应用标记不能为空")
    @Schema(description = "是否示例应用：1-是，0-否", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer demoApp;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：1-启用，0-停用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
