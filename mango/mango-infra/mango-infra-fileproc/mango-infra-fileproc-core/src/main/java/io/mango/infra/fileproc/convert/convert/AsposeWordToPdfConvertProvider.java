package io.mango.infra.fileproc.convert.convert;

import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.LoadOptions;
import com.aspose.words.PdfSaveOptions;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayOutputStream;

/**
 * 基于 Aspose.Words 的 Word 转 PDF 转换器。
 */
public class AsposeWordToPdfConvertProvider implements IConvertProvider {

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposeWordToPdfConvertProvider() {
        this((AsposeLicenseApi) null);
    }

    public AsposeWordToPdfConvertProvider(byte[] licenseContent) {
        this(product -> licenseContent == null ? new byte[0] : licenseContent.clone());
    }

    public AsposeWordToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return targetFormat == ConvertFormat.PDF
                && (sourceFormat == ConvertFormat.DOC || sourceFormat == ConvertFormat.DOCX);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        applyLicense();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(command.inputStream(), loadOptions(command));
            document.updateFields();
            document.save(outputStream, saveOptions(command));
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(outputStream.toByteArray())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Aspose Word 转 PDF 失败", ex);
        }
    }

    private PdfSaveOptions saveOptions(ConvertCommand command) {
        PdfSaveOptions saveOptions = new PdfSaveOptions();
        saveOptions.setJpegQuality(intOption(command, ConvertOptionKeys.QUALITY, 95));
        saveOptions.setExportDocumentStructure(booleanOption(command, ConvertOptionKeys.EXPORT_BOOKMARKS, true));
        return saveOptions;
    }

    private LoadOptions loadOptions(ConvertCommand command) {
        LoadOptions loadOptions = new LoadOptions();
        Object password = command.options().get(ConvertOptionKeys.PASSWORD);
        if (password != null && !password.toString().isBlank()) {
            loadOptions.setPassword(password.toString());
        }
        return loadOptions;
    }

    private void applyLicense() {
        byte[] licenseContent = licenseApi == null ? new byte[0] : licenseApi.licenseContent(AsposeProduct.WORDS);
        if (licenseApplied || licenseContent.length == 0) {
            return;
        }
        try {
            doApplyLicense(licenseContent);
        } catch (Exception ex) {
            throw new ConvertToolException("加载 Aspose.Words License 失败", ex);
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

    private boolean booleanOption(ConvertCommand command, String key, boolean defaultValue) {
        Object value = command.options().get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value != null && !value.toString().isBlank()) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }
}
