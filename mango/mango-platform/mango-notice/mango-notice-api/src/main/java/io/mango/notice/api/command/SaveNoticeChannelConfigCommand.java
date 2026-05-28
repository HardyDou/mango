package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存通知渠道配置命令")
public class SaveNoticeChannelConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道配置 ID，传入则更新")
    private Long id;

    @Schema(description = "渠道类型")
    @NotNull(message = "渠道类型不能为空")
    private NoticeChannelType channelType;

    @Schema(description = "供应商编码")
    private String providerCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "配置 JSON，邮件可配置多个邮箱账号")
    private String configJson;

    @Schema(description = "是否启用")
    private Boolean enabled = Boolean.TRUE;

    @Schema(description = "优先级")
    private Integer priority = 0;

    @Schema(description = "路由权重")
    private Integer weight = 100;

    @Schema(description = "频控配置 JSON")
    private String rateLimitConfig;
}
