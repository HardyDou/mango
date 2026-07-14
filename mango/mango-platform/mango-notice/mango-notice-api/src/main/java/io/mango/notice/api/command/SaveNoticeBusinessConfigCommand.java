package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存业务通知发布配置命令")
public class SaveNoticeBusinessConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "参数 schema JSON")
    @jakarta.validation.constraints.Size(max = 65535)
    private String paramsSchema;

    @Schema(description = "默认优先级")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticePriority defaultPriority = NoticePriority.NORMAL;

    @Schema(description = "幂等策略")
    @jakarta.validation.constraints.Size(max = 65535)
    private String idempotentStrategy;
}
