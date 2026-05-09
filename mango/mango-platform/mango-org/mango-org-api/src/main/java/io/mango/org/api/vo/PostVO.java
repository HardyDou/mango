package io.mango.org.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "岗位信息")
public class PostVO {

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "岗位编码")
    private String postCode;

    @Schema(description = "排序值")
    private Integer postSort;

    @Schema(description = "岗位状态：0-禁用，1-启用")
    private String postStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
