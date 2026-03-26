package io.mango.common.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 业务异常
 * 抛出时只打印 warn 日志，不走 err 处理
 *
 * @author Mango
 */
@Slf4j
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
        log.warn("业务异常: code={}, message={}", code, message);
    }

    public BizException(String message) {
        this(400, message);
    }
}
