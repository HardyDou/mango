package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统消息未读分类统计")
public class NoticeUnreadCategoryStatsVO {

    @Schema(description = "未读总数")
    private Long total;

    @Schema(description = "分类数量")
    private List<NoticeUnreadCategoryCountVO> categories;
}
