package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import io.mango.infra.tools.doc.DocumentConvertRequest;
import io.mango.infra.tools.doc.DocumentConvertResult;
import io.mango.infra.tools.doc.DocumentFormat;
import io.mango.infra.tools.doc.DocumentToolException;
import io.mango.infra.tools.doc.DocumentToolService;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 模板渲染调度器。
 */
@RequiredArgsConstructor
public class TemplateRenderManager {

    private final List<TemplateRenderer> renderers;
    private final DocumentToolService documentToolService;

    public TemplateRenderOutput render(TemplateRenderPayload payload) {
        TemplateRenderer renderer = renderer(payload.sourceFormat());
        if (renderer.supportsOutput(payload.outputFormat())) {
            return renderer.render(payload);
        }
        if (TemplateOutputFormat.PDF == payload.outputFormat() || TemplateOutputFormat.OFD == payload.outputFormat()) {
            TemplateOutputFormat intermediate = intermediateFormat(payload.sourceFormat());
            TemplateRenderOutput rendered = renderer.render(new TemplateRenderPayload(
                    payload.sourceFormat(),
                    intermediate,
                    payload.content(),
                    payload.sourceBytes(),
                    payload.sourceFileName(),
                    payload.variables(),
                    payload.variableDefinitions()));
            DocumentConvertResult converted = convert(rendered, intermediate, payload.outputFormat());
            return new TemplateRenderOutput(null, converted.content(), converted.fileName(), converted.contentType());
        }
        throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
    }

    public List<String> extractVariables(TemplateRenderPayload payload) {
        return renderer(payload.sourceFormat()).extractVariables(payload);
    }

    private TemplateRenderer renderer(TemplateSourceFormat sourceFormat) {
        return renderers.stream()
                .filter(item -> item.supports(sourceFormat))
                .findFirst()
                .orElseThrow(() -> new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                        TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage()));
    }

    private DocumentConvertResult convert(TemplateRenderOutput rendered,
                                          TemplateOutputFormat sourceFormat,
                                          TemplateOutputFormat targetFormat) {
        byte[] source = rendered.fileBytes();
        if (source == null && rendered.content() != null) {
            source = rendered.content().getBytes(StandardCharsets.UTF_8);
        }
        if (source == null) {
            throw new BizException(TemplateCode.TEMPLATE_RENDER_FAILED.getCode(), "模板渲染结果为空，无法转换文档");
        }
        try {
            return documentToolService.convert(DocumentConvertRequest.builder()
                    .sourceFormat(documentFormat(sourceFormat))
                    .targetFormat(documentFormat(targetFormat))
                    .inputStream(new ByteArrayInputStream(source))
                    .fileName(rendered.fileName())
                    .build());
        } catch (DocumentToolException ex) {
            throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(), ex.getMessage());
        }
    }

    private TemplateOutputFormat intermediateFormat(TemplateSourceFormat sourceFormat) {
        return switch (sourceFormat) {
            case TEXT -> TemplateOutputFormat.TEXT;
            case HTML -> TemplateOutputFormat.HTML;
            case DOCX -> TemplateOutputFormat.DOCX;
            case XLSX -> TemplateOutputFormat.XLSX;
        };
    }

    private DocumentFormat documentFormat(TemplateOutputFormat format) {
        return switch (format) {
            case TEXT -> DocumentFormat.TEXT;
            case HTML -> DocumentFormat.HTML;
            case DOCX -> DocumentFormat.DOCX;
            case XLSX -> DocumentFormat.XLSX;
            case PDF -> DocumentFormat.PDF;
            case OFD -> DocumentFormat.OFD;
        };
    }
}
