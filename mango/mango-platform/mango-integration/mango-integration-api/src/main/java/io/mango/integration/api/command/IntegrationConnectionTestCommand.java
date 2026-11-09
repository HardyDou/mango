package io.mango.integration.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 集成实例连接测试命令。
 */
@Data
@Schema(description = "集成实例连接测试命令")
public class IntegrationConnectionTestCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "实例编码不能为空")
    @Size(max = 64, message = "实例编码最多64个字符")
    @Schema(description = "实例编码")
    private String instanceCode;

    @Size(max = 200, message = "测试路径最多200个字符")
    @Schema(description = "相对测试路径，默认斜杠")
    private String testPath;
}
