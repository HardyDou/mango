package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelConfigStatus;
import io.mango.notice.api.enums.NoticeChannelSendHealthStatus;
import io.mango.notice.api.enums.NoticeChannelSecretStatus;
import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "通知渠道配置")
public class NoticeChannelConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "渠道配置稳定编码")
    private String configCode;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "供应商编码")
    private String providerCode;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "脱敏配置 JSON")
    private String configJson;

    @Schema(description = "Resource 声明 ID")
    private String resourceId;

    @Schema(description = "Resource 版本")
    private Integer resourceVersion;

    @Schema(description = "Resource 模块编码")
    private String resourceModuleCode;

    @Schema(description = "配置来源：MANUAL 或 RESOURCE")
    private String resourceSource;

    @Schema(description = "Secret 完整性状态")
    private NoticeChannelSecretStatus secretStatus;

    @Schema(description = "缺失的 Secret 键")
    private List<String> missingSecretKeys;

    @Schema(description = "绑定的路由标签编码")
    private List<String> routeTagCodes;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "路由权重")
    private Integer weight;

    @Schema(description = "配置状态")
    private NoticeChannelConfigStatus configStatus;

    @Schema(description = "最近发送状态")
    private NoticeChannelSendHealthStatus lastSendStatus;

    @Schema(description = "最近发送时间")
    private LocalDateTime lastSendTime;

    @Schema(description = "最近失败码")
    private String lastFailureCode;

    @Schema(description = "最近失败原因")
    private String lastFailureReason;

    @Schema(description = "频控配置 JSON")
    private String rateLimitConfig;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
