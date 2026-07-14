package io.mango.workflow.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程分类实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workflow_category")
public class WorkflowCategoryEntity extends WorkflowBaseEntity {

    private String categoryName;
    private String categoryCode;
    private String domainCode;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
