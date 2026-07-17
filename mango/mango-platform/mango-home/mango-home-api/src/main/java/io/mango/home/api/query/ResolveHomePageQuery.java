package io.mango.home.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "当前用户首页解析条件")
public class ResolveHomePageQuery implements Serializable {

    @Size(max = 128, message = "首页标识长度不能超过128")
    @Schema(description = "首页路由标识；为空时解析默认首页")
    private String homeId;
}
