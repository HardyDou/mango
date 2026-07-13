package io.mango.infra.persistence.api.crud;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

/** 标准删除命令。 */
@Data
@Schema(description = "标准删除命令")
public class DeleteCommand {

    /** 主键。 */
    @Schema(description = "待删除记录主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "待删除记录主键ID不能为空")
    private Long id;
}
