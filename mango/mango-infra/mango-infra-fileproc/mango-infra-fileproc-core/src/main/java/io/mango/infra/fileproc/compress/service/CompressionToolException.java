package io.mango.infra.fileproc.compress.service;

/**
 * 文件压缩异常。
 */
public class CompressionToolException extends RuntimeException {

    public CompressionToolException(String message) {
        super(message);
    }

    public CompressionToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
