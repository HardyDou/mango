package io.mango.ai.api.enums;

import io.mango.common.result.BizCode;

/**
 * AI 模块业务错误码。
 */
public enum AiCode implements BizCode {

    /** 对话请求不能为空。 */
    CHAT_REQUEST_REQUIRED(95001, "对话请求不能为空"),

    /** 租户标识不能为空。 */
    TENANT_REQUIRED(95002, "租户标识不能为空"),

    /** 对话参数不合法。 */
    CHAT_REQUEST_INVALID(95003, "对话参数不合法"),

    /** 用户标识不能为空。 */
    USER_REQUIRED(95004, "用户标识不能为空"),

    /** 对话请求超过调用频率限制。 */
    CHAT_RATE_LIMITED(95005, "AI 对话请求过于频繁"),

    /** AI 模型当前不可用。 */
    CHAT_MODEL_UNAVAILABLE(95006, "AI 模型当前不可用"),

    /** 会话上下文读写失败。 */
    CHAT_CONTEXT_UNAVAILABLE(95007, "AI 会话上下文不可用"),

    /** 厂商接入参数不合法。 */
    PROVIDER_INVALID(95008, "AI 厂商接入参数不合法"),

    /** 厂商接入不存在。 */
    PROVIDER_NOT_FOUND(95009, "AI 厂商接入不存在"),

    /** 厂商接入编码已存在。 */
    PROVIDER_CONFLICT(95010, "AI 厂商接入编码已存在"),

    /** 厂商仍有模型时不可删除。 */
    PROVIDER_HAS_MODELS(95011, "厂商仍有模型，不能删除"),

    /** 模型参数不合法。 */
    MODEL_INVALID(95012, "AI 模型参数不合法"),

    /** 模型不存在。 */
    MODEL_NOT_FOUND(95013, "AI 模型不存在"),

    /** 同一厂商下模型标识已存在。 */
    MODEL_CONFLICT(95014, "AI 模型标识已存在"),

    /** 能力路由参数不合法。 */
    ROUTE_INVALID(95015, "AI 能力路由参数不合法"),

    /** Chat 能力没有配置默认路由。 */
    CHAT_ROUTE_REQUIRED(95016, "当前租户没有可用的 Chat 能力路由"),

    /** Chat 运行时适配器尚未实现。 */
    CHAT_ADAPTER_UNAVAILABLE(95017, "当前供应商尚未接入 Chat 运行时适配器"),

    /** 模型配置密钥不可用。 */
    MODEL_SECRET_UNAVAILABLE(95018, "AI 厂商接入密钥不可用"),

    /** AI 配置参数不合法。 */
    CONFIG_INVALID(95020, "AI 配置参数不合法"),

    /** Prompt 模板不存在。 */
    PROMPT_NOT_FOUND(95021, "Prompt 模板不存在"),

    /** Prompt 模板编码已存在。 */
    PROMPT_CONFLICT(95022, "Prompt 模板编码已存在"),

    /** Prompt 模板仍被服务引用。 */
    PROMPT_REFERENCED(95023, "Prompt 模板仍被 AI 服务引用"),

    /** Skill 不存在。 */
    SKILL_NOT_FOUND(95024, "Skill 不存在"),

    /** Skill 编码已存在。 */
    SKILL_CONFLICT(95025, "Skill 编码已存在"),

    /** Skill 仍被服务引用。 */
    SKILL_REFERENCED(95026, "Skill 仍被 AI 服务引用"),

    /** 工具不存在。 */
    TOOL_NOT_FOUND(95027, "AI 工具不存在"),

    /** 工具编码已存在。 */
    TOOL_CONFLICT(95028, "AI 工具编码已存在"),

    /** 工具仍被 Skill 引用。 */
    TOOL_REFERENCED(95029, "AI 工具仍被 Skill 引用"),

    /** AI 服务不存在。 */
    SERVICE_NOT_FOUND(95030, "AI 服务不存在"),

    /** AI 服务编码已存在。 */
    SERVICE_CONFLICT(95031, "AI 服务编码已存在"),

    /** AI 服务不存在可执行定义。 */
    SERVICE_NOT_EXECUTABLE(95032, "AI 服务当前不可执行"),

    /** AI 服务输入不符合定义。 */
    SERVICE_INPUT_INVALID(95033, "AI 服务输入不符合 Schema"),

    /** AI 服务输出不符合定义。 */
    SERVICE_OUTPUT_INVALID(95034, "AI 服务输出不符合 Schema"),

    /** AI 服务类型尚未接入当前运行时。 */
    SERVICE_TYPE_UNSUPPORTED(95035, "AI 服务类型尚未接入当前运行时"),

    /** AI 服务工具执行链尚未接入。 */
    SERVICE_TOOLS_UNSUPPORTED(95036, "AI 服务引用的工具执行链尚未接入"),

    /** AI 服务调用失败。 */
    SERVICE_INVOCATION_FAILED(95037, "AI 服务调用失败"),

    /** AI 服务审计记录失败。 */
    SERVICE_AUDIT_FAILED(95038, "AI 服务审计记录失败"),

    /** AI 对话会话不存在。 */
    CHAT_CONVERSATION_NOT_FOUND(95039, "AI 对话会话不存在");

    private final int code;
    private final String message;

    AiCode(int code, String message) {
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
