package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道路由标签查询")
public class NoticeRouteTagQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "标签编码或名称关键词")
    @jakarta.validation.constraints.Size(max = 128)
    private String keyword;
}
