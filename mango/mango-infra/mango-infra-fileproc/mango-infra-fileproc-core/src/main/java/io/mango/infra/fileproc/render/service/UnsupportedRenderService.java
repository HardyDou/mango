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

import java.util.Set;
import java.util.List;

/**
 * 默认渲染处理实现，用于未接入具体 PDF 引擎时明确失败。
 */
public class UnsupportedRenderService implements RenderApi {

    @Override
    public boolean canRender(RenderFormat sourceFormat, RenderFormat targetFormat) {
        return false;
    }

    @Override
    public RenderResultVO render(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        throw new RenderToolException("文档渲染能力未配置");
    }

    @Override
    public List<String> extractVariables(RenderCommand command) {
        Require.notNull(command, "渲染命令不能为空");
        throw new RenderToolException("变量提取能力未配置");
    }

    @Override
    public Set<RenderFormatPairVO> supportedRenderings() {
        return Set.of();
    }

    @Override
    public PdfOperationResultVO mergePdf(MergePdfCommand command) {
        Require.notNull(command, "PDF 合并命令不能为空");
        throw new RenderToolException("PDF 合并能力未配置");
    }

    @Override
    public PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command) {
        Require.notNull(command, "PDF 水印命令不能为空");
        throw new RenderToolException("PDF 水印能力未配置");
    }

    @Override
    public PdfOperationResultVO compressPdf(CompressPdfCommand command) {
        Require.notNull(command, "PDF 压缩命令不能为空");
        throw new RenderToolException("PDF 压缩能力未配置");
    }

    @Override
    public PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command) {
        Require.notNull(command, "PDF 目标压缩命令不能为空");
        throw new RenderToolException("PDF 目标压缩能力未配置");
    }
}
