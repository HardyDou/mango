package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建业务通知类型命令")
public class CreateNoticeBusinessTypeCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型编码")
    @NotBlank(message = "业务类型编码不能为空")
    private String bizType;

    @Schema(description = "业务类型名称")
    @NotBlank(message = "业务类型名称不能为空")
    private String bizName;

    @Schema(description = "业务分组")
    private String bizGroup;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "参数 schema JSON")
    private String paramsSchema;

    @Schema(description = "默认优先级")
    private NoticePriority defaultPriority = NoticePriority.NORMAL;

    @Schema(description = "幂等策略")
    private String idempotentStrategy;
}
