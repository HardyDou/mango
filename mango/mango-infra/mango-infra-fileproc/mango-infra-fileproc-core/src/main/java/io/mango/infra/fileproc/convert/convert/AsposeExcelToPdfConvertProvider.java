package io.mango.infra.fileproc.convert.convert;

import com.aspose.cells.License;
import com.aspose.cells.LoadOptions;
import com.aspose.cells.PdfSaveOptions;
import com.aspose.cells.Workbook;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.aspose.AsposeLicenseApi;
import io.mango.infra.fileproc.aspose.enums.AsposeProduct;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * 基于 Aspose.Cells 的 Excel 转 PDF 转换器。
 */
public class AsposeExcelToPdfConvertProvider implements IConvertProvider {

    /**
     * Excel 转 PDF 时将每个工作表压缩到单页。
     */
    public static final String OPTION_ONE_PAGE_PER_SHEET = "onePagePerSheet";

    /**
     * Excel 转 PDF 时将每个工作表所有列压缩到单页宽度。
     */
    public static final String OPTION_ALL_COLUMNS_IN_ONE_PAGE_PER_SHEET = "allColumnsInOnePagePerSheet";

    private final AsposeLicenseApi licenseApi;

    private volatile boolean licenseApplied;

    public AsposeExcelToPdfConvertProvider() {
        this((AsposeLicenseApi) null);
    }

    public AsposeExcelToPdfConvertProvider(byte[] licenseContent) {
        this(product -> licenseContent == null ? new byte[0] : licenseContent.clone());
    }

    public AsposeExcelToPdfConvertProvider(AsposeLicenseApi licenseApi) {
        this.licenseApi = licenseApi;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return targetFormat == ConvertFormat.PDF
                && (sourceFormat == ConvertFormat.XLS || sourceFormat == ConvertFormat.XLSX);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        applyLicense();
        try (OutputStream outputStream = outputStream(command)) {
            Workbook workbook = new Workbook(command.inputStream(), loadOptions(command));
            try {
                workbook.calculateFormula();
                workbook.save(outputStream, saveOptions(command));
            } finally {
                workbook.dispose();
            }
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(content(command, outputStream))
                    .outputPath(command.targetPath())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Aspose Excel 转 PDF 失败", ex);
        }
    }

    private OutputStream outputStream(ConvertCommand command) throws java.io.IOException {
        if (command.hasTargetPath()) {
            ConvertTempFiles.createParent(command.targetPath());
            return Files.newOutputStream(command.targetPath());
        }
        return new ByteArrayOutputStream();
    }

    private byte[] content(ConvertCommand command, OutputStream outputStream) {
        if (command.hasTargetPath()) {
            return new byte[0];
        }
        return ((ByteArrayOutputStream) outputStream).toByteArray();
    }

    private LoadOptions loadOptions(ConvertCommand command) {
        LoadOptions loadOptions = new LoadOptions();
        Object password = command.options().get(ConvertOptionKeys.PASSWORD);
        if (password != null && !password.toString().isBlank()) {
            loadOptions.setPassword(password.toString());
        }
        return loadOptions;
    }

    private PdfSaveOptions saveOptions(ConvertCommand command) {
        PdfSaveOptions saveOptions = new PdfSaveOptions();
        saveOptions.setCalculateFormula(true);
        saveOptions.setOnePagePerSheet(booleanOption(command, OPTION_ONE_PAGE_PER_SHEET, false));
        saveOptions.setAllColumnsInOnePagePerSheet(
                booleanOption(command, OPTION_ALL_COLUMNS_IN_ONE_PAGE_PER_SHEET, true));
        saveOptions.setExportDocumentStructure(booleanOption(command, ConvertOptionKeys.EXPORT_BOOKMARKS, true));
        saveOptions.setImageResample(300, intOption(command, ConvertOptionKeys.QUALITY, 95));
        return saveOptions;
    }

    private void applyLicense() {
        byte[] licenseContent = licenseApi == null ? new byte[0] : licenseApi.licenseContent(AsposeProduct.CELLS);
        if (licenseApplied || licenseContent.length == 0) {
            return;
        }
        try {
            doApplyLicense(licenseContent);
        } catch (Exception ex) {
            throw new ConvertToolException("加载 Aspose.Cells License 失败", ex);
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
