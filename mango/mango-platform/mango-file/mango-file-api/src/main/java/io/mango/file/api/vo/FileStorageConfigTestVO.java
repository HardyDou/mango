package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件存储配置测试结果。
 */
@Data
@Schema(description = "文件存储配置测试结果")
public class FileStorageConfigTestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否测试通过")
    private Boolean success;

    @Schema(description = "测试结果说明")
    private String message;
}
