package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticeAnnouncementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "公告管理分页查询")
public class NoticeAnnouncementPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Schema(description = "当前页，从 1 开始")
    private long pageNum = 1;

    @Schema(description = "每页大小")
    private long pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "公告状态")
    private NoticeAnnouncementStatus status;

    @Schema(description = "关键字，匹配标题和内容")
    private String keyword;
}
