package io.mango.cms.api.query;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteAdvertisementQuery extends SiteBaseQuery {

    @Size(max = 64, message = "广告位置最多64个字符")
    private String position;
}
