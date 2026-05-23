package io.mango.template.core.render;

import io.mango.common.exception.BizException;
import io.mango.infra.fileproc.convert.ConvertApi;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.command.RenderVariableDefinition;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import io.mango.template.api.TemplateCode;
import io.mango.template.api.command.TemplateVariableDefinition;
import io.mango.template.api.enums.TemplateOutputFormat;
import io.mango.template.api.enums.TemplateSourceFormat;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 模板渲染调度器。
 */
@RequiredArgsConstructor
public class TemplateRenderManager {

    private final RenderApi renderApi;
    private final ConvertApi convertApi;

    public TemplateRenderOutput render(TemplateRenderPayload payload) {
        RenderFormat sourceRenderFormat = renderFormat(payload.sourceFormat());
        RenderFormat targetRenderFormat = renderFormat(intermediateFormat(payload.sourceFormat()));
        RenderCommand renderCommand = renderCommand(payload, sourceRenderFormat, targetRenderFormat);
        if (!renderApi.canRender(sourceRenderFormat, targetRenderFormat)) {
            throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                    TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
        }
        RenderResultVO rendered = renderApi.render(renderCommand);
        if (rendered.format() == renderFormat(payload.outputFormat())) {
            return output(rendered);
        }
        if (TemplateOutputFormat.PDF == payload.outputFormat() || TemplateOutputFormat.OFD == payload.outputFormat()) {
            TemplateOutputFormat intermediate = intermediateFormat(payload.sourceFormat());
            ConvertResultVO converted = convert(rendered, intermediate, payload.outputFormat());
            return output(converted);
        }
        throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
    }

    public List<String> extractVariables(TemplateRenderPayload payload) {
        RenderFormat sourceRenderFormat = renderFormat(payload.sourceFormat());
        RenderFormat targetRenderFormat = renderFormat(intermediateFormat(payload.sourceFormat()));
        return renderApi.extractVariables(renderCommand(payload, sourceRenderFormat, targetRenderFormat));
    }

    private ConvertResultVO convert(RenderResultVO rendered,
                                          TemplateOutputFormat sourceFormat,
                                          TemplateOutputFormat targetFormat) {
        byte[] sourceBytes = rendered.content();
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new BizException(TemplateCode.TEMPLATE_RENDER_FAILED.getCode(), "模板渲染结果为空，无法转换文档");
        }
        ConvertFormat sourceConvertFormat = convertFormat(sourceFormat);
        ConvertFormat targetConvertFormat = convertFormat(targetFormat);
        if (!convertApi.canConvert(sourceConvertFormat, targetConvertFormat)) {
            throw new BizException(TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getCode(),
                    TemplateCode.TEMPLATE_FORMAT_UNSUPPORTED.getMessage());
        }
        return convertApi.convert(ConvertCommand.builder()
                .sourceFormat(sourceConvertFormat)
                .targetFormat(targetConvertFormat)
                .inputStream(new ByteArrayInputStream(sourceBytes))
                .fileName(rendered.fileName())
                .build());
    }

    private TemplateRenderOutput output(RenderResultVO result) {
        if (result.format() == RenderFormat.TEXT || result.format() == RenderFormat.HTML) {
            return new TemplateRenderOutput(new String(result.content(), StandardCharsets.UTF_8),
                    null, result.fileName(), result.contentType());
        }
        return new TemplateRenderOutput(null, result.content(), result.fileName(), result.contentType());
    }

    private TemplateRenderOutput output(ConvertResultVO result) {
        if (result.format() == ConvertFormat.TEXT || result.format() == ConvertFormat.HTML) {
            return new TemplateRenderOutput(new String(result.content(), StandardCharsets.UTF_8),
                    null, result.fileName(), result.contentType());
        }
        return new TemplateRenderOutput(null, result.content(), result.fileName(), result.contentType());
    }

    private RenderCommand renderCommand(TemplateRenderPayload payload,
                                        RenderFormat sourceFormat,
                                        RenderFormat targetFormat) {
        byte[] sourceBytes = payload.sourceBytes();
        if ((sourceBytes == null || sourceBytes.length == 0) && payload.content() != null) {
            sourceBytes = payload.content().getBytes(StandardCharsets.UTF_8);
        }
        if (sourceBytes == null) {
            sourceBytes = new byte[0];
        }
        return RenderCommand.builder()
                .sourceFormat(sourceFormat)
                .targetFormat(targetFormat)
                .inputStream(new ByteArrayInputStream(sourceBytes))
                .fileName(payload.sourceFileName())
                .variables(payload.variables())
                .variableDefinitions(renderVariableDefinitions(payload.variableDefinitions()))
                .build();
    }

    private TemplateOutputFormat intermediateFormat(TemplateSourceFormat sourceFormat) {
        return switch (sourceFormat) {
            case TEXT -> TemplateOutputFormat.TEXT;
            case HTML -> TemplateOutputFormat.HTML;
            case DOCX -> TemplateOutputFormat.DOCX;
            case XLSX -> TemplateOutputFormat.XLSX;
        };
    }

    private RenderFormat renderFormat(TemplateSourceFormat format) {
        return switch (format) {
            case TEXT -> RenderFormat.TEXT;
            case HTML -> RenderFormat.HTML;
            case DOCX -> RenderFormat.DOCX;
            case XLSX -> RenderFormat.XLSX;
        };
    }

    private RenderFormat renderFormat(TemplateOutputFormat format) {
        return switch (format) {
            case TEXT -> RenderFormat.TEXT;
            case HTML -> RenderFormat.HTML;
            case DOCX -> RenderFormat.DOCX;
            case XLSX -> RenderFormat.XLSX;
            case PDF -> RenderFormat.PDF;
            case OFD -> RenderFormat.OFD;
        };
    }

    private ConvertFormat convertFormat(TemplateOutputFormat format) {
        return switch (format) {
            case TEXT -> ConvertFormat.TEXT;
            case HTML -> ConvertFormat.HTML;
            case DOCX -> ConvertFormat.DOCX;
            case XLSX -> ConvertFormat.XLSX;
            case PDF -> ConvertFormat.PDF;
            case OFD -> ConvertFormat.OFD;
        };
    }

    private List<RenderVariableDefinition> renderVariableDefinitions(List<TemplateVariableDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return List.of();
        }
        return definitions.stream()
                .map(this::renderVariableDefinition)
                .toList();
    }

    private RenderVariableDefinition renderVariableDefinition(TemplateVariableDefinition definition) {
        return new RenderVariableDefinition(
                definition.getName(),
                definition.getType(),
                renderVariableDefinitions(definition.getChildren()));
    }
}
