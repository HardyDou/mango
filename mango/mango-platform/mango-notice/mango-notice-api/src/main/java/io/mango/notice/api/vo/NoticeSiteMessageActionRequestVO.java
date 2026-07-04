package io.mango.notice.api.vo;

import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "系统消息动作请求视图")
public class NoticeSiteMessageActionRequestVO {

    @Schema(description = "动作请求 ID")
    private String requestId;

    @Schema(description = "系统消息 ID")
    private Long messageId;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "事件 ID")
    private String eventId;

    @Schema(description = "请求状态")
    private NoticeSiteMessageActionRequestStatus status;

    @Schema(description = "失败码")
    private String failCode;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "业务处理结果")
    private Map<String, Object> result;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间")
    private LocalDateTime finishedAt;
}
