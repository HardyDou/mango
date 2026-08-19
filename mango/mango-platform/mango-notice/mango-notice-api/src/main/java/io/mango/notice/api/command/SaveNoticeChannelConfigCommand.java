package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "保存通知渠道配置命令")
public class SaveNoticeChannelConfigCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_ROUTE_WEIGHT = 100;

    @Schema(description = "渠道配置 ID，传入则更新")
    @jakarta.validation.constraints.Positive
    private Long id;

    @Schema(description = "渠道配置稳定编码；创建后不可变")
    @jakarta.validation.constraints.Size(max = 64)
    private String configCode;

    @Schema(description = "渠道类型")
    @NotNull(message = "渠道类型不能为空")
    private NoticeChannelType channelType;

    @Schema(description = "渠道用途：SEND 仅发送、RECEIVE 仅接收、BOTH 收发一体")
    @NotNull(message = "渠道用途不能为空")
    private NoticeChannelCapabilityMode capabilityMode = NoticeChannelCapabilityMode.SEND;

    @Schema(description = "供应商编码")
    @jakarta.validation.constraints.Size(max = 65535)
    private String providerCode;

    @Schema(description = "配置名称")
    @jakarta.validation.constraints.Size(max = 65535)
    private String configName;

    @Schema(description = "配置 JSON，邮件可配置多个邮箱账号")
    @jakarta.validation.constraints.Size(max = 65535)
    private String configJson;

    @Schema(description = "Secret 补录值；空值不覆盖已有值，普通查询不返回内容")
    @Valid
    @jakarta.validation.constraints.Size(max = 64, message = "Secret 补录项不能超过64个")
    private List<NoticeChannelSecretValueCommand> secretValues;

    @Schema(description = "绑定的路由标签编码")
    @jakarta.validation.constraints.Size(max = 64, message = "路由标签不能超过64个")
    private List<String> routeTagCodes;

    @Schema(description = "是否启用")
    @jakarta.validation.constraints.NotNull(
            groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean enabled = Boolean.TRUE;

    @Schema(description = "优先级")
    @jakarta.validation.constraints.Min(0)
    private Integer priority = 0;

    @Schema(description = "路由权重")
    @jakarta.validation.constraints.Min(0)
    private Integer weight = DEFAULT_ROUTE_WEIGHT;

    @Schema(description = "频控配置 JSON")
    @jakarta.validation.constraints.Size(max = 65535)
    private String rateLimitConfig;

    public List<NoticeChannelSecretValueCommand> getSecretValues() {
        return secretValues == null ? null : List.copyOf(secretValues);
    }

    public void setSecretValues(List<NoticeChannelSecretValueCommand> secretValues) {
        this.secretValues = secretValues == null ? null : List.copyOf(secretValues);
    }

    public List<String> getRouteTagCodes() {
        return routeTagCodes == null ? null : List.copyOf(routeTagCodes);
    }

    public void setRouteTagCodes(List<String> routeTagCodes) {
        this.routeTagCodes = routeTagCodes == null ? null : List.copyOf(routeTagCodes);
    }
}
