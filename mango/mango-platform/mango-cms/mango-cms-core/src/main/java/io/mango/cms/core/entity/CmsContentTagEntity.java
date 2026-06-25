package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_content_tag")
public class CmsContentTagEntity extends CmsBaseTenantEntity {
    private String tagCode;
    private String tagName;
    private Integer sort;
    private String status;
    private String remark;
}
