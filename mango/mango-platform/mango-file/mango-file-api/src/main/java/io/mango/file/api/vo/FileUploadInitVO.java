package io.mango.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件上传初始化结果。
 */
@Data
@Schema(description = "文件上传初始化结果")
public class FileUploadInitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否命中秒传")
    private Boolean instant;

    @Schema(description = "秒传命中时返回的文件记录")
    private FileRecordVO fileRecord;

    @Schema(description = "上传会话ID")
    private Long sessionId;

    @Schema(description = "上传模式：SERVER_CHUNK、S3_MULTIPART")
    private String uploadMode;

    @Schema(description = "对象存储原生上传ID")
    private String storageUploadId;

    @Schema(description = "分片大小，单位字节")
    private Long chunkSize;

    @Schema(description = "总分片数")
    private Integer totalParts;

    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;
}
