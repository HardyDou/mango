package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeSiteMessageActionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "完成系统消息动作命令")
public class CompleteNoticeSiteMessageActionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "动作请求 ID 不能为空")
    @Schema(description = "动作请求 ID")
    private String requestId;

    @NotNull(message = "动作结果状态不能为空")
    @Schema(description = "动作结果状态")
    private NoticeSiteMessageActionRequestStatus status;

    @Schema(description = "失败码")
    @jakarta.validation.constraints.Size(max = 65535)
    private String failCode;

    @Schema(description = "失败原因")
    @jakarta.validation.constraints.Size(max = 65535)
    private String failReason;

    @Schema(description = "业务处理结果")
    @jakarta.validation.Valid
    private NoticeJsonRequest result;
}
