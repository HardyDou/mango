package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板渲染记录分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板渲染记录分页查询")
public class TemplateRenderRecordPageQuery extends PageQuery {

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "渲染状态：PENDING、RUNNING、SUCCESS、FAILED")
    private String status;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;
}
