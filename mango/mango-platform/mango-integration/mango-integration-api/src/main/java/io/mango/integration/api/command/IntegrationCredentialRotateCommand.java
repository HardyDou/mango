package io.mango.integration.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 集成凭证轮换命令。
 */
@Data
@Schema(description = "集成凭证轮换命令")
public class IntegrationCredentialRotateCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "凭证编码不能为空")
    @Size(max = 64, message = "凭证编码最多64个字符")
    @Schema(description = "凭证编码")
    private String credentialCode;

    @Size(max = 20000, message = "凭证字段JSON最多20000个字符")
    @Schema(description = "新的凭证字段JSON，未提交的敏感字段保持原值")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String fieldsJson;
}
