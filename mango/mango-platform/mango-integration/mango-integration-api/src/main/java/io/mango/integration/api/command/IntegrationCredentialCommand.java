package io.mango.integration.api.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 集成凭证保存命令。
 *
 * <p>敏感字段只在写入时提交，响应不返回原文。</p>
 */
@Data
@Schema(description = "集成凭证保存命令")
public class IntegrationCredentialCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "凭证编码不能为空")
    @Size(max = 64, message = "凭证编码最多64个字符")
    @Schema(description = "凭证编码")
    private String credentialCode;

    @NotBlank(message = "连接器编码不能为空")
    @Size(max = 64, message = "连接器编码最多64个字符")
    @Schema(description = "连接器编码")
    private String connectorCode;

    @Size(max = 20000, message = "凭证字段JSON最多20000个字符")
    @Schema(description = "凭证字段JSON对象，敏感字段为明文提交；证书类字段直接提交文本内容")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String fieldsJson;

    @Schema(description = "到期时间")
    private LocalDateTime expiresAt;

    @Size(max = 500, message = "备注最多500个字符")
    @Schema(description = "备注")
    private String remark;
}
