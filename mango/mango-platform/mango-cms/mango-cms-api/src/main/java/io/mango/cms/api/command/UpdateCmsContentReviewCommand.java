package io.mango.cms.api.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCmsContentReviewCommand {

    @NotNull(message = "内容 ID 不能为空")
    private Long id;

    @Size(max = 1024, message = "审核意见最多1024个字符")
    private String reviewComment;
}
