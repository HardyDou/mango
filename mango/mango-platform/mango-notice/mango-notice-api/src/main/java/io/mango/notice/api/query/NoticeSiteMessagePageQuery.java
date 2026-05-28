package io.mango.notice.api.query;

import io.mango.notice.api.enums.NoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "系统消息分页查询")
public class NoticeSiteMessagePageQuery {

    @Schema(description = "当前页，从 1 开始")
    private Integer pageNum = 1;

    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "是否只查询未读系统消息")
    private Boolean unreadOnly;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务域")
    private String bizGroup;

    @Schema(description = "通知优先级")
    private NoticePriority priority;

    @Schema(description = "关键字，匹配标题和内容")
    private String keyword;

    @Schema(description = "接收开始时间")
    private LocalDateTime startTime;

    @Schema(description = "接收结束时间")
    private LocalDateTime endTime;

    @Schema(description = "业务对象ID")
    private String bizId;
}
