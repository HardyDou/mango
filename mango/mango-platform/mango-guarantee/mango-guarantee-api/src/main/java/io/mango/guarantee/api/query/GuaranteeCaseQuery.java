package io.mango.guarantee.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 保函业务单列表查询条件。
 */
@Data
@Schema(description = "保函业务单列表查询条件")
public class GuaranteeCaseQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务单标题，支持模糊查询")
    private String title;

    @Schema(description = "申请人名称，支持模糊查询")
    private String applicantName;

    @Schema(description = "保函类型编码")
    private String guaranteeType;

    @Schema(description = "状态：0-草稿，1-处理中，2-已完成，9-已取消")
    private Integer status;

    @Schema(description = "当前页，从1开始")
    private Integer page = 1;

    @Schema(description = "每页条数")
    private Integer size = 10;
}
