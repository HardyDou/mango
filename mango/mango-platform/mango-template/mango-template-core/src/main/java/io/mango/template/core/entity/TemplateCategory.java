package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板分类实体。
 */
@Data
@TableName("template_category")
public class TemplateCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String categoryCode;
    private String categoryName;
    private Integer sort;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private LocalDateTime updatedAt;
}
