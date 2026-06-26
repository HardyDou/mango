package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeAnnouncementTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告发布对象快照")
public class NoticeAnnouncementTargetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "公告ID")
    private Long announcementId;

    @Schema(description = "发布对象类型")
    private NoticeAnnouncementTargetType targetType;

    @Schema(description = "发布对象ID")
    private Long targetId;

    @Schema(description = "发布对象名称")
    private String targetName;

    @Schema(description = "组织是否包含下级")
    private Boolean includeChildren;
}
