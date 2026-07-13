package io.mango.infra.persistence.api.crud;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

/** 标准批量删除命令。 */
@Schema(description = "标准批量删除命令")
public class BatchDeleteCommand {

    /** 主键列表。 */
    @Schema(description = "待删除记录主键ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "待删除记录主键ID列表不能为空")
    private List<@NotNull @Positive Long> ids = new ArrayList<>();

    public List<Long> getIds() {
        return new ArrayList<>(ids);
    }

    public void setIds(List<Long> ids) {
        if (ids == null) {
            this.ids = new ArrayList<>();
            return;
        }
        this.ids = new ArrayList<>(ids);
    }
}
