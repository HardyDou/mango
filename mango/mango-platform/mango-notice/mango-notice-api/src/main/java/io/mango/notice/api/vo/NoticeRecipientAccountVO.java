package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通知接收账户")
public class NoticeRecipientAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户 ID")
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "账户类型")
    private NoticeRecipientAccountType accountType;

    @Schema(description = "账户标识")
    private String accountValue;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "验证状态")
    private NoticeRecipientAccountStatus verifiedStatus;

    @Schema(description = "是否默认")
    private Boolean defaultAccount;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
