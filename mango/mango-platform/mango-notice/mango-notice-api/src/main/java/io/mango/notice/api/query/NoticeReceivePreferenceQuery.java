package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知接收偏好查询。
 */
@Data
@Schema(description = "通知接收偏好查询")
public class NoticeReceivePreferenceQuery {

    @Schema(description = "用户 ID；为空时使用当前用户")
    private Long userId;

    @Schema(description = "范围类型")
    private NoticeReceivePreferenceScopeType scopeType;

    @Schema(description = "范围值")
    private String scopeValue;
}
