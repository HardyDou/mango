package io.mango.auth.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "企业微信扫码登录命令")
public class WecomLoginCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "企业微信授权码不能为空")
    @Size(max = 256, message = "企业微信授权码最多256个字符")
    @Schema(description = "企业微信扫码或工作台授权返回的 code")
    private String code;

    @Schema(description = "企业微信通知渠道配置ID；为空时使用当前机构启用的第一个企微扫码配置")
    private Long channelConfigId;

    @Size(max = 64, message = "机构ID最多64个字符")
    @Schema(description = "机构ID")
    private String tenantId;

    @Size(max = 50, message = "机构编码最多50个字符")
    @Schema(description = "机构编码")
    private String tenantCode;

    @Size(max = 64, message = "应用编码最多64个字符")
    @Schema(description = "应用编码")
    private String appCode;
}
