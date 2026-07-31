package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ZIP 条目大小控制结果。
 */
@Data
@Schema(description = "ZIP条目大小控制结果")
public class FilePackageEntryResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "源文件ID")
    private Long fileId;

    @Schema(description = "ZIP内路径")
    private String path;

    @Schema(description = "源文件实际大小，单位字节")
    private Long originalSizeBytes;

    @Schema(description = "写入ZIP前的实际大小，单位字节")
    private Long outputSizeBytes;

    @Schema(description = "当前条目的压缩目标大小，单位字节；未设置时为空")
    private Long targetSizeBytes;

    @Schema(description = "文件格式是否受压缩组件支持")
    private Boolean compressionSupported;

    @Schema(description = "是否实际缩小了文件")
    private Boolean compressionApplied;

    @Schema(description = "是否达到当前条目目标；没有条目目标时为空")
    private Boolean targetAchieved;

    @Schema(description = "当前条目处理说明")
    private String message;
}
