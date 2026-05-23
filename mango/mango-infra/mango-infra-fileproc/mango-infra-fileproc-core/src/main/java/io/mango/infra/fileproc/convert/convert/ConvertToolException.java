package io.mango.infra.fileproc.convert.convert;

/**
 * 格式转换执行异常。
 */
public class ConvertToolException extends RuntimeException {

    public ConvertToolException(String message) {
        super(message);
    }

    public ConvertToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
