package io.mango.file.preview.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件预览入口信息。
 */
@Data
@Schema(description = "文件预览入口信息")
public class FilePreviewLinkVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "预览页面地址")
    private String previewUrl;

    @Schema(description = "预览入口临时令牌")
    private String previewToken;

    @Schema(description = "源文件临时访问地址有效期，单位秒")
    private Long expireSeconds;
}
