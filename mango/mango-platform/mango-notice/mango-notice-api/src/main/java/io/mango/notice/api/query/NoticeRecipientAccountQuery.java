package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知接收账户查询。
 */
@Data
@Schema(description = "通知接收账户查询")
public class NoticeRecipientAccountQuery {

    @Schema(description = "用户 ID；为空时使用当前用户")
    private Long userId;

    @Schema(description = "账户类型")
    private NoticeRecipientAccountType accountType;
}
