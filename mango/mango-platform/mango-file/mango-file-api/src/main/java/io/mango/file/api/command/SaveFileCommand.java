package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 内部服务保存文件命令。
 */
@Data
@Schema(description = "内部服务保存文件命令")
public class SaveFileCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件输入流")
    private transient InputStream inputStream;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小，单位字节")
    private Long fileSize;

    @Schema(description = "内容类型")
    private String contentType;

    @Schema(description = "文件用途，例如 avatar、attachment、contract")
    private String purpose;

    @Schema(description = "访问级别：PRIVATE、PUBLIC_READ、INTERNAL。默认 PRIVATE")
    private String accessLevel;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "业务自定义参数 JSON")
    private String bizMeta;

    @Schema(description = "逻辑目录ID。根目录为0")
    private Long directoryId;
}
