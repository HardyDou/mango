package io.mango.cms.api.query;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsContentPageQuery extends CmsBasePageQuery {

    private Long categoryId;

    @Pattern(regexp = "|ARTICLE|IMAGE_TEXT|PAGE|ATTACHMENT|VIDEO", message = "内容类型不合法")
    private String contentType;
}
