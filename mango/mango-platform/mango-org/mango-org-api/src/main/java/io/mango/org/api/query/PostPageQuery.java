package io.mango.org.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "岗位分页查询条件")
public class PostPageQuery extends PageQuery {

    @Schema(description = "岗位名称，支持模糊查询")
    @Size(max = 100, message = "岗位名称长度不能超过100个字符")
    private String postName;

    @Schema(description = "岗位编码，支持模糊查询")
    @Size(max = 50, message = "岗位编码长度不能超过50个字符")
    private String postCode;

    @Schema(description = "岗位状态：0-禁用，1-启用")
    @Pattern(regexp = "[01]", message = "岗位状态只能为0或1")
    private String postStatus;
}
