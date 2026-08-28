package io.mango.resource.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Per-resource synchronization context sent to a target module.
 */
@Data
public class ResourceSyncContextCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "资源ID不能为空")
    @Schema(description = "稳定资源ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String resourceId;

    @PastOrPresent(message = "上次同步时间不能晚于当前时间")
    @Schema(description = "Registry 中记录的上次目标同步时间")
    private LocalDateTime previousSyncTime;

    @NotNull(message = "本次同步时间不能为空")
    @Schema(description = "Handler 与 Registry 必须共同使用的固定同步时间",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime synchronizationTime;

    @Positive(message = "目标数据主键必须为正数")
    @Schema(description = "上次同步得到的目标数据主键")
    private Long targetId;

    @Size(max = 128, message = "目标数据表长度不能超过128")
    @Schema(description = "上次同步得到的目标数据表")
    private String targetTable;
}
