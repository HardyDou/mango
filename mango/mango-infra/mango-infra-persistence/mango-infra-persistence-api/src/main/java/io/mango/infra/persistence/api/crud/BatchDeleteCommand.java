package io.mango.infra.persistence.api.crud;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 标准批量删除命令。
 */
@Data
@Schema(description = "标准批量删除命令")
public class BatchDeleteCommand {

    /**
     * 主键列表。
     */
    @Schema(description = "待删除记录主键ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids = new ArrayList<>();
}
