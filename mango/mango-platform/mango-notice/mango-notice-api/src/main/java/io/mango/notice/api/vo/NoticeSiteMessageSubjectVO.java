package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统消息业务对象视图")
public class NoticeSiteMessageSubjectVO {

    @Schema(description = "业务对象类型")
    private String subjectType;

    @Schema(description = "业务对象 ID")
    private String subjectId;

    @Schema(description = "业务对象名称快照")
    private String subjectName;
}
