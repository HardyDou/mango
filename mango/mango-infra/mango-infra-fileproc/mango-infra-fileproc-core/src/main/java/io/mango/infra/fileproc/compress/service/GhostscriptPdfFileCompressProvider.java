package io.mango.infra.fileproc.compress.service;

import io.mango.common.result.Require;
import io.mango.infra.fileproc.compress.command.CompressFileCommand;
import io.mango.infra.fileproc.compress.enums.FileCompression;
import io.mango.infra.fileproc.compress.vo.CompressFileResultVO;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Ghostscript 的 PDF 压缩实现。
 * <p>
 * 输入以流方式写入临时文件，输出写入临时文件后再读取，避免把整个源文件常驻内存。
 * Ghostscript 只重写 PDF 结构并降采样图像，纯文字页面保持矢量文字，不会被栅格化。
 */
public class GhostscriptPdfFileCompressProvider implements IFileCompressProvider {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String MISSING_EXECUTABLE_MESSAGE =
            "无法执行 Ghostscript，请确认已安装 Ghostscript 并配置 mango.fileproc.compress.ghostscript-path";

    private final String ghostscriptPath;
    private final Duration timeout;

    public GhostscriptPdfFileCompressProvider(String ghostscriptPath, Duration timeout) {
        this.ghostscriptPath = StringUtils.hasText(ghostscriptPath) ? ghostscriptPath.trim() : "gs";
        this.timeout = timeout == null || timeout.isZero() || timeout.isNegative() ? Duration.ofMinutes(15) : timeout;
    }

    @Override
    public boolean supports(String fileName, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals(PDF_CONTENT_TYPE) || "pdf".equals(FileCompressionNames.extension(fileName));
    }

    @Override
    public CompressFileResultVO compress(CompressFileCommand command) {
        Require.notNull(command, "PDF 压缩命令不能为空");
        Path source = null;
        Path target = null;
        Path log = null;
        try {
            source = Files.createTempFile("mango-fileproc-compress-in-", ".pdf");
            target = Files.createTempFile("mango-fileproc-compress-out-", ".pdf");
            log = Files.createTempFile("mango-fileproc-compress-log-", ".txt");
            try (InputStream inputStream = command.inputStream()) {
                Files.copy(inputStream, source, StandardCopyOption.REPLACE_EXISTING);
            }
            long originalSize = Files.size(source);
            if (originalSize == 0) {
                return result(command, new byte[0], 0L);
            }
            return compressToSmallest(command, source, target, log, originalSize);
        } catch (CompressionToolException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new CompressionToolException("PDF 压缩失败", ex);
        } finally {
            deleteQuietly(log);
            deleteQuietly(target);
            deleteQuietly(source);
        }
    }

    private CompressFileResultVO compressToSmallest(CompressFileCommand command, Path source, Path target, Path log,
            long originalSize) throws IOException {
        byte[] best = Files.readAllBytes(source);
        boolean reached = targetReached(best.length, command);
        for (FileCompression compression : attempts(command.resolvedCompression(), command.targetSizeBytes())) {
            runGhostscript(source, target, log, compression);
            byte[] compressed = Files.readAllBytes(target);
            if (compressed.length > 0 && compressed.length < best.length) {
                best = compressed;
            }
            if (targetReached(compressed.length, command)) {
                reached = true;
                best = compressed;
                break;
            }
        }
        return result(command, best, originalSize, reached);
    }

    private List<FileCompression> attempts(FileCompression compression, Long targetSizeBytes) {
        if (targetSizeBytes == null) {
            return List.of(compression);
        }
        return CompressionLadder.from(compression);
    }

    private void runGhostscript(Path source, Path target, Path log, FileCompression compression) {
        PdfGhostscriptSettings settings = PdfGhostscriptSettings.of(compression);
        List<String> arguments = new ArrayList<>();
        arguments.add(ghostscriptPath);
        arguments.add("-sDEVICE=pdfwrite");
        arguments.add("-dSAFER");
        arguments.add("-dCompatibilityLevel=1.5");
        arguments.add("-dNOPAUSE");
        arguments.add("-dQUIET");
        arguments.add("-dBATCH");
        arguments.add("-dDetectDuplicateImages=true");
        arguments.add("-dDownsampleColorImages=true");
        arguments.add("-dDownsampleGrayImages=true");
        arguments.add("-dDownsampleMonoImages=true");
        settings.pdfSettingsArgument().ifPresent(arguments::add);
        arguments.add("-dColorImageResolution=" + settings.imageResolution());
        arguments.add("-dGrayImageResolution=" + settings.imageResolution());
        arguments.add("-dMonoImageResolution=" + settings.imageResolution());
        arguments.add("-dJPEGQ=" + settings.jpegQuality());
        arguments.add("-sOutputFile=" + target);
        arguments.add(source.toString());

        ProcessBuilder builder = new ProcessBuilder(arguments);
        builder.redirectErrorStream(true);
        builder.redirectOutput(log.toFile());
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new CompressionToolException("PDF 压缩超时，超过 " + timeout.toMinutes() + " 分钟未完成");
            }
            if (process.exitValue() != 0) {
                throw new CompressionToolException("PDF 压缩失败：Ghostscript 退出码 " + process.exitValue()
                        + describeLog(log));
            }
        } catch (IOException ex) {
            throw new CompressionToolException(MISSING_EXECUTABLE_MESSAGE, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CompressionToolException("PDF 压缩被中断", ex);
        }
    }

    private String describeLog(Path log) {
        try {
            String content = Files.readString(log).trim();
            return content.isEmpty() ? "" : "，" + content;
        } catch (IOException ex) {
            return "";
        }
    }

    private CompressFileResultVO result(CompressFileCommand command, byte[] content, long originalSize) {
        return result(command, content, originalSize, targetReached(content.length, command));
    }

    private CompressFileResultVO result(CompressFileCommand command, byte[] content, long originalSize,
            boolean targetReached) {
        return new CompressFileResultVO(FileCompressionNames.pdfFileName(command.fileName()), PDF_CONTENT_TYPE,
                content, originalSize, content.length, command.targetSizeBytes(), targetReached);
    }

    private boolean targetReached(long size, CompressFileCommand command) {
        return command.targetSizeBytes() == null || size <= command.targetSizeBytes();
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件删除失败不影响压缩结果，交由操作系统清理。
        }
    }
}
