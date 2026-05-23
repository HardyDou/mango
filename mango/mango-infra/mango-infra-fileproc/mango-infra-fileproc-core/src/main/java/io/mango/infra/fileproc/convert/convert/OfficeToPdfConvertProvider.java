package io.mango.infra.fileproc.convert.convert;

import com.sun.star.document.UpdateDocMode;
import io.mango.common.result.Require;
import io.mango.infra.fileproc.convert.command.ConvertCommand;
import io.mango.infra.fileproc.convert.enums.ConvertFormat;
import io.mango.infra.fileproc.convert.vo.ConvertResultVO;
import org.jodconverter.local.LocalConverter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 kkFileView/JODConverter 链路的 Office 转 PDF 转换器。
 */
public class OfficeToPdfConvertProvider implements IConvertProvider {

    private final OfficeManagerHolder officeManagerHolder;

    public OfficeToPdfConvertProvider(OfficeManagerHolder officeManagerHolder) {
        this.officeManagerHolder = officeManagerHolder;
    }

    @Override
    public boolean supports(ConvertFormat sourceFormat, ConvertFormat targetFormat) {
        return targetFormat == ConvertFormat.PDF
                && (sourceFormat == ConvertFormat.DOC
                || sourceFormat == ConvertFormat.DOCX
                || sourceFormat == ConvertFormat.XLS
                || sourceFormat == ConvertFormat.XLSX
                || sourceFormat == ConvertFormat.PPT
                || sourceFormat == ConvertFormat.PPTX
                || sourceFormat == ConvertFormat.HTML
                || sourceFormat == ConvertFormat.TEXT);
    }

    @Override
    public ConvertResultVO convert(ConvertCommand command) {
        Require.notNull(command, "转换命令不能为空");
        Path workDir = ConvertTempFiles.createWorkDir();
        try {
            Path inputFile = ConvertTempFiles.writeInput(workDir, command);
            Path outputFile = ConvertTempFiles.output(workDir, command, ConvertFormat.PDF);
            LocalConverter.builder()
                    .officeManager(officeManagerHolder.officeManager())
                    .loadProperties(loadProperties(command))
                    .storeProperties(storeProperties(command))
                    .build()
                    .convert(inputFile.toFile())
                    .to(outputFile.toFile())
                    .execute();
            return ConvertResultVO.builder()
                    .format(ConvertFormat.PDF)
                    .fileName(ConvertFileNames.resolve(command.fileName(), ConvertFormat.PDF))
                    .contentType(ConvertFormat.PDF.contentType())
                    .content(ConvertTempFiles.readIfNeeded(command, outputFile))
                    .outputPath(command.targetPath())
                    .build();
        } catch (Exception ex) {
            throw new ConvertToolException("Office 转 PDF 失败", ex);
        } finally {
            ConvertTempFiles.deleteQuietly(workDir);
        }
    }

    private Map<String, Object> loadProperties(ConvertCommand command) {
        Map<String, Object> loadProperties = new HashMap<>();
        loadProperties.put("Hidden", true);
        loadProperties.put("ReadOnly", true);
        loadProperties.put("UpdateDocMode", UpdateDocMode.NO_UPDATE);
        String password = stringOption(command, ConvertOptionKeys.PASSWORD);
        if (password != null && !password.isBlank()) {
            loadProperties.put("Password", password);
        }
        return loadProperties;
    }

    private Map<String, Object> storeProperties(ConvertCommand command) {
        Map<String, Object> filterData = new HashMap<>();
        filterData.put("EncryptFile", true);
        putIfPresent(filterData, "PageRange", command, ConvertOptionKeys.PAGE_RANGE);
        putIfPresent(filterData, "Watermark", command, ConvertOptionKeys.WATERMARK);
        filterData.put("Quality", intOption(command, ConvertOptionKeys.QUALITY, 80));
        filterData.put("MaxImageResolution", intOption(command, ConvertOptionKeys.MAX_IMAGE_RESOLUTION, 150));
        filterData.put("ExportBookmarks", booleanOption(command, ConvertOptionKeys.EXPORT_BOOKMARKS, true));
        filterData.put("ExportNotes", booleanOption(command, ConvertOptionKeys.EXPORT_NOTES, true));

        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("FilterData", filterData);
        return customProperties;
    }

    private void putIfPresent(Map<String, Object> target, String targetKey, ConvertCommand command, String optionKey) {
        Object value = command.options().get(optionKey);
        if (value != null && !value.toString().isBlank() && !"false".equalsIgnoreCase(value.toString())) {
            target.put(targetKey, value);
        }
    }

    private String stringOption(ConvertCommand command, String key) {
        Object value = command.options().get(key);
        return value == null ? null : value.toString();
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
