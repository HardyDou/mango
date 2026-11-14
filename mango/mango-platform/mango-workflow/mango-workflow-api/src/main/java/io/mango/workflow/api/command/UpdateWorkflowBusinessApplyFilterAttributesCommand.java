package io.mango.workflow.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新业务申请可筛属性命令。
 *
 * <p>整体覆盖语义：传入的键值替换该申请已登记的全部可筛属性，传空集合表示清空。
 * 属性名和属性值与创建申请时的规则一致。
 */
@Data
@Schema(description = "更新业务申请可筛属性命令，整体覆盖")
public class UpdateWorkflowBusinessApplyFilterAttributesCommand {

    @Schema(description = "可筛属性；整体覆盖，空集合表示清空")
    @NotNull(message = "可筛属性不能为空，清空请传空集合")
    @Size(max = 20, message = "可筛属性数量不能超过20")
    private Map<String, List<String>> filterAttributes;
}
