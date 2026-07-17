package io.mango.template.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 模板渲染记录分页查询。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板渲染记录分页查询")
public class TemplateRenderRecordPageQuery extends PageQuery {

    @Size(max = 128, message = "模板编码不能超过128个字符")
    @Schema(description = "模板编码")
    private String templateCode;

    @Size(max = 32, message = "渲染状态不能超过32个字符")
    @Schema(description = "渲染状态：PENDING、RUNNING、SUCCESS、FAILED")
    private String status;

    @Size(max = 64, message = "业务类型不能超过64个字符")
    @Schema(description = "业务类型")
    private String bizType;

    @Size(max = 128, message = "业务ID不能超过128个字符")
    @Schema(description = "业务ID")
    private String bizId;
}
