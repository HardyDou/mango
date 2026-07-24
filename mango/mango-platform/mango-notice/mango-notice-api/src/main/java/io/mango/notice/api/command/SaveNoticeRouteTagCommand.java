package io.mango.notice.api.command;

import io.mango.notice.api.enums.NoticeChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "保存通知渠道路由标签命令")
public class SaveNoticeRouteTagCommand implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "标签 ID，传入则更新")
    @jakarta.validation.constraints.Positive
    private Long id;

    @Schema(description = "渠道类型")
    @NotNull(message = "渠道类型不能为空")
    private NoticeChannelType channelType;

    @Schema(description = "标签编码")
    @NotBlank(message = "标签编码不能为空")
    @Size(max = 64, message = "标签编码长度不能超过64")
    private String tagCode;

    @Schema(description = "标签名称")
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 128, message = "标签名称长度不能超过128")
    private String tagName;

    @Schema(description = "标签说明")
    @Size(max = 500, message = "标签说明长度不能超过500")
    private String description;
}
