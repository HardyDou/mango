package io.mango.workflow.api.query;

import io.mango.common.po.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流程定义分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程定义分页查询")
public class WorkflowDefinitionPageQuery extends PageQuery {

    @Schema(description = "关键字，支持按流程名称或流程编码模糊查询")
    private String keyword;

    @Schema(description = "流程分类ID")
    private Long categoryId;

    @Schema(description = "业务域编码")
    private String domainCode;

    @Schema(description = "所属组织ID")
    private Long orgId;

    @Schema(description = "流程状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-停用")
    private String status;

    @Schema(description = "是否仅返回已发布生效快照；发起流程列表应传 true，避免读取未发布草稿")
    private Boolean publishedOnly;

    @Schema(description = "启动入口是否可见；审批中心发起流程列表应传 true")
    private Boolean startEntryVisible;
}
