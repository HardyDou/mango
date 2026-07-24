package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知渠道路由标签查询")
public class NoticeRouteTagQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private NoticeChannelType channelType;

    private String keyword;
}
