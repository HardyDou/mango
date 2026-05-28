package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 保存通知接收偏好命令。
 */
@Data
@Schema(description = "保存通知接收偏好命令")
public class SaveNoticeReceivePreferenceCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID；为空时使用当前用户")
    private Long userId;

    @NotNull(message = "范围类型不能为空")
    @Schema(description = "范围类型：GLOBAL、BIZ_GROUP、BIZ_TYPE")
    private NoticeReceivePreferenceScopeType scopeType;

    @Schema(description = "范围值：业务域或业务类型；GLOBAL 为空")
    private String scopeValue;

    @Schema(description = "渠道类型；为空表示总开关")
    private NoticeChannelType channelType;

    @NotNull(message = "是否接收不能为空")
    @Schema(description = "是否接收")
    private Boolean enabled;

    @Schema(description = "指定接收账户 ID")
    private Long accountId;
}
