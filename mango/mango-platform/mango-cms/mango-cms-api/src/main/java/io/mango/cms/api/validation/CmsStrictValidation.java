package io.mango.cms.api.validation;

/**
 * CMS 严格校验组。
 *
 * <p>用于需要调用方显式提供可选发布窗口或展示开关的场景。默认 HTTP 契约不启用该组，
 * 以保留历史接口对空值采用默认值或“不限制时间”的行为。</p>
 */
public interface CmsStrictValidation {
}
