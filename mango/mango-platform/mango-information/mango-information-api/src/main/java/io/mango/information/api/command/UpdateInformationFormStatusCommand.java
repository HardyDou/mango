package io.mango.information.api.command;

import io.mango.information.api.enums.InformationFormStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/** 表单启停命令。 */
@Data
@Schema(description = "表单启停命令")
public class UpdateInformationFormStatusCommand implements Serializable {
    @NotNull(message = "表单ID不能为空")
    private Long id;

    @NotNull(message = "表单状态不能为空")
    @Schema(description = "表单状态：DRAFT、PUBLISHED、DISABLED")
    private InformationFormStatus status;
}
