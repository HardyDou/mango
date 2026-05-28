package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存个人参数配置命令。
 */
@Data
@Schema(description = "保存个人参数配置命令")
public class SavePersonalConfigCommand {

    @Schema(description = "配置分组，例如 notice")
    @NotBlank(message = "groupCode不能为空")
    @Size(max = 64, message = "groupCode长度不能超过64")
    private String groupCode;

    @Schema(description = "业务类型，例如 client_reminder")
    @NotBlank(message = "bizType不能为空")
    @Size(max = 64, message = "bizType长度不能超过64")
    private String bizType;

    @Schema(description = "配置键，例如 reminder_setting")
    @NotBlank(message = "configKey不能为空")
    @Size(max = 100, message = "configKey长度不能超过100")
    private String configKey;

    @Schema(description = "配置值，支持JSON字符串")
    @NotBlank(message = "configValue不能为空")
    private String configValue;

    @Schema(description = "值类型：JSON/STRING/NUMBER/BOOLEAN")
    @Size(max = 20, message = "valueType长度不能超过20")
    private String valueType;

    @Schema(description = "配置名称")
    @Size(max = 100, message = "configName长度不能超过100")
    private String configName;

    @Schema(description = "备注")
    @Size(max = 500, message = "remark长度不能超过500")
    private String remark;
}
