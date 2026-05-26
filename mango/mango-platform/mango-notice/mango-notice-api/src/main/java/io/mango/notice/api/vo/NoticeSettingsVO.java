package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知设置")
public class NoticeSettingsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否启用提示音")
    private Boolean soundEnabled;

    @Schema(description = "是否启用桌面通知")
    private Boolean desktopEnabled;

    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Schema(description = "消息保留天数")
    private Integer retentionDays;
}
