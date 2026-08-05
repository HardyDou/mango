package {{basePackage}}.{{modulePackage}}.api.enums;

import io.mango.common.result.BizCode;

/**
 * {{aggregateName}}业务错误码。
 */
public enum {{aggregatePascal}}Code implements BizCode {

    NOT_FOUND(404, "{{aggregateName}}不存在"),
    VALIDATION_ERROR(400, "{{aggregateName}}参数校验失败");

    private final int code;
    private final String message;

    {{aggregatePascal}}Code(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
