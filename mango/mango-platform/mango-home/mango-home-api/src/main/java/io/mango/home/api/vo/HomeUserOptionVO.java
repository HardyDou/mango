package io.mango.home.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** 首页管理当前租户用户候选项。 */
@Data
@Schema(description = "首页管理当前租户用户候选项")
public class HomeUserOptionVO implements Serializable {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "当前租户成员ID")
    private Long memberId;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "登录用户名")
    private String username;
}
