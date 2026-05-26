package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存通知设置命令")
public class SaveNoticeSettingsCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否启用提示音")
    private Boolean soundEnabled;

    @Schema(description = "是否启用桌面通知")
    private Boolean desktopEnabled;

    @Min(value = 0, message = "最大重试次数不能小于0")
    @Schema(description = "最大重试次数")
    private Integer maxRetry;

    @Min(value = 1, message = "消息保留天数不能小于1")
    @Schema(description = "消息保留天数")
    private Integer retentionDays;
}
