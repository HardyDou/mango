package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_content_category")
public class CmsContentCategoryEntity extends CmsBaseTenantEntity {
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer sort;
    private String status;
    private String remark;
}
