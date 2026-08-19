package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知渠道单字段 Secret 明文；响应禁止缓存")
public class NoticeChannelSecretVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道配置 ID")
    private Long channelConfigId;

    @Schema(description = "Secret 字段")
    private String secretKey;

    @Schema(description = "Secret 明文")
    private String value;
}
