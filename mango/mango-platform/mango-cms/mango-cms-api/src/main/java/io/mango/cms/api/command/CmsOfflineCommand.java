package io.mango.cms.api.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CmsOfflineCommand {

    @NotNull(message = "ID 不能为空")
    private Long id;
}
