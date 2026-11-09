package io.mango.integration.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 凭证字段声明，供业务域读取解密后的凭证值。
 */
@Data
@Schema(description = "凭证字段声明")
public class IntegrationCredentialFieldVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "字段编码")
    private String fieldCode;

    @Schema(description = "字段值，敏感字段为解密后的明文，仅允许在内存中使用")
    private String fieldValue;
}
