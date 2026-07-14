package io.mango.payment.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "导入线下银行流水命令")
public class ImportOfflineBankStatementCommand {

    @NotNull(message = "银行流水文件内容不能为空")
    @Size(min = 1, message = "银行流水文件内容不能为空")
    @Schema(description = "银行流水文件内容")
    private byte[] fileContent;

    @NotBlank(message = "银行流水原始文件名不能为空")
    @Size(max = 255, message = "银行流水原始文件名不能超过 255 个字符")
    @Schema(description = "银行流水原始文件名")
    private String originalFilename;

    @Positive(message = "银行流水文件 ID 必须大于 0")
    @Schema(description = "银行流水文件 ID")
    private Long statementFileId;
}
