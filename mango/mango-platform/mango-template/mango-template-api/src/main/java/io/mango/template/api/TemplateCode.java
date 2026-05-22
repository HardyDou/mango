package io.mango.template.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模板模块业务码。
 */
@Getter
@AllArgsConstructor
public enum TemplateCode implements BizCode {

    /** 模板不存在。 */
    TEMPLATE_NOT_FOUND(3604, "模板不存在"),

    /** 模板编码重复。 */
    TEMPLATE_CODE_DUPLICATED(3605, "模板编码已存在"),

    /** 业务Key重复。 */
    TEMPLATE_BUSINESS_KEY_DUPLICATED(3613, "业务KEY已绑定其他模板"),

    /** 模板分类不存在。 */
    TEMPLATE_CATEGORY_NOT_FOUND(3603, "模板分类不存在"),

    /** 模板分类编码重复。 */
    TEMPLATE_CATEGORY_CODE_DUPLICATED(3602, "模板分类编码已存在"),

    /** 模板状态不可用。 */
    TEMPLATE_DISABLED(3606, "模板已停用"),

    /** 模板版本不存在。 */
    TEMPLATE_VERSION_NOT_FOUND(3607, "模板版本不存在"),

    /** 模板变量不完整。 */
    TEMPLATE_VARIABLE_MISSING(3608, "模板变量不完整"),

    /** 模板格式不支持。 */
    TEMPLATE_FORMAT_UNSUPPORTED(3609, "模板格式不支持"),

    /** 模板渲染失败。 */
    TEMPLATE_RENDER_FAILED(3610, "模板渲染失败"),

    /** 模板文件不存在。 */
    TEMPLATE_FILE_NOT_FOUND(3611, "模板文件不存在"),

    /** 异步任务不存在。 */
    TEMPLATE_RENDER_RECORD_NOT_FOUND(3612, "模板渲染记录不存在");

    private final int code;
    private final String message;
}
