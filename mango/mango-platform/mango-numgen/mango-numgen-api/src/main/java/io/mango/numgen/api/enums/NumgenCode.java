package io.mango.numgen.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 编号生成模块业务码。
 */
@Getter
@AllArgsConstructor
public enum NumgenCode implements BizCode {

    /** 请求或业务参数不正确。 */
    NUMGEN_INVALID(3901, "编号生成参数不正确"),

    /** 当前请求缺少有效租户上下文。 */
    NUMGEN_TENANT_CONTEXT_INVALID(3902, "编号生成缺少有效租户上下文"),

    /** 编号生成器不存在。 */
    NUMGEN_GENERATOR_NOT_FOUND(3903, "编号生成器不存在"),

    /** 编号生成器业务 Key 已存在。 */
    NUMGEN_GENERATOR_KEY_DUPLICATED(3904, "编号生成器业务 Key 已存在"),

    /** 编号生成器业务 Key 不允许修改。 */
    NUMGEN_GENERATOR_KEY_IMMUTABLE(3905, "编号生成器业务 Key 不允许修改"),

    /** 编号生成器关联业务域不可用。 */
    NUMGEN_DOMAIN_INVALID(3906, "编号生成器业务域不可用"),

    /** 编号规则不存在。 */
    NUMGEN_RULE_NOT_FOUND(3910, "编号规则不存在"),

    /** 编号规则当前状态不允许编辑或删除。 */
    NUMGEN_RULE_NOT_EDITABLE(3911, "编号规则当前状态不允许编辑或删除"),

    /** 编号规则当前不可发布。 */
    NUMGEN_RULE_NOT_PUBLISHABLE(3912, "编号规则当前不可发布"),

    /** 没有可发布的编号规则。 */
    NUMGEN_RULE_NO_DRAFT(3913, "没有可发布的编号规则"),

    /** 编号规则片段不存在。 */
    NUMGEN_SEGMENT_NOT_FOUND(3920, "编号规则片段不存在"),

    /** 编号规则片段配置不正确。 */
    NUMGEN_SEGMENT_INVALID(3921, "编号规则片段配置不正确"),

    /** 编号规则片段所属规则不允许修改。 */
    NUMGEN_SEGMENT_RULE_IMMUTABLE(3922, "编号规则片段所属规则不允许修改"),

    /** 编号序列分配失败。 */
    NUMGEN_SEQUENCE_ALLOCATE_FAILED(3930, "编号序列分配失败，请重试"),

    /** 编号规则不存在或尚未发布。 */
    NUMGEN_ACTIVE_RULE_NOT_FOUND(3931, "编号规则不存在或尚未发布");

    private final int code;
    private final String message;
}
