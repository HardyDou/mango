package io.mango.infra.fileproc.convert.convert;

import com.aspose.slides.License;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.PdfOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayOutputStream;

/**
 * 基于 Aspose.Slides 的 PPT 转 PDF 转换器。
 */
public class AsposeSlideToPdfConvertProvider implements IConvertProvider {

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposeSlideToPdfConvertProvider() {
        this((AsposeLicenseApi) null);
    }

    public AsposeSlideToPdfConvertProvider(byte[] licenseContent) {
        this(product -> licenseContent == null ? new byte[0] : licenseContent.clone());
    }

    public AsposeSlideToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return targetFormat == ConvertFormat.PDF
                && (sourceFormat == ConvertFormat.PPT || sourceFormat == ConvertFormat.PPTX);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Presentation presentation = new Presentation(command.inputStream(), loadOptions(command));
            try {
                presentation.save(outputStream, SaveFormat.Pdf, saveOptions(command));
            } finally {
                presentation.dispose();
            }
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(outputStream.toByteArray())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Aspose PPT 转 PDF 失败", ex);
        }
    }

    private LoadOptions loadOptions(ConvertCommand command) {
        LoadOptions loadOptions = new LoadOptions();
        Object password = command.options().get(ConvertOptionKeys.PASSWORD);
        if (password != null && !password.toString().isBlank()) {
            loadOptions.setPassword(password.toString());
        }
        return loadOptions;
    }

    private PdfOptions saveOptions(ConvertCommand command) {
        PdfOptions pdfOptions = new PdfOptions();
        pdfOptions.setJpegQuality((byte) intOption(command, ConvertOptionKeys.QUALITY, 95));
        pdfOptions.setEmbedFullFonts(true);
        pdfOptions.setDrawSlidesFrame(false);
        return pdfOptions;
    }

    private void applyLicense() {
        byte[] licenseContent = licenseApi == null ? new byte[0] : licenseApi.licenseContent(AsposeProduct.SLIDES);
        if (licenseApplied || licenseContent.length == 0) {
            return;
        }
        try {
            doApplyLicense(licenseContent);
        } catch (Exception ex) {
            throw new ConvertToolException("加载 Aspose.Slides License 失败", ex);
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
