package io.mango.infra.fileproc.render.service;

/**
 * 渲染处理执行异常。
 */
public class RenderToolException extends RuntimeException {

    public RenderToolException(String message) {
        super(message);
    }

    public RenderToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
