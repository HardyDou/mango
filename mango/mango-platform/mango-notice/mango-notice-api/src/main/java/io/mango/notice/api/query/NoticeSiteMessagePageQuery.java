package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统消息分页查询")
public class NoticeSiteMessagePageQuery {

    @Schema(description = "当前页，从 1 开始")
    @jakarta.validation.constraints.Min(0)
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    @jakarta.validation.constraints.Min(0)
    private Integer pageSize = 10;

    @Schema(description = "是否只查询未读系统消息")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean unreadOnly;

    @Schema(description = "业务类型")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizType;

    @Schema(description = "业务域")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizGroup;

    @Schema(description = "通知优先级")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private NoticePriority priority;

    @Schema(description = "关键字，匹配标题和内容")
    @jakarta.validation.constraints.Size(max = 65535)
    private String keyword;

    @Schema(description = "接收开始时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime startTime;

    @Schema(description = "接收结束时间")
    @jakarta.validation.constraints.NotNull(groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private LocalDateTime endTime;

    @Schema(description = "业务对象ID")
    @jakarta.validation.constraints.Size(max = 65535)
    private String bizId;
}
