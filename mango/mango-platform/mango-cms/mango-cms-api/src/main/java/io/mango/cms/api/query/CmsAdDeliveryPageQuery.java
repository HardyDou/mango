package io.mango.cms.api.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsAdDeliveryPageQuery extends CmsBasePageQuery {

    private Long siteId;

    private Long adId;

    private String materialType;
}
