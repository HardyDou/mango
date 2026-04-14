package io.mango.common.exception;

import io.mango.common.result.CommonCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author Mango
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(CommonCode.BAD_REQUEST.getCode(), message);
    }
}

