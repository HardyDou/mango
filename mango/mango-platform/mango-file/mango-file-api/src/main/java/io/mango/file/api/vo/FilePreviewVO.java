package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件预览元数据。
 */
@Data
@Schema(description = "文件预览元数据")
public class FilePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "是否可在线预览")
    private Boolean previewable;

    @Schema(description = "预览地址")
    private String previewUrl;

    @Schema(description = "下载地址")
    private String downloadUrl;
}
