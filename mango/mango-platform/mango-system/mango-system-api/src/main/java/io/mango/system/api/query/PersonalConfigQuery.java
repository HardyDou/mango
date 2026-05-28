package io.mango.system.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人参数配置查询条件。
 */
@Data
@Schema(description = "个人参数配置查询条件")
public class PersonalConfigQuery {

    @Schema(description = "配置分组，例如 notice")
    @Size(max = 64, message = "groupCode长度不能超过64")
    private String groupCode;

    @Schema(description = "业务类型，例如 client_reminder")
    @Size(max = 64, message = "bizType长度不能超过64")
    private String bizType;

    @Schema(description = "配置键，例如 reminder_setting")
    @Size(max = 100, message = "configKey长度不能超过100")
    private String configKey;
}
