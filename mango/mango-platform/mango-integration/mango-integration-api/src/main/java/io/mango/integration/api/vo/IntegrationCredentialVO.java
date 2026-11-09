package io.mango.integration.api.vo;

import io.mango.integration.api.enums.IntegrationCredentialStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 凭证结果。
 */
@Data
@Schema(description = "集成凭证")
public class IntegrationCredentialVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "凭证编码")
    private String credentialCode;

    @Schema(description = "连接器编码")
    private String connectorCode;

    @Schema(description = "非敏感字段值，敏感字段只返回掩码")
    private String maskedFieldsJson;

    @Schema(description = "敏感字段版本JSON")
    private String fieldVersionsJson;

    @Schema(description = "最近轮换时间")
    private LocalDateTime rotatedAt;

    @Schema(description = "到期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "状态")
    private IntegrationCredentialStatus status;

    @Schema(description = "备注")
    private String remark;
}
