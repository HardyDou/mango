package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "系统消息跳转目标视图")
public class NoticeSiteMessageTargetVO {

    @Schema(description = "跳转目标类型")
    private NoticeSiteMessageTargetType targetType;

    @Schema(description = "命名目标键")
    private String targetKey;

    @Schema(description = "目标参数")
    private NoticeJsonVO params;

    @Schema(description = "打开方式")
    private String openMode;
}
