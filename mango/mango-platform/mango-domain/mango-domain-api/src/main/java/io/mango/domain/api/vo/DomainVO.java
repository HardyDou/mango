package io.mango.domain.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务域视图。
 */
@Data
@Schema(description = "业务域视图")
public class DomainVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务域ID")
    private Long id;

    @Schema(description = "租户标识")
    private String tenantId;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "业务域编码简写")
    private String domainShortCode;

    @Schema(description = "业务域名称")
    private String domainName;

    @Schema(description = "父业务域ID")
    private Long parentId;

    @Schema(description = "父业务域名称")
    private String parentName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0停用，1启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private Long createdBy;

    @Schema(description = "标准创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新人ID")
    private Long updatedBy;

    @Schema(description = "标准更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "子业务域")
    private List<DomainVO> children = new ArrayList<>();
}
