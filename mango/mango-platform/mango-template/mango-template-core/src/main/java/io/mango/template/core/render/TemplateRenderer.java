package io.mango.template.core.render;

import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;

import java.util.List;

/**
 * 模板渲染器 SPI。
 */
public interface TemplateRenderer {

    /**
     * 判断渲染器是否支持指定源格式。
     *
     * @param sourceFormat 源格式。
     * @return 支持返回 true。
     */
    boolean supports(TemplateSourceFormat sourceFormat);

    /**
     * 渲染模板。
     *
     * @param payload 渲染载荷。
     * @return 渲染结果。
     */
    TemplateRenderOutput render(TemplateRenderPayload payload);

    /**
     * 提取模板变量。
     *
     * @param payload 渲染载荷。
     * @return 变量名列表。
     */
    List<String> extractVariables(TemplateRenderPayload payload);

    /**
     * 判断渲染器是否可直接输出目标格式。
     *
     * @param outputFormat 输出格式。
     * @return 支持返回 true。
     */
    default boolean supportsOutput(TemplateOutputFormat outputFormat) {
        return false;
    }
}
