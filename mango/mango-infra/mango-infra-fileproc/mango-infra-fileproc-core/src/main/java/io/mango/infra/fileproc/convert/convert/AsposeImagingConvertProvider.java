package io.mango.infra.fileproc.convert.convert;

import com.aspose.imaging.Image;
import com.aspose.imaging.License;
import com.aspose.imaging.fileformats.pdf.PdfCoreOptions;
import com.aspose.imaging.fileformats.tiff.enums.TiffExpectedFormat;
import com.aspose.imaging.imageoptions.JpegOptions;
import com.aspose.imaging.imageoptions.PdfOptions;
import com.aspose.imaging.imageoptions.PngOptions;
import com.aspose.imaging.imageoptions.TiffOptions;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 基于 Aspose.Imaging 的图片格式转换器。
 */
public class AsposeImagingConvertProvider implements IConvertProvider {

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposeImagingConvertProvider() {
        this((AsposeLicenseApi) null);
    }

    public AsposeImagingConvertProvider(byte[] licenseContent) {
        this(product -> licenseContent == null ? new byte[0] : licenseContent.clone());
    }

    public AsposeImagingConvertProvider(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return isImage(sourceFormat) && isImageOrPdf(targetFormat) && sourceFormat != targetFormat;
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Image image = Image.load(command.inputStream())) {
            image.save(outputStream, imageOptions(command));
            return ConvertResultVO.builder()
                    .format(command.targetFormat())
                    .fileName(ConvertFileNames.resolve(command.fileName(), command.targetFormat()))
                    .contentType(command.targetFormat().contentType())
                    .content(outputStream.toByteArray())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Aspose 图片格式转换失败", ex);
        }
    }

    private boolean isImage(ConvertFormat format) {
        return format == ConvertFormat.PNG || format == ConvertFormat.JPEG || format == ConvertFormat.TIFF;
    }

    private boolean isImageOrPdf(ConvertFormat format) {
        return isImage(format) || format == ConvertFormat.PDF;
    }

    private com.aspose.imaging.ImageOptionsBase imageOptions(ConvertCommand command) {
        if (command.targetFormat() == ConvertFormat.PNG) {
            return new PngOptions();
        }
        if (command.targetFormat() == ConvertFormat.JPEG) {
            JpegOptions options = new JpegOptions();
            options.setQuality(intOption(command, ConvertOptionKeys.QUALITY, 90));
            return options;
        }
        if (command.targetFormat() == ConvertFormat.TIFF) {
            return new TiffOptions(TiffExpectedFormat.TiffLzwRgb);
        }
        PdfOptions options = new PdfOptions();
        useOriginalImageSizeIfSupported(options);
        PdfCoreOptions coreOptions = new PdfCoreOptions();
        coreOptions.setJpegQuality(intOption(command, ConvertOptionKeys.QUALITY, 90));
        options.setPdfCoreOptions(coreOptions);
        return options;
    }

    private void useOriginalImageSizeIfSupported(PdfOptions options) {
        Method method = Arrays.stream(PdfOptions.class.getMethods())
                .filter(candidate -> "setUseOriginalImageSize".equals(candidate.getName()))
                .filter(candidate -> candidate.getParameterCount() == 1)
                .filter(candidate -> candidate.getParameterTypes()[0] == boolean.class)
                .findFirst()
                .orElse(null);
        if (method == null) {
            return;
        }
        try {
            method.invoke(options, true);
        } catch (ReflectiveOperationException ex) {
            throw new ConvertToolException("设置 Aspose.Imaging PDF 原始尺寸失败", ex);
        }
    }

    private void applyLicense() {
        byte[] licenseContent = licenseApi == null ? new byte[0] : licenseApi.licenseContent(AsposeProduct.IMAGING);
        if (licenseApplied || licenseContent.length == 0) {
            return;
        }
        try {
            doApplyLicense(licenseContent);
        } catch (Exception ex) {
            throw new ConvertToolException("加载 Aspose.Imaging License 失败", ex);
        }
    }

    private synchronized void doApplyLicense(byte[] licenseContent) {
        if (licenseApplied) {
            return;
        }
        try (java.io.InputStream inputStream = new java.io.ByteArrayInputStream(licenseContent)) {
            new License().setLicense(inputStream);
            licenseApplied = true;
        } catch (Exception ex) {
            throw new ConvertToolException("加载 Aspose.Imaging License 失败", ex);
        }
    }

    private int intOption(ConvertCommand command, String key, int defaultValue) {
        Object value = command.options().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null && !value.toString().isBlank()) {
            return Integer.parseInt(value.toString());
        }
        return defaultValue;
    }
}
