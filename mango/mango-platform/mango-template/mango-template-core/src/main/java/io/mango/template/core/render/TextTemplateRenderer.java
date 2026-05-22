package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文本模板渲染器。
 */
public class TextTemplateRenderer implements TemplateRenderer {

    private final FreemarkerTemplateEngine freemarkerEngine;

    public TextTemplateRenderer(FreemarkerTemplateEngine freemarkerEngine) {
        this.freemarkerEngine = freemarkerEngine;
    }

    @Override
    public boolean supports(TemplateSourceFormat sourceFormat) {
        return TemplateSourceFormat.TEXT == sourceFormat;
    }

    @Override
    public boolean supportsOutput(TemplateOutputFormat outputFormat) {
        return TemplateOutputFormat.TEXT == outputFormat;
    }

    @Override
    public TemplateRenderOutput render(TemplateRenderPayload payload) {
        if (TemplateOutputFormat.TEXT != payload.outputFormat()) {
            throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                    TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
        }
        return new TemplateRenderOutput(freemarkerEngine.render(payload.content(), payload.variables()), null, null,
                "text/plain;charset=UTF-8");
    }

    @Override
    public List<String> extractVariables(TemplateRenderPayload payload) {
        if (payload.content() != null) {
            return freemarkerEngine.extract(payload.content());
        }
        if (payload.sourceBytes() != null) {
            return freemarkerEngine.extract(new String(payload.sourceBytes(), StandardCharsets.UTF_8));
        }
        return List.of();
    }
}
