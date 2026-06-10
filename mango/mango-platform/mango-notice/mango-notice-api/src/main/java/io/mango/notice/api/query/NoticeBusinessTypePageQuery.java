package io.mango.notice.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "业务通知类型分页查询")
public class NoticeBusinessTypePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private long pageNum = 1;

    @Schema(description = "每页数量")
    private long pageSize = 10;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务分组")
    private String bizGroup;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "是否启用")
    private Boolean enabled;
}
