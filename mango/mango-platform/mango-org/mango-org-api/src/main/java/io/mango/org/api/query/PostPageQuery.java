package io.mango.org.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "岗位分页查询条件")
public class PostPageQuery extends PageQuery {

    @Schema(description = "岗位名称，支持模糊查询")
    private String postName;

    @Schema(description = "岗位编码，支持模糊查询")
    private String postCode;

    @Schema(description = "岗位状态：0-禁用，1-启用")
    private String postStatus;
}
