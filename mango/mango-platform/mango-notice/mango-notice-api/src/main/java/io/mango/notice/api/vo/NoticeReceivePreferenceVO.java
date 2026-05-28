package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.enums.NoticeReceivePreferenceScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "通知接收偏好")
public class NoticeReceivePreferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "偏好 ID")
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "范围类型")
    private NoticeReceivePreferenceScopeType scopeType;

    @Schema(description = "范围值")
    private String scopeValue;

    @Schema(description = "渠道类型")
    private NoticeChannelType channelType;

    @Schema(description = "是否接收")
    private Boolean enabled;

    @Schema(description = "接收账户 ID")
    private Long accountId;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
