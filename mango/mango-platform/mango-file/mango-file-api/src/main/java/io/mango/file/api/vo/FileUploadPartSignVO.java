package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分片上传签名结果。
 */
@Data
@Schema(description = "分片上传签名结果")
public class FileUploadPartSignVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分片序号，从 1 开始")
    private Integer partNumber;

    @Schema(description = "上传 URL")
    private String uploadUrl;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "有效期，单位秒")
    private Long expireSeconds;
}
