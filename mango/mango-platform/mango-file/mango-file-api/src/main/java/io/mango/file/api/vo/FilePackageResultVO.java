package io.mango.file.api.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.file.api.enums.FilePackageSizeControlMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 带大小控制的文件打包结果。
 */
@Data
@Schema(description = "带大小控制的文件打包结果")
public class FilePackageResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "最终ZIP文件记录")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable value object getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable value object setter"))
    private FileRecordVO file;

    @Schema(description = "大小控制模式")
    private FilePackageSizeControlMode sizeControlMode;

    @Schema(description = "请求的最终ZIP目标大小，单位字节；未设置时为空")
    private Long maxPackageSizeBytes;

    @Schema(description = "最终ZIP实际大小，单位字节")
    private Long actualPackageSizeBytes;

    @Schema(description = "最终ZIP是否达到目标；未设置总目标时为空")
    private Boolean packageTargetAchieved;

    @Schema(description = "所有手动条目目标是否达到；AUTO模式或没有条目目标时为空")
    private Boolean entryTargetsAchieved;

    @Schema(description = "是否至少有一个条目被实际缩小")
    private Boolean compressionApplied;

    @Schema(description = "逐条目大小控制结果")
    @Getter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Jackson and Feign require this mutable collection getter"))
    @Setter(onMethod_ = @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Jackson and Feign require this mutable collection setter"))
    private List<FilePackageEntryResultVO> entries;

    @Schema(description = "总体处理说明")
    private String message;
}
