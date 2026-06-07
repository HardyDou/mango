package io.mango.template.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板列表项。
 */
@Data
@Schema(description = "模板列表项")
public class TemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    private Long id;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "模板编码")
    private String templateCode;
    @Schema(description = "模板名称")
    private String templateName;
    @Schema(description = "分类编码")
    private String categoryCode;
    @Schema(description = "分类名称")
    private String categoryName;
    @Schema(description = "业务域编码")
    private String domainCode;
    @Deprecated
    @Schema(description = "业务组编码。兼容历史字段，前端不再使用")
    private String businessGroup;
    @Deprecated
    @Schema(description = "业务类型。兼容历史字段，前端不再使用")
    private String businessType;
    @Deprecated
    @Schema(description = "业务KEY。兼容历史字段，新调用统一使用模板编码")
    private String businessKey;
    @Schema(description = "模板源格式")
    private String sourceFormat;
    @Schema(description = "模板状态：0停用，1启用")
    private Integer status;
    @Schema(description = "当前发布版本号")
    private Integer currentVersionNo;
    @Schema(description = "生效版本号")
    private Integer publishedVersionNo;
    @Schema(description = "是否存在未发布变更")
    private Boolean hasUnpublishedChanges;
    @Schema(description = "未发布变更原因")
    private List<String> unpublishedChangeReasons = new ArrayList<>();
    @Schema(description = "未发布草稿源格式")
    private String draftSourceFormat;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
