package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "系统消息业务对象")
public class NoticeSiteMessageSubjectCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务对象类型")
    @jakarta.validation.constraints.Size(max = 65535)
    private String subjectType;

    @Schema(description = "业务对象 ID")
    @jakarta.validation.constraints.Size(max = 65535)
    private String subjectId;

    @Schema(description = "业务对象名称快照")
    @jakarta.validation.constraints.Size(max = 65535)
    private String subjectName;
}
