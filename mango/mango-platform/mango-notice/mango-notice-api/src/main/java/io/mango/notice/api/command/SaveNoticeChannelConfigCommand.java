package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "保存通知渠道配置命令")
public class SaveNoticeChannelConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道配置 ID，传入则更新")
    @jakarta.validation.constraints.Positive
    private Long id;

    @Schema(description = "渠道配置稳定编码；创建后不可变")
    @jakarta.validation.constraints.Size(max = 64)
    private String configCode;

    @Schema(description = "渠道类型")
    @NotNull(message = "渠道类型不能为空")
    private NoticeChannelType channelType;

    @Schema(description = "供应商编码")
    @jakarta.validation.constraints.Size(max = 65535)
    private String providerCode;

    @Schema(description = "配置名称")
    @jakarta.validation.constraints.Size(max = 65535)
    private String configName;

    @Schema(description = "配置 JSON，邮件可配置多个邮箱账号")
    @jakarta.validation.constraints.Size(max = 65535)
    private String configJson;

    @Schema(description = "Secret 补录值，只写不回显；空值不覆盖已有值")
    private Map<String, String> secretValues;

    @Schema(description = "绑定的路由标签编码")
    private List<String> routeTagCodes;

    @Schema(description = "是否启用")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean enabled = Boolean.TRUE;

    @Schema(description = "优先级")
    @jakarta.validation.constraints.Min(0)
    private Integer priority = 0;

    @Schema(description = "路由权重")
    @jakarta.validation.constraints.Min(0)
    private Integer weight = 100;

    @Schema(description = "频控配置 JSON")
    @jakarta.validation.constraints.Size(max = 65535)
    private String rateLimitConfig;
}
