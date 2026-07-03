package io.mango.home.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "首页模板查询条件")
public class HomeTemplateQuery implements Serializable {

    @Schema(description = "模板名称关键字")
    private String keyword;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
