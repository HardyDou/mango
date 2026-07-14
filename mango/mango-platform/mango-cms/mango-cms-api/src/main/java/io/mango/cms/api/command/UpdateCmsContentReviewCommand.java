package io.mango.cms.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCmsContentReviewCommand {

    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "主键 ID")
    private Long id;

    @Size(max = 1024, message = "审核意见最多1024个字符")
    @Schema(description = "审核意见")
    private String reviewComment;
}
