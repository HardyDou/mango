package io.mango.notice.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "系统消息未读分类统计")
public class NoticeUnreadCategoryStatsVO {

    @Schema(description = "未读总数")
    private Long total;

    @Schema(description = "分类数量")
    private List<NoticeUnreadCategoryCountVO> categories = List.of();

    public NoticeUnreadCategoryStatsVO(Long total, List<NoticeUnreadCategoryCountVO> categories) {
        this.total = total;
        setCategories(categories);
    }

    public List<NoticeUnreadCategoryCountVO> getCategories() {
        return List.copyOf(categories);
    }

    public void setCategories(List<NoticeUnreadCategoryCountVO> categories) {
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
