package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeSyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "业务通知类型")
public class NoticeBusinessTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "业务类型编码")
    private String bizType;

    @Schema(description = "业务类型名称")
    private String bizName;

    @Schema(description = "业务分组")
    private String bizGroup;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "参数 schema JSON")
    private String paramsSchema;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "默认优先级")
    private NoticePriority defaultPriority;

    @Schema(description = "幂等策略")
    private String idempotentStrategy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "同步状态")
    private NoticeSyncStatus syncStatus;

    @Schema(description = "同步状态说明")
    private String syncReason;

    @Schema(description = "生效版本")
    private Integer activeVersion;

    @Schema(description = "草稿版本")
    private Integer draftVersion;

    @Schema(description = "最后发布时间")
    private LocalDateTime lastPublishTime;

    @Schema(description = "开启渠道，逗号分隔")
    private String enabledChannels;
}
