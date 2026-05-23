package io.mango.infra.fileproc.render.service;

import com.aspose.pdf.Color;
import com.aspose.pdf.Document;
import com.aspose.pdf.License;
import com.aspose.pdf.Rotation;
import com.aspose.pdf.TextStamp;
import com.aspose.pdf.optimization.ImageCompressionOptions;
import com.aspose.pdf.optimization.ImageCompressionVersion;
import com.aspose.pdf.optimization.ImageEncoding;
import com.aspose.pdf.optimization.OptimizationOptions;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.render.RenderApi;
import io.mango.infra.fileproc.render.command.AddPdfWatermarkCommand;
import io.mango.infra.fileproc.render.command.CompressPdfCommand;
import io.mango.infra.fileproc.render.command.CompressPdfToTargetCommand;
import io.mango.infra.fileproc.render.command.MergePdfCommand;
import io.mango.infra.fileproc.render.command.RenderCommand;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageEncoding;
import io.mango.infra.fileproc.render.enums.PdfCompressionImageVersion;
import io.mango.infra.fileproc.render.enums.PdfCompressionPreset;
import io.mango.infra.fileproc.render.enums.RenderFormat;
import io.mango.infra.fileproc.render.vo.PdfCompressionResultVO;
import io.mango.infra.fileproc.render.vo.PdfOperationResultVO;
import io.mango.infra.fileproc.render.vo.PdfSourceVO;
import io.mango.infra.fileproc.render.vo.RenderFormatPairVO;
import io.mango.infra.fileproc.render.vo.RenderResultVO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 基于 Aspose.PDF 的渲染处理实现。
 */
public class AsposePdfRenderApi implements RenderApi {

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposePdfRenderApi(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

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
        return List.of();
    }

    @Override
    public Set<RenderFormatPairVO> supportedRenderings() {
        return Set.of();
    }

    @Override
    public PdfOperationResultVO mergePdf(MergePdfCommand command) {
        Require.notNull(command, "PDF 合并命令不能为空");
        Require.notEmpty(command.sources(), "PDF 合并输入不能为空");
        ensureSupportedLocale();
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document output = new Document();
            try {
                output.getPages().delete();
                for (PdfSourceVO source : command.sources()) {
                    Document input = new Document(source.inputStream());
                    try {
                        output.getPages().add(input.getPages());
                    } finally {
                        close(input);
                    }
                }
                output.save(outputStream);
                return new PdfOperationResultVO(resolvePdfFileName(command.fileName(), "merged.pdf"),
                        outputStream.toByteArray());
            } finally {
                close(output);
            }
        } catch (Exception ex) {
            throw new RenderToolException("Aspose PDF 合并失败", ex);
        }
    }

    @Override
    public PdfOperationResultVO addPdfWatermark(AddPdfWatermarkCommand command) {
        Require.notNull(command, "PDF 水印命令不能为空");
        Require.notBlank(command.watermarkText(), "PDF 水印文本不能为空");
        ensureSupportedLocale();
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(command.inputStream());
            try {
                TextStamp stamp = watermark(command.watermarkText());
                for (int pageIndex = 1; pageIndex <= document.getPages().size(); pageIndex++) {
                    document.getPages().get_Item(pageIndex).addStamp(stamp);
                }
                document.save(outputStream);
                return new PdfOperationResultVO(resolvePdfFileName(command.fileName(), "watermarked.pdf"),
                        outputStream.toByteArray());
            } finally {
                close(document);
            }
        } catch (Exception ex) {
            throw new RenderToolException("Aspose PDF 水印失败", ex);
        }
    }

    @Override
    public PdfOperationResultVO compressPdf(CompressPdfCommand command) {
        Require.notNull(command, "PDF 压缩命令不能为空");
        ensureSupportedLocale();
        applyLicense();
        try {
            byte[] compressed = compress(readAllBytes(command.inputStream()), command);
            return new PdfOperationResultVO(resolvePdfFileName(command.fileName(), "compressed.pdf"),
                    compressed);
        } catch (Exception ex) {
            throw new RenderToolException("Aspose PDF 压缩失败", ex);
        }
    }

    @Override
    public PdfCompressionResultVO compressPdfToTarget(CompressPdfToTargetCommand command) {
        Require.notNull(command, "PDF 目标压缩命令不能为空");
        ensureSupportedLocale();
        applyLicense();
        try {
            byte[] source = readAllBytes(command.inputStream());
            TargetCompressionSettings settings = targetSettings(command);
            if (source.length <= command.targetSizeBytes()) {
                return new PdfCompressionResultVO(resolvePdfFileName(command.fileName(), "compressed.pdf"),
                        source,
                        source.length,
                        source.length,
                        command.targetSizeBytes(),
                        true,
                        settings.preferredQuality(),
                        settings.preferredResolution(),
                        0);
            }

            byte[] bestContent = source;
            int bestQuality = settings.preferredQuality();
            int bestResolution = settings.preferredResolution();
            int actualIterations = 0;
            for (int index = 0; index < settings.maxIterations(); index++) {
                actualIterations = index + 1;
                int quality = stepValue(settings.preferredQuality(), settings.minQuality(),
                        index, settings.maxIterations());
                int resolution = stepValue(settings.preferredResolution(), settings.minResolution(),
                        index, settings.maxIterations());
                byte[] compressed = compress(source, targetCommand(command, quality, resolution));
                if (compressed.length < bestContent.length) {
                    bestContent = compressed;
                    bestQuality = quality;
                    bestResolution = resolution;
                }
                if (compressed.length <= command.targetSizeBytes()) {
                    return new PdfCompressionResultVO(resolvePdfFileName(command.fileName(), "compressed.pdf"),
                            compressed,
                            source.length,
                            compressed.length,
                            command.targetSizeBytes(),
                            true,
                            quality,
                            resolution,
                            actualIterations);
                }
            }

            if (settings.strictTarget()) {
                throw new RenderToolException("PDF 压缩后仍超过目标大小");
            }
            return new PdfCompressionResultVO(resolvePdfFileName(command.fileName(), "compressed.pdf"),
                    bestContent,
                    source.length,
                    bestContent.length,
                    command.targetSizeBytes(),
                    false,
                    bestQuality,
                    bestResolution,
                    actualIterations);
        } catch (RenderToolException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RenderToolException("Aspose PDF 目标压缩失败", ex);
        }
    }

    private byte[] compress(byte[] source, CompressPdfCommand command) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(new ByteArrayInputStream(source));
            try {
                document.optimizeResources(optimizationOptions(command));
                document.save(outputStream);
                return outputStream.toByteArray();
            } finally {
                close(document);
            }
        }
    }

    private CompressPdfCommand targetCommand(CompressPdfToTargetCommand command, int quality, int resolution) {
        return new CompressPdfCommand(command.fileName(),
                new ByteArrayInputStream(new byte[0]),
                command.initialPreset() == null ? PdfCompressionPreset.HIGH : command.initialPreset(),
                true,
                true,
                true,
                true,
                true,
                true,
                true,
                quality,
                resolution,
                command.imageEncoding() == null ? PdfCompressionImageEncoding.JPEG : command.imageEncoding(),
                command.imageVersion() == null ? PdfCompressionImageVersion.FAST : command.imageVersion(),
                false,
                true,
                true);
    }

    private TargetCompressionSettings targetSettings(CompressPdfToTargetCommand command) {
        int preferredQuality = command.preferredImageQuality() == null ? 75 : command.preferredImageQuality();
        int minQuality = command.minImageQuality() == null ? 35 : command.minImageQuality();
        int preferredResolution = command.preferredResolution() == null ? 220 : command.preferredResolution();
        int minResolution = command.minResolution() == null ? 100 : command.minResolution();
        int maxIterations = command.maxIterations() == null ? 6 : command.maxIterations();
        boolean strictTarget = Boolean.TRUE.equals(command.strictTarget());
        return new TargetCompressionSettings(preferredQuality, minQuality, preferredResolution, minResolution,
                maxIterations, strictTarget);
    }

    private int stepValue(int start, int end, int index, int iterations) {
        if (iterations <= 1) {
            return end;
        }
        double ratio = (double) index / (double) (iterations - 1);
        return Math.max(end, (int) Math.round(start - ((start - end) * ratio)));
    }

    private OptimizationOptions optimizationOptions(CompressPdfCommand command) {
        PdfCompressionPreset preset = command.preset() == null ? PdfCompressionPreset.DEFAULT : command.preset();
        OptimizationOptions options = new OptimizationOptions();
        applyPreset(options, preset);
        applyOverrides(options, command);
        return options;
    }

    private void applyPreset(OptimizationOptions options, PdfCompressionPreset preset) {
        switch (preset) {
            case LOW -> {
                applyStructureOptimization(options);
                options.setCompressImages(true);
                options.setResizeImages(false);
                options.setImageQuality(90);
                options.setImageEncoding(ImageEncoding.Unchanged);
            }
            case MEDIUM, DEFAULT -> {
                applyStructureOptimization(options);
                options.setCompressImages(true);
                options.setResizeImages(true);
                options.setImageQuality(75);
                options.setMaxResoultion(220);
                options.setImageEncoding(ImageEncoding.Jpeg);
            }
            case HIGH -> {
                applyStructureOptimization(options);
                options.setCompressImages(true);
                options.setResizeImages(true);
                options.setImageQuality(55);
                options.setMaxResoultion(150);
                options.setImageEncoding(ImageEncoding.Jpeg);
                options.setSubsetFonts(true);
                options.setRemovePrivateInfo(true);
            }
            case STRUCTURE_ONLY -> applyStructureOptimization(options);
            case CUSTOM -> {
            }
        }
    }

    private void applyStructureOptimization(OptimizationOptions options) {
        invokeBooleanSetterIfPresent(options, "setCompressObjects", true);
        invokeBooleanSetterIfPresent(options, "setLinkDuplicateStreams", true);
        invokeBooleanSetterIfPresent(options, "setLinkDuplcateStreams", true);
        options.setAllowReusePageContent(true);
        options.setRemoveUnusedStreams(true);
        options.setRemoveUnusedObjects(true);
    }

    private void applyOverrides(OptimizationOptions options, CompressPdfCommand command) {
        if (command.compressObjects() != null) {
            invokeBooleanSetterIfPresent(options, "setCompressObjects", command.compressObjects());
        }
        if (command.linkDuplicateStreams() != null) {
            if (!invokeBooleanSetterIfPresent(options, "setLinkDuplicateStreams", command.linkDuplicateStreams())) {
                invokeBooleanSetterIfPresent(options, "setLinkDuplcateStreams", command.linkDuplicateStreams());
            }
        }
        setIfNotNull(command.allowReusePageContent(), options::setAllowReusePageContent);
        setIfNotNull(command.removeUnusedStreams(), options::setRemoveUnusedStreams);
        setIfNotNull(command.removeUnusedObjects(), options::setRemoveUnusedObjects);
        setIfNotNull(command.compressImages(), options::setCompressImages);
        setIfNotNull(command.resizeImages(), options::setResizeImages);
        setIfNotNull(command.imageQuality(), options::setImageQuality);
        setIfNotNull(command.maxResolution(), options::setMaxResoultion);
        setIfNotNull(command.unembedFonts(), options::setUnembedFonts);
        setIfNotNull(command.subsetFonts(), options::setSubsetFonts);
        setIfNotNull(command.removePrivateInfo(), options::setRemovePrivateInfo);
        if (command.imageEncoding() != null) {
            options.setImageEncoding(imageEncoding(command.imageEncoding()));
        }
        ImageCompressionOptions imageOptions = options.getImageCompressionOptions();
        if (imageOptions != null && command.imageVersion() != null) {
            imageOptions.setVersion(imageVersion(command.imageVersion()));
        }
    }

    private void setIfNotNull(Boolean value, java.util.function.Consumer<Boolean> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void setIfNotNull(Integer value, java.util.function.IntConsumer setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private boolean invokeBooleanSetterIfPresent(Object target, String methodName, Boolean value) {
        try {
            Method method = target.getClass().getMethod(methodName, boolean.class);
            method.invoke(target, value);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        } catch (ReflectiveOperationException ex) {
            throw new RenderToolException("Aspose PDF 压缩参数设置失败: " + methodName, ex);
        }
    }

    private int imageEncoding(PdfCompressionImageEncoding encoding) {
        return switch (encoding) {
            case UNCHANGED -> ImageEncoding.Unchanged;
            case JPEG -> ImageEncoding.Jpeg;
            case FLATE -> ImageEncoding.Flate;
            case JPEG2000 -> ImageEncoding.Jpeg2000;
        };
    }

    private int imageVersion(PdfCompressionImageVersion version) {
        return switch (version) {
            case STANDARD -> ImageCompressionVersion.Standard;
            case FAST -> ImageCompressionVersion.Fast;
            case MIXED -> ImageCompressionVersion.Mixed;
        };
    }

    private TextStamp watermark(String text) {
        TextStamp stamp = new TextStamp(text);
        stamp.setBackground(true);
        stamp.setOpacity(0.18D);
        stamp.setRotate(Rotation.None);
        stamp.setRotateAngle(45D);
        stamp.setHorizontalAlignment(com.aspose.pdf.HorizontalAlignment.Center);
        stamp.setVerticalAlignment(com.aspose.pdf.VerticalAlignment.Center);
        stamp.getTextState().setFontSize(48F);
        stamp.getTextState().setForegroundColor(Color.fromArgb(150, 150, 150));
        return stamp;
    }

    private String resolvePdfFileName(String fileName, String defaultName) {
        if (fileName == null || fileName.isBlank()) {
            return defaultName;
        }
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return baseName + ".pdf";
    }

    private byte[] readAllBytes(java.io.InputStream inputStream) throws IOException {
        try (inputStream) {
            return inputStream.readAllBytes();
        }
    }

    private void close(Document document) {
        if (document != null) {
            document.close();
        }
    }

    private void applyLicense() {
        ensureSupportedLocale();
        byte[] licenseContent = licenseApi == null ? new byte[0] : licenseApi.licenseContent(AsposeProduct.PDF);
        if (licenseApplied || licenseContent.length == 0) {
            return;
        }
        try {
            doApplyLicense(licenseContent);
        } catch (Exception ex) {
            throw new RenderToolException("加载 Aspose.PDF License 失败", ex);
        }
    }

    private synchronized void doApplyLicense(byte[] licenseContent) throws Exception {
        if (licenseApplied) {
            return;
        }
        try (java.io.InputStream inputStream = new java.io.ByteArrayInputStream(licenseContent)) {
            new License().setLicense(inputStream);
            licenseApplied = true;
        }
    }

    private record TargetCompressionSettings(
            int preferredQuality,
            int minQuality,
            int preferredResolution,
            int minResolution,
            int maxIterations,
            boolean strictTarget) {
    }

    private void ensureSupportedLocale() {
        Locale locale = Locale.getDefault();
        if ("zh".equals(locale.getLanguage()) && "CN".equals(locale.getCountry()) && locale.getScript() != null
                && !locale.getScript().isBlank()) {
            Locale.setDefault(Locale.CHINA);
        }
    }
}
