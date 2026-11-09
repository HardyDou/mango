package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 凭证轮换记录。
 */
@Data
@Schema(description = "凭证轮换记录")
public class IntegrationCredentialRotationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "凭证编码")
    private String credentialCode;

    @Schema(description = "轮换后版本")
    private Integer version;

    @Schema(description = "本次更新的字段编码")
    private String fieldCodes;

    @Schema(description = "操作人")
    private Long operatorId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "轮换时间")
    private LocalDateTime rotatedAt;
}
