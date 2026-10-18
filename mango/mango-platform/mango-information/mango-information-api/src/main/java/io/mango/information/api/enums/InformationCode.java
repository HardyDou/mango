package io.mango.information.api.enums;

import io.mango.common.result.BizCode;

/** 信息采集错误码。 */
public enum InformationCode implements BizCode {
    FORM_NOT_FOUND("信息采集表单不存在"),
    FORM_CODE_DUPLICATED("信息采集表单编码已存在"),
    SUBMISSION_NOT_FOUND("信息采集提交记录不存在"),
    FORM_NOT_PUBLISHED("信息采集表单未发布"),
    INVALID_FORM_DEFINITION("信息采集字段定义无效"),
    INVALID_SUBMISSION("信息采集提交内容无效"),
    SUBMISSION_CLOSED("已关闭的提交记录不可继续处理"),
    SERVER_SECRET_INVALID("服务端密钥无效");

    private final String message;

    InformationCode(String message) { this.message = message; }

    @Override
    public int getCode() { return 105500 + ordinal(); }

    @Override
    public String getMessage() { return message; }
}
