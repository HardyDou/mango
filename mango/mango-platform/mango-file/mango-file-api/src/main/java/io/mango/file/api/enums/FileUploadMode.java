package io.mango.file.api.enums;

/**
 * 文件上传模式。
 */
public enum FileUploadMode {

    /** 后端普通上传。 */
    SERVER,

    /** 后端接收分片并合并。 */
    SERVER_CHUNK,

    /** 对象存储原生分片上传。 */
    S3_MULTIPART;
}
