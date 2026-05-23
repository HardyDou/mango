package io.mango.infra.fileproc.render;

import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.RenderFormatPairVO;
import io.mango.infra.fileproc.render.vo.RenderResultVO;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;

import java.util.List;
import java.util.Set;

/**
 * 渲染处理能力契约。
 * <p>
 * 本接口只提供本地文档渲染处理能力，不处理文件存储、权限、租户或 fileId 语义。
 */
public interface RenderApi {

    /**
     * 判断当前运行时是否支持指定格式渲染。
     *
     * @param sourceFormat 源格式，不能为空；为空时返回 false。
     * @param targetFormat 目标格式，不能为空；为空时返回 false。
     * @return 支持返回 true，否则返回 false。
     */
    boolean canRender(RenderFormat sourceFormat, RenderFormat targetFormat);

    /**
     * 执行一次本地文档渲染。
     *
     * @param command 渲染命令，必须包含源格式、目标格式和输入流。
     * @return 渲染后的文件内容、格式和内容类型。
     */
    RenderResultVO render(RenderCommand command);

    /**
     * 提取渲染输入中的变量名。
     *
     * @param command 渲染命令，必须包含源格式、目标格式和输入流。
     * @return 变量名列表。
     */
    List<String> extractVariables(RenderCommand command);

    /**
     * 获取当前运行时注册的跨格式渲染能力。
     *
     * @return 支持的源格式与目标格式组合，不包含同格式直通复制能力。
     */
    Set<RenderFormatPairVO> supportedRenderings();

    /**
     * 合并多个 PDF 输入。
     *
     * @param command 合并命令，必须包含输入文件列表。
     * @return 合并后的 PDF 内容。
     */
    PdfOperationResultVO mergePdf(MergePdfCommand command);

    /**
     * 为 PDF 添加水印。
     *
     * @param command 水印命令，必须包含 PDF 输入流。
     * @return 添加水印后的 PDF 内容。
     */
    PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command);

    /**
     * 压缩 PDF。
     *
     * @param command 压缩命令，必须包含 PDF 输入流。
     * @return 压缩后的 PDF 内容。
     */
    PdfOperationResultVO compressPdf(CompressPdfCommand command);

    /**
     * 按目标大小压缩 PDF。
     * <p>
     * 该能力会在质量与分辨率下限内多次尝试压缩。若无法达到目标大小，
     * 非严格模式返回当前最小结果，严格模式抛出异常。
     *
     * @param command 目标压缩命令，必须包含 PDF 输入流和目标大小。
     * @return 压缩结果及最终压缩参数。
     */
    PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command);
}
