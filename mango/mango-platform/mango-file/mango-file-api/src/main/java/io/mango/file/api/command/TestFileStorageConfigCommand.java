package io.mango.file.api.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.io.Serializable;

/**
 * 测试文件存储配置命令。
 */
@Data
@Schema(description = "测试文件存储配置命令")
public class TestFileStorageConfigCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "配置ID。测试已保存配置时填写")
    private Long id;

    @Valid
    @Schema(description = "临时配置。测试未保存配置或编辑中的配置时填写")
    private SaveFileStorageConfigCommand config;
}
