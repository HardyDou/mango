package io.mango.cms.api.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsContentPublishPageQuery extends CmsBasePageQuery {

    private Long contentId;

    private Long siteId;

    private Long categoryId;
}
