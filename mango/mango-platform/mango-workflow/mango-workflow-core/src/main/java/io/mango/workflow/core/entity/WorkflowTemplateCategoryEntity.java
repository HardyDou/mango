package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程模板分类实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_template_category")
public class WorkflowTemplateCategoryEntity extends WorkflowBaseEntity {

    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private String icon;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
