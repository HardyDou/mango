package io.mango.notice.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "我的公告分页查询")
public class MyNoticeAnnouncementPageQuery implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;

    @Schema(description = "当前页，从 1 开始")
    @jakarta.validation.constraints.Positive
    private long pageNum = 1;

    @Schema(description = "每页大小")
    @jakarta.validation.constraints.Positive
    private long pageSize = DEFAULT_PAGE_SIZE;

    @Schema(description = "是否只查询未读公告")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean unreadOnly;

    @Schema(description = "是否只查询待确认公告")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean pendingConfirmOnly;

    @Schema(description = "关键字，匹配标题和内容")
    @jakarta.validation.constraints.Size(max = 65535)
    private String keyword;
}
