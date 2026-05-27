package io.mango.infra.fileproc.convert.convert;

import com.aspose.pdf.Document;
import com.aspose.pdf.License;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.devices.PngDevice;
import com.aspose.pdf.devices.Resolution;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * 基于 Aspose.PDF 的 PDF 首页转图片转换器。
 */
public class AsposePdfToImageConvertProvider implements IConvertProvider {

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposePdfToImageConvertProvider() {
        this((AsposeLicenseApi) null);
    }

    public AsposePdfToImageConvertProvider(byte[] licenseContent) {
        this(product -> licenseContent == null ? new byte[0] : licenseContent.clone());
    }

    public AsposePdfToImageConvertProvider(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return sourceFormat == ConvertFormat.PDF
                && (targetFormat == ConvertFormat.PNG || targetFormat == ConvertFormat.JPEG);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(command.inputStream(), password(command));
            try {
                if (command.targetFormat() == ConvertFormat.PNG) {
                    new PngDevice(new Resolution(dpi(command))).process(document.getPages().get_Item(1), outputStream);
                } else {
                    new JpegDevice(new Resolution(dpi(command)), quality(command)).process(
                            document.getPages().get_Item(1), outputStream);
                }
            } finally {
                document.close();
            }
            return ConvertResultVO.builder()
                    .format(command.targetFormat())
                    .fileName(ConvertFileNames.resolve(command.fileName(), command.targetFormat()))
                    .contentType(command.targetFormat().contentType())
                    .content(outputStream.toByteArray())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Aspose PDF 转图片失败", ex);
        }
    }

    private String password(ConvertCommand command) {
        Object password = command.options().get(ConvertOptionKeys.PASSWORD);
        return password == null ? null : password.toString();
    }

    private int dpi(ConvertCommand command) {
        return intOption(command, ConvertOptionKeys.DPI, 144);
    }

    private int quality(ConvertCommand command) {
        return intOption(command, ConvertOptionKeys.QUALITY, 90);
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
            throw new ConvertToolException("加载 Aspose.PDF License 失败", ex);
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

    private void ensureSupportedLocale() {
        Locale locale = Locale.getDefault();
        if ("zh".equals(locale.getLanguage()) && "CN".equals(locale.getCountry()) && locale.getScript() != null
                && !locale.getScript().isBlank()) {
            Locale.setDefault(Locale.CHINA);
        }
    }
}
