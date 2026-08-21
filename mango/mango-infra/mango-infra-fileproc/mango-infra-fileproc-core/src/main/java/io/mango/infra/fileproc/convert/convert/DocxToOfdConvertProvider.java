package io.mango.infra.fileproc.convert.convert;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

/**
 * 复用现有 Word 转 PDF 能力，再通过 OFDRW 将 PDF 转为 OFD。
 *
 * <p>Word 文档的表格、图片和印章等可见内容先由 Word 转 PDF 引擎排版，
 * 最终以 OFDRW 的页面绘制结果进入 OFD。交互式表单和数字签名不属于此转换链的语义范围。</p>
 */
public class DocxToOfdConvertProvider implements IConvertProvider {

    private final IConvertProvider docxToPdfProvider;

    private final PdfToOfdConvertProvider pdfToOfdProvider;

    public DocxToOfdConvertProvider(IConvertProvider docxToPdfProvider) {
        this(docxToPdfProvider, new PdfToOfdConvertProvider());
    }

    public DocxToOfdConvertProvider(IConvertProvider docxToPdfProvider,
                                    PdfToOfdConvertProvider pdfToOfdProvider) {
        Require.notNull(docxToPdfProvider, "Word 转 PDF 转换器不能为空");
        Require.notNull(pdfToOfdProvider, "PDF 转 OFD 转换器不能为空");
        this.docxToPdfProvider = docxToPdfProvider;
        this.pdfToOfdProvider = pdfToOfdProvider;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.DOCX && targetFormat == ConvertFormat.OFD;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        Path workDir = ConvertTempFiles.createWorkDir();
        try {
            Path inputFile = ConvertTempFiles.writeInput(workDir, command);
            ConvertCommand pdfCommand = ConvertCommand.builder()
                    .sourceFormat(ConvertFormat.DOCX)
                    .targetFormat(ConvertFormat.PDF)
                    .sourcePath(inputFile)
                    .fileName(command.fileName())
                    .options(command.options())
                    .build();
            ConvertResultVO pdfResult = docxToPdfProvider.convert(pdfCommand);
            ConvertCommand ofdCommand = ConvertCommand.builder()
                    .sourceFormat(ConvertFormat.PDF)
                    .targetFormat(ConvertFormat.OFD)
                    .inputStream(new ByteArrayInputStream(pdfResult.content()))
                    .targetPath(command.targetPath())
                    .fileName(command.fileName())
                    .options(command.options())
                    .build();
            return pdfToOfdProvider.convert(ofdCommand);
        } catch (Exception ex) {
            throw new ConvertToolException("DOCX 转 OFD 失败", ex);
        } finally {
            ConvertTempFiles.deleteQuietly(workDir);
        }
    }
}
