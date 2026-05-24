package io.mango.file.preview.api;

import io.mango.common.result.BizCode;

/**
 * 文件预览业务码。
 */
public enum FilePreviewCode implements BizCode {

    /** 文件 ID 不能为空。 */
    FILE_ID_EMPTY(180001, "文件ID不能为空"),

    /** 预览令牌无效或已过期。 */
    PREVIEW_TOKEN_INVALID(180002, "预览令牌无效或已过期"),

    /** 文件不存在或不可预览。 */
    FILE_NOT_FOUND(180003, "文件不存在或不可预览");

    private final int code;

    private final String message;

    FilePreviewCode(int code, String message) {
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
