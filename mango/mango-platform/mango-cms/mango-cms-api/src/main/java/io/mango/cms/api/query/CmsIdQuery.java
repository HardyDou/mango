package io.mango.cms.api.query;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CmsIdQuery {

    @NotNull(message = "ID 不能为空")
    private Long id;
}
