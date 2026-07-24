package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道 Secret 补录项")
public class NoticeChannelSecretValueCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "Secret 配置键")
    @NotBlank(message = "Secret 配置键不能为空")
    @Size(max = 64, message = "Secret 配置键长度不能超过64")
    private String key;

    @Schema(description = "Secret 配置值，只写不回显")
    @NotBlank(message = "Secret 配置值不能为空")
    @Size(max = 65535, message = "Secret 配置值长度不能超过65535")
    private String value;
}
