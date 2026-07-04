package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Schema(description = "系统消息跳转目标")
public class NoticeSiteMessageTargetCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "跳转目标类型")
    private NoticeSiteMessageTargetType targetType = NoticeSiteMessageTargetType.NONE;

    @Schema(description = "命名目标键，ROUTE 为路由名，FLOW 为业务流程键")
    private String targetKey;

    @Schema(description = "目标参数")
    private Map<String, Object> params;

    @Schema(description = "打开方式")
    private String openMode;
}
