package io.mango.auth.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "企业微信扫码登录公开配置")
public class WecomLoginConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "企业微信 CorpId")
    private String corpId;

    @Schema(description = "企业微信 AgentId")
    private String agentId;

    @Schema(description = "扫码登录回调地址")
    private String redirectUri;
}
