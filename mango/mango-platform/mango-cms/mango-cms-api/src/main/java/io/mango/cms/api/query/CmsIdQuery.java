package io.mango.cms.api.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CmsIdQuery {

    @NotNull(message = "ID 不能为空")
    @Schema(description = "主键 ID")
    private Long id;
}
