package io.mango.home.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户首页视图查询条件")
public class UserHomeViewQuery implements Serializable {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "成员ID。不传时只按用户维度查询")
    private Long memberId;

    @Schema(description = "组织ID。不传时不解析部门继承")
    private Long orgId;
}
