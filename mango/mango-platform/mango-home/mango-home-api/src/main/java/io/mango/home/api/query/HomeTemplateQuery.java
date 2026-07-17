package io.mango.home.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "首页模板查询条件")
public class HomeTemplateQuery implements Serializable {

    @Size(max = 64, message = "模板名称关键字长度不能超过64")
    @Schema(description = "模板名称关键字")
    private String keyword;

    @Pattern(regexp = "(?i:true|false)", message = "启用状态必须为true或false")
    @Schema(description = "是否启用")
    private String enabled;
}
