package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.enums.NoticeTemplateVersionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "业务通知发布配置版本")
public class NoticeBusinessConfigVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "业务类型 ID")
    private Long businessTypeId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "参数 schema JSON")
    private String paramsSchema;

    @Schema(description = "默认优先级")
    private NoticePriority defaultPriority;

    @Schema(description = "幂等策略")
    private String idempotentStrategy;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "版本状态")
    private NoticeTemplateVersionStatus versionStatus;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
