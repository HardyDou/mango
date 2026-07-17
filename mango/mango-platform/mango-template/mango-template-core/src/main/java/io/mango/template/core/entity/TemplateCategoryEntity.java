package io.mango.template.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板分类实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("template_category")
public class TemplateCategoryEntity extends TenantEntity {

    private String categoryCode;
    private String categoryName;
    private Integer sort;
    private Integer status;
    private String remark;
}
