package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeRecipientAccountStatus;
import io.mango.notice.api.enums.NoticeRecipientAccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存通知接收账户命令。
 */
@Data
@Schema(description = "保存通知接收账户命令")
public class SaveNoticeRecipientAccountCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户 ID，更新时必填")
    private Long id;

    @Schema(description = "用户 ID；为空时使用当前用户")
    private Long userId;

    @NotNull(message = "账户类型不能为空")
    @Schema(description = "账户类型")
    private NoticeRecipientAccountType accountType;

    @NotBlank(message = "账户标识不能为空")
    @Schema(description = "账户标识")
    private String accountValue;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "验证状态")
    private NoticeRecipientAccountStatus verifiedStatus;

    @Schema(description = "是否默认账户")
    private Boolean defaultAccount;
}
