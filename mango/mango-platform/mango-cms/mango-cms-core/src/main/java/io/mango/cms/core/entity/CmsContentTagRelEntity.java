package io.mango.cms.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("cms_content_tag_rel")
public class CmsContentTagRelEntity extends CmsBaseTenantEntity {
    private Long contentId;
    private Long tagId;
}
