package io.mango.file.api.command;

import io.mango.file.api.enums.FilePackageSizeControlMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带大小控制的文件打包命令。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "带大小控制的文件打包命令")
public class FilePackageSizeControlCommand extends FilePackageCommand {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "大小控制模式不能为空")
    @Schema(description = "大小控制模式：AUTO、MANUAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private FilePackageSizeControlMode sizeControlMode;

    @Positive(message = "ZIP目标大小必须大于0")
    @Schema(description = "最终 ZIP 目标大小，单位字节；AUTO 模式必填，MANUAL 模式只用于结果判断")
    private Long maxPackageSizeBytes;

    /**
     * 校验自动模式必须声明最终 ZIP 目标大小。
     *
     * @return 参数组合有效时返回 true
     */
    @AssertTrue(message = "AUTO模式必须设置ZIP目标大小")
    public boolean isAutoTargetValid() {
        return sizeControlMode != FilePackageSizeControlMode.AUTO || maxPackageSizeBytes != null;
    }
}
