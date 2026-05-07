package io.mango.biz.notification.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 消息通知分页查询条件。
 */
@Data
@Schema(description = "消息通知分页查询条件")
public class NotificationPageQuery {

    @Schema(description = "当前页，从 1 开始")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "是否只查询未读消息")
    private Boolean unreadOnly;
}
