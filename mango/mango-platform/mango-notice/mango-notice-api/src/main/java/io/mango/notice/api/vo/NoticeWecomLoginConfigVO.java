package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "企业微信扫码登录渠道配置")
public class NoticeWecomLoginConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "通知渠道配置ID")
    private Long channelConfigId;

    @Schema(description = "渠道名称")
    private String configName;

    @Schema(description = "企业微信 CorpId")
    private String corpId;

    @Schema(description = "企业微信 AgentId")
    private String agentId;

    @Schema(description = "通讯录 Secret，仅内部登录链路使用")
    private String secret;

    @Schema(description = "扫码登录回调地址")
    private String redirectUri;
}
