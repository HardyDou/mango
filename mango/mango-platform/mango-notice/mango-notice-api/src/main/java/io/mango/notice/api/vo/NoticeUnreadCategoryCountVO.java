package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeSiteMessageCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统消息未读分类数量")
public class NoticeUnreadCategoryCountVO {

    @Schema(description = "消息分类")
    private NoticeSiteMessageCategory category;

    @Schema(description = "未读数量")
    private Long count;
}
