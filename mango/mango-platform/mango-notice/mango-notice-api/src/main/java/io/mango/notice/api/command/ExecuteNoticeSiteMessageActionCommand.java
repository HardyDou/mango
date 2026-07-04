package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema(description = "执行我的系统消息动作命令")
public class ExecuteNoticeSiteMessageActionCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "动作输入")
    private Map<String, Object> input;
}
