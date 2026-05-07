package io.mango.system.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "系统路由排序修改命令")
public class UpdateRouteSortCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "按目标顺序排列的路由ID列表")
    @NotEmpty(message = "路由ID列表不能为空")
    private List<Long> ids;
}
