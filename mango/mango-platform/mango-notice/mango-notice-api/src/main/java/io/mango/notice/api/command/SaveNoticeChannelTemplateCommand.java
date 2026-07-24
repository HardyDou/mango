package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelRouteMode;
import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存业务渠道模板命令")
public class SaveNoticeChannelTemplateCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "渠道类型")
    @jakarta.validation.constraints.NotNull(message = "渠道类型不能为空")
    private NoticeChannelType channelType;

    @Schema(description = "模板名称")
    @jakarta.validation.constraints.Size(max = 65535)
    private String templateName;

    @Schema(description = "标题模板")
    @jakarta.validation.constraints.Size(max = 65535)
    private String titleTemplate;

    @Schema(description = "内容模板")
    @jakarta.validation.constraints.Size(max = 65535)
    private String contentTemplate;

    @Schema(description = "三方模板 ID")
    @jakarta.validation.constraints.Size(max = 65535)
    private String channelTemplateId;

    @Schema(description = "变量映射 JSON")
    @jakarta.validation.constraints.Size(max = 65535)
    private String variableMapping;

    @Schema(description = "是否启用")
    @jakarta.validation.constraints.NotNull(
            groups = io.mango.notice.api.validation.NoticeOptionalValidation.class)
    private Boolean enabled = Boolean.TRUE;

    @Schema(description = "绑定渠道配置 ID，空表示 AUTO")
    @jakarta.validation.constraints.Positive
    private Long channelConfigId;

    @Schema(description = "路由模式：EXACT、TAG、AUTO；未传时按旧字段兼容推导")
    private NoticeChannelRouteMode routeMode;

    @Schema(description = "TAG 模式使用的路由标签编码")
    @jakarta.validation.constraints.Size(max = 64)
    private String routeTagCode;
}
