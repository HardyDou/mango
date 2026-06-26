package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告发布对象")
public class NoticeAnnouncementTargetCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "发布对象类型不能为空")
    @Schema(description = "发布对象类型：ALL、ORG、ROLE、USER")
    private NoticeAnnouncementTargetType targetType;

    @Schema(description = "发布对象ID。ALL 可为空")
    private Long targetId;

    @Schema(description = "发布对象名称，仅用于快照回显")
    private String targetName;

    @Schema(description = "组织是否包含下级。首期保存快照，解析由身份服务目标接口负责")
    private Boolean includeChildren;
}
