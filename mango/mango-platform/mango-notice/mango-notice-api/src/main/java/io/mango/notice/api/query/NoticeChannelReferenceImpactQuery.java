package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道或路由标签引用影响查询")
public class NoticeChannelReferenceImpactQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道配置 ID")
    @jakarta.validation.constraints.Positive
    private Long configId;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "路由标签编码")
    @jakarta.validation.constraints.Size(max = 64)
    private String routeTagCode;
}
