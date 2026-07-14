package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
@Data
@Schema(description = "执行我的系统消息动作命令")
public class ExecuteNoticeSiteMessageActionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "系统消息 ID")
    @jakarta.validation.constraints.Positive
    private Long messageId;

    @Schema(description = "动作编码")
    @jakarta.validation.constraints.NotBlank
    private String actionCode;

    @Schema(description = "动作输入")
    @jakarta.validation.Valid
    private NoticeJsonRequest input;
}
