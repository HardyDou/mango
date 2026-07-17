package io.mango.home.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "批量删除用户首页命令")
public class BatchDeleteHomePagesCommand implements Serializable {

    @NotEmpty(message = "首页ID不能为空")
    @Schema(description = "待删除首页ID列表")
    private List<@NotNull(message = "首页ID不能为空") Long> ids;
}
