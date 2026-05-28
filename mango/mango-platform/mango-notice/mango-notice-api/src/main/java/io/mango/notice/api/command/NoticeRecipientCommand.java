package io.mango.notice.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "通知接收人命令")
public class NoticeRecipientCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "接收人名称")
    private String recipientName;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "微信 openid")
    private String wechatOpenid;

    @Schema(description = "企业微信用户ID")
    private String wecomUserId;

    @Schema(description = "钉钉用户ID")
    private String dingtalkUserId;

    @Schema(description = "外部联系人标识")
    private String externalId;
}
