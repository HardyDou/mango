package io.mango.notice.api.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "通知渠道账号路由模式")
public enum NoticeChannelRouteMode {
    EXACT,
    TAG,
    AUTO
}
