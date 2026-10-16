package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SiteContentDetailQuery extends SiteBaseQuery {

    @Schema(description = "内容 ID，与内容编码至少提供一个")
    private Long contentId;

    @Size(max = 128, message = "内容编码最多128个字符")
    @Schema(description = "内容编码，与内容 ID 至少提供一个")
    private String code;

    @Schema(description = "分类 ID")
    @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;
}
