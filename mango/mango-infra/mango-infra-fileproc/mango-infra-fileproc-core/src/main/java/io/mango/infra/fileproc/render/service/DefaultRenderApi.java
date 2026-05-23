package io.mango.infra.fileproc.render.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;
import io.mango.infra.fileproc.render.vo.RenderFormatPairVO;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认文档渲染门面。
 */
public class DefaultRenderApi implements RenderApi {

    private final RenderRegistry registry;

    private final RenderApi pdfRenderApi;

    public DefaultRenderApi(RenderRegistry registry, RenderApi pdfRenderApi) {
        this.registry = registry;
        this.pdfRenderApi = pdfRenderApi == null ? new UnsupportedRenderService() : pdfRenderApi;
    }

    @Override
    public boolean canRender(RenderFormat sourceFormat, RenderFormat targetFormat) {
        if (sourceFormat == null || targetFormat == null) {
            return false;
        }
        if (sourceFormat == targetFormat) {
            return true;
        }
        return registry.findProvider(sourceFormat, targetFormat).isPresent();
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        if (command.sourceFormat() == command.targetFormat()) {
            RenderResultVO result = registry.providers().stream()
                    .filter(provider -> !(provider instanceof SameFormatRenderProvider))
                    .filter(provider -> provider.supports(command.sourceFormat(), command.targetFormat()))
                    .findFirst()
                    .orElseGet(SameFormatRenderProvider::new)
                    .render(command);
            return complete(command, result);
        }
        IRenderProvider provider = registry.findProvider(command.sourceFormat(), command.targetFormat())
                .orElseThrow(() -> new RenderToolException(
                        "不支持的文档渲染：" + command.sourceFormat() + " -> " + command.targetFormat()));
        return complete(command, provider.render(command));
    }

    @Override
    public List<String> extractVariables(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        IRenderProvider provider = registry.providers().stream()
                .filter(item -> !(item instanceof SameFormatRenderProvider))
                .filter(item -> item.supports(command.sourceFormat(), command.targetFormat()))
                .findFirst()
                .orElseThrow(() -> new RenderToolException(
                        "不支持的变量提取：" + command.sourceFormat() + " -> " + command.targetFormat()));
        return provider.extractVariables(command);
    }

    @Override
    public Set<RenderFormatPairVO> supportedRenderings() {
        Set<RenderFormatPairVO> pairs = new LinkedHashSet<>();
        for (RenderFormat sourceFormat : RenderFormat.values()) {
            for (RenderFormat targetFormat : RenderFormat.values()) {
                if (sourceFormat != targetFormat && canRender(sourceFormat, targetFormat)) {
                    pairs.add(new RenderFormatPairVO(sourceFormat, targetFormat));
                }
            }
        }
        return pairs;
    }

    @Override
    public PdfOperationResultVO mergePdf(MergePdfCommand command) {
        return pdfRenderApi.mergePdf(command);
    }

    @Override
    public PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command) {
        return pdfRenderApi.addPdfWatermark(command);
    }

    @Override
    public PdfOperationResultVO compressPdf(CompressPdfCommand command) {
        return pdfRenderApi.compressPdf(command);
    }

    @Override
    public PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command) {
        return pdfRenderApi.compressPdfToTarget(command);
    }

    private RenderResultVO complete(RenderCommand command, RenderResultVO result) {
        if (!command.hasTargetPath() || result.hasOutputPath()) {
            return result;
        }
        try {
            createParent(command.targetPath());
            Files.write(command.targetPath(), result.content());
            return RenderResultVO.builder()
                    .format(result.format())
                    .fileName(result.fileName())
                    .contentType(result.contentType())
                    .outputPath(command.targetPath())
                    .build();
        } catch (IOException ex) {
            throw new RenderToolException("写入渲染目标文件失败", ex);
        }
    }

    private void createParent(java.nio.file.Path path) throws IOException {
        if (path != null && path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
    }
}
