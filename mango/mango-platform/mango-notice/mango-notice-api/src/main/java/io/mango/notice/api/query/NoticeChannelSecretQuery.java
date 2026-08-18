package io.mango.notice.api.query;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道单字段 Secret 查看参数")
public class NoticeChannelSecretQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "渠道配置 ID 不能为空")
    @Positive(message = "渠道配置 ID 必须为正数")
    @Schema(description = "渠道配置 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long channelConfigId;

    @NotBlank(message = "Secret 字段不能为空")
    @Size(max = 64, message = "Secret 字段长度不能超过64个字符")
    @Pattern(regexp = "[A-Za-z][A-Za-z0-9]*", message = "Secret 字段格式不正确")
    @Schema(description = "渠道定义允许的单个 Secret 字段", requiredMode = Schema.RequiredMode.REQUIRED)
    private String secretKey;
}
