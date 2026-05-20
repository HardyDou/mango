package io.mango.workflow.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 流程模板视图。
 */
@Data
@Schema(description = "流程模板视图")
public class WorkflowTemplateVO {

    @Schema(description = "流程模板ID")
    private Long id;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "流程模板分类ID")
    private Long templateCategoryId;

    @Schema(description = "流程模板分类名称")
    private String templateCategoryName;

    @Schema(description = "业务场景编码")
    private String categoryCode;

    @Schema(description = "业务场景名称")
    private String categoryName;

    @Schema(description = "流程图标")
    private String icon;

    @Schema(description = "流程管理员用户名列表")
    private List<String> adminUsers;

    @Schema(description = "设计器JSON内容")
    private String designerJson;

    @Schema(description = "表单编码")
    private String formCode;

    @Schema(description = "动态表单JSON配置")
    private String formJson;

    @Schema(description = "模板版本号")
    private Integer versionNo;

    @Schema(description = "是否当前版本")
    private Boolean latestFlag;

    @Schema(description = "模板状态")
    private String status;

    @Schema(description = "模板状态名称")
    private String statusName;

    @Schema(description = "来源流程定义ID")
    private Long sourceDefinitionId;

    @Schema(description = "来源流程编码")
    private String sourceDefinitionKey;

    @Schema(description = "来源流程名称")
    private String sourceDefinitionName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
}
