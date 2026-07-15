package io.mango.infra.excel.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelCellValue;
import io.mango.infra.persistence.web.starter.excel.ExcelColumn;
import io.mango.infra.persistence.web.starter.excel.ExcelColumnConverter;
import io.mango.infra.persistence.web.starter.excel.ExcelColumnMetadata;
import io.mango.infra.persistence.web.starter.excel.ExcelDictionaryProvider;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportException;
import io.mango.infra.persistence.web.starter.excel.ExcelLine;
import io.mango.infra.persistence.web.starter.excel.ExcelReadResult;
import io.mango.infra.persistence.web.starter.excel.FailureRowPolicy;
import io.mango.infra.persistence.web.starter.excel.ImportError;
import io.mango.infra.persistence.web.starter.excel.UnknownColumnPolicy;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 Apache POI 的 Excel 默认适配器。
 */
public class PoiExcelAdapter implements ExcelAdapter {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy-M-d")
    };
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss")
    };

    private final ApplicationContext applicationContext;
    private final ObjectProvider<ExcelDictionaryProvider> dictionaryProvider;
    private final PoiExcelExporter exporter;

    public PoiExcelAdapter(ApplicationContext applicationContext,
                           ObjectProvider<ExcelDictionaryProvider> dictionaryProvider) {
        this.applicationContext = applicationContext;
        this.dictionaryProvider = dictionaryProvider;
        this.exporter = new PoiExcelExporter(applicationContext);
    }

    @Override
    public <ROW> List<ROW> read(MultipartFile file, ExcelImportContext context, Class<ROW> rowType) {
        ExcelReadResult<ROW> result = readResult(file, context, rowType);
        if (result.hasErrors()) {
            throw new ExcelImportException("Excel 导入内容存在错误", result.errors());
        }
        return result.rows();
    }

    @Override
    public <ROW> ExcelReadResult<ROW> readResult(MultipartFile file, ExcelImportContext context,
                                                  Class<ROW> rowType) {
        validateFile(file);
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = selectSheet(workbook, context);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.getDefault());
            List<ExcelColumnBinding> bindings = bindings(rowType);
            List<ImportError> errors = new ArrayList<>();
            resolveColumns(sheet, context, bindings, evaluator, formatter, errors);
            if (hasBatchErrors(errors)) {
                return new ExcelReadResult<>(List.of(), errors);
            }
            List<ROW> rows = readRows(sheet, context, rowType, bindings, evaluator, formatter, errors);
            return new ExcelReadResult<>(rows, errors);
        } catch (IOException ex) {
            throw new ExcelImportException("无法读取 xlsx 工作簿", ex);
        }
    }

    @Override
    public <ROW> void write(HttpServletResponse response, ExcelExportContext context, Class<ROW> rowType,
                            List<ROW> rows) {
        exporter.write(response, context, rowType, bindings(rowType), rows);
    }

    @Override
    public <ROW> void writeImportTemplate(HttpServletResponse response, ExcelImportContext context,
                                           Class<ROW> rowType) {
        if (StringUtils.hasText(context.templateLocation())) {
            writeClasspathTemplate(response, context.templateLocation());
            return;
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(importSheetName(context));
            Row header = sheet.createRow(0);
            List<ExcelColumnBinding> bindings = bindings(rowType);
            for (int i = 0; i < bindings.size(); i++) {
                ExcelColumnBinding binding = bindings.get(i);
                int column = columnFor(binding, i);
                header.createCell(column).setCellValue(binding.displayTitle());
            }
            for (int rowIndex = 1; rowIndex < context.headRowNumber(); rowIndex++) {
                sheet.createRow(rowIndex);
            }
            writeResponse(response, "import-template.xlsx", workbook);
        } catch (IOException ex) {
            throw new ExcelImportException("无法生成 Excel 导入模板", ex);
        }
    }

    @Override
    public byte[] createFailureWorkbook(MultipartFile file, ExcelImportContext context, List<ImportError> errors) {
        validateFile(file);
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = selectSheet(workbook, context);
            Map<Integer, String> reasons = errorsByLine(errors);
            int reasonColumn = lastUsedColumn(sheet) + 1;
            Row titleRow = sheet.getRow(0);
            if (titleRow == null) {
                titleRow = sheet.createRow(0);
            }
            titleRow.createCell(reasonColumn).setCellValue("失败原因");
            for (int rowIndex = sheet.getLastRowNum(); rowIndex >= context.headRowNumber(); rowIndex--) {
                int line = rowIndex + 1;
                String reason = reasons.get(line);
                if (reason == null && FailureRowPolicy.FAILED_ONLY.equals(context.failureRowPolicy())) {
                    removeRow(sheet, rowIndex);
                } else if (reason != null) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        row = sheet.createRow(rowIndex);
                    }
                    row.createCell(reasonColumn).setCellValue(reason);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new ExcelImportException("无法生成失败工作簿", ex);
        }
    }

    private <ROW> List<ROW> readRows(Sheet sheet, ExcelImportContext context, Class<ROW> rowType,
                                      List<ExcelColumnBinding> bindings, FormulaEvaluator evaluator,
                                      DataFormatter formatter, List<ImportError> errors) {
        List<ROW> rows = new ArrayList<>();
        for (int rowIndex = context.headRowNumber(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row source = sheet.getRow(rowIndex);
            if (isEmptyRow(sheet, source, bindings, evaluator, formatter)) {
                if (context.ignoreEmptyRow()) {
                    continue;
                }
            }
            ROW target = instantiate(rowType);
            fillLine(target, rowIndex + 1);
            for (ExcelColumnBinding binding : bindings) {
                Cell cell = effectiveCell(sheet, source, binding.columnIndex());
                ExcelCellValue value = new ExcelCellValue("", null, null, CellType.BLANK.name(),
                        rowIndex + 1, binding.columnIndex());
                try {
                    value = cellValue(cell, evaluator, formatter, rowIndex + 1, binding.columnIndex());
                    Object converted = convert(value, binding, context);
                    writeField(binding.field(), target, converted);
                } catch (RuntimeException ex) {
                    errors.add(ImportError.cell(rowIndex + 1, binding.field().getName(), binding.displayTitle(),
                            value.rawText(), "CELL_CONVERSION_FAILED", meaningfulMessage(ex)));
                }
            }
            rows.add(target);
        }
        return rows;
    }

    private void resolveColumns(Sheet sheet, ExcelImportContext context, List<ExcelColumnBinding> bindings,
                                FormulaEvaluator evaluator, DataFormatter formatter, List<ImportError> errors) {
        Map<String, Integer> titleIndexes = new LinkedHashMap<>();
        readTitleIndexes(sheet, evaluator, formatter, titleIndexes, errors);
        Set<String> declaredTitles = new HashSet<>();
        Set<Integer> declaredIndexes = new HashSet<>();
        Set<Integer> resolvedIndexes = new HashSet<>();
        for (ExcelColumnBinding binding : bindings) {
            resolveBinding(binding, titleIndexes, declaredTitles, declaredIndexes, errors);
            if (binding.columnIndex() >= 0 && !resolvedIndexes.add(binding.columnIndex())) {
                errors.add(batchError("DUPLICATE_COLUMN_MAPPING",
                        "多个字段映射到同一 Excel 列: " + binding.columnIndex()));
            }
        }
        if (UnknownColumnPolicy.ERROR.equals(context.unknownColumnPolicy())) {
            titleIndexes.forEach((title, index) -> {
                if (!declaredTitles.contains(title) && !declaredIndexes.contains(index)) {
                    errors.add(batchError("UNKNOWN_TITLE", "存在未声明标题: " + title));
                }
            });
        }
    }

    private void readTitleIndexes(Sheet sheet, FormulaEvaluator evaluator, DataFormatter formatter,
                                  Map<String, Integer> titleIndexes, List<ImportError> errors) {
        Row titleRow = sheet.getRow(0);
        if (titleRow == null) {
            return;
        }
        Set<String> duplicateTitles = new LinkedHashSet<>();
        int firstColumn = titleRow.getFirstCellNum();
        if (firstColumn < 0) {
            firstColumn = 0;
        }
        for (int column = firstColumn; column < titleRow.getLastCellNum(); column++) {
            Cell cell = effectiveCell(sheet, titleRow, column);
            String title = normalizeTitle(formatter.formatCellValue(cell, evaluator));
            if (!title.isEmpty() && titleIndexes.putIfAbsent(title, column) != null) {
                duplicateTitles.add(title);
            }
        }
        for (String duplicate : duplicateTitles) {
            errors.add(batchError("DUPLICATE_TITLE", "存在重复标题: " + duplicate));
        }
    }

    private void resolveBinding(ExcelColumnBinding binding, Map<String, Integer> titleIndexes,
                                Set<String> declaredTitles, Set<Integer> declaredIndexes,
                                List<ImportError> errors) {
        if (binding.configuredIndex() >= 0) {
            binding.columnIndex(binding.configuredIndex());
            declaredIndexes.add(binding.configuredIndex());
            return;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add(normalizeTitle(binding.annotation().title()));
        Arrays.stream(binding.annotation().aliases()).map(this::normalizeTitle).forEach(candidates::add);
        declaredTitles.addAll(candidates);
        Integer resolved = candidates.stream().map(titleIndexes::get).filter(Objects::nonNull).findFirst().orElse(null);
        if (resolved != null) {
            binding.columnIndex(resolved);
        } else if (binding.annotation().required()) {
            errors.add(batchError("REQUIRED_TITLE_MISSING", "缺少必填标题: " + binding.displayTitle()));
        }
    }

    private Object convert(ExcelCellValue value, ExcelColumnBinding binding, ExcelImportContext context) {
        ExcelColumn annotation = binding.annotation();
        ExcelColumnMetadata metadata = binding.metadata();
        if (!ExcelColumnConverter.None.class.equals(annotation.converter())) {
            return converter(annotation.converter()).convert(value, metadata, context);
        }
        if (StringUtils.hasText(annotation.dictType())) {
            ExcelDictionaryProvider provider = dictionaryProvider.getIfUnique();
            if (provider == null) {
                throw new IllegalStateException("字段 " + binding.field().getName() + " 配置了 dictType，但未装配字典 Provider");
            }
            String resolved = provider.resolveValue(annotation.dictType().trim(), value.rawText(), metadata, context);
            if (resolved == null) {
                throw new IllegalArgumentException("字典 " + annotation.dictType().trim() + " 不存在 label: " + value.rawText());
            }
            return convertText(resolved, binding.field().getType());
        }
        return convertBuiltIn(value, binding.field().getType());
    }

    @SuppressWarnings("unchecked")
    private ExcelColumnConverter<Object> converter(Class<? extends ExcelColumnConverter<?>> type) {
        Map<String, ? extends ExcelColumnConverter<?>> beans = applicationContext.getBeansOfType(type);
        if (beans.size() == 1) {
            return (ExcelColumnConverter<Object>) beans.values().iterator().next();
        }
        if (beans.size() > 1) {
            throw new IllegalStateException("自定义 Excel Converter 存在多个 Bean: " + type.getName());
        }
        try {
            return (ExcelColumnConverter<Object>) BeanUtils.instantiateClass(type.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("自定义 Excel Converter 必须注册为唯一 Bean 或提供无参构造: " + type.getName(), ex);
        }
    }

    private Object convertBuiltIn(ExcelCellValue value, Class<?> targetType) {
        if (String.class.equals(targetType)) {
            return value.rawText();
        }
        if (value.rawText() == null || value.rawText().isBlank()) {
            if (targetType.isPrimitive()) {
                return primitiveDefault(targetType);
            }
            return null;
        }
        if (LocalDate.class.equals(targetType) || LocalDateTime.class.equals(targetType) || Date.class.equals(targetType)
                || Instant.class.equals(targetType)) {
            return convertDate(value, targetType);
        }
        return convertText(value.rawText(), targetType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertText(String text, Class<?> targetType) {
        if (String.class.equals(targetType)) {
            return text;
        }
        String normalized = "";
        if (text != null) {
            normalized = text.trim();
        }
        Class<?> boxedType = boxedType(targetType);
        Object numeric = convertNumeric(normalized, boxedType);
        if (numeric != null) {
            return numeric;
        }
        if (Boolean.class.equals(boxedType)) {
            return convertBoolean(text, normalized);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, normalized);
        }
        throw new IllegalArgumentException("不支持的 Excel 字段类型: " + targetType.getName());
    }

    private Object convertNumeric(String normalized, Class<?> boxedType) {
        String number = normalized.replace(",", "");
        Object integral = convertIntegral(number, boxedType);
        if (integral != null) {
            return integral;
        }
        return convertDecimal(number, boxedType);
    }

    private Object convertIntegral(String number, Class<?> boxedType) {
        if (Integer.class.equals(boxedType)) {
            return new BigDecimal(number).intValueExact();
        }
        if (Long.class.equals(boxedType)) {
            return new BigDecimal(number).longValueExact();
        }
        if (Short.class.equals(boxedType)) {
            return new BigDecimal(number).shortValueExact();
        }
        if (Byte.class.equals(boxedType)) {
            return new BigDecimal(number).byteValueExact();
        }
        return null;
    }

    private Object convertDecimal(String number, Class<?> boxedType) {
        if (Double.class.equals(boxedType)) {
            return Double.valueOf(number);
        }
        if (Float.class.equals(boxedType)) {
            return Float.valueOf(number);
        }
        if (BigDecimal.class.equals(boxedType)) {
            return new BigDecimal(number);
        }
        if (BigInteger.class.equals(boxedType)) {
            return new BigDecimal(number).toBigIntegerExact();
        }
        return null;
    }

    private Boolean convertBoolean(String text, String normalized) {
        String value = normalized.toLowerCase(Locale.ROOT);
        if (Set.of("true", "1", "yes", "是").contains(value)) {
            return true;
        }
        if (Set.of("false", "0", "no", "否").contains(value)) {
            return false;
        }
        throw new IllegalArgumentException("无法转换为布尔值: " + text);
    }

    private Object convertDate(ExcelCellValue value, Class<?> targetType) {
        Date date = null;
        if (value.value() instanceof Date actual) {
            date = actual;
        }
        if (date != null) {
            Instant instant = date.toInstant();
            if (Date.class.equals(targetType)) {
                return date;
            }
            if (Instant.class.equals(targetType)) {
                return instant;
            }
            LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            if (LocalDate.class.equals(targetType)) {
                return local.toLocalDate();
            }
            return local;
        }
        if (LocalDate.class.equals(targetType)) {
            return parseDate(value.rawText());
        }
        LocalDateTime local = parseDateTime(value.rawText());
        if (LocalDateTime.class.equals(targetType)) {
            return local;
        }
        Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();
        if (Instant.class.equals(targetType)) {
            return instant;
        }
        return Date.from(instant);
    }

    private LocalDate parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个明确支持的格式。
            }
        }
        throw new IllegalArgumentException("无法转换为日期: " + value);
    }

    private LocalDateTime parseDateTime(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个明确支持的格式。
            }
        }
        return parseDate(value).atStartOfDay();
    }

    private ExcelCellValue cellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter,
                                     int line, int column) {
        if (cell == null) {
            return new ExcelCellValue("", null, null, CellType.BLANK.name(), line, column);
        }
        String rawText = formatter.formatCellValue(cell, evaluator);
        String formula = null;
        if (cell.getCellType() == CellType.FORMULA) {
            formula = cell.getCellFormula();
        }
        Object value = underlyingValue(cell, evaluator);
        return new ExcelCellValue(rawText, formula, value, cell.getCellType().name(), line, column);
    }

    private Object underlyingValue(Cell cell, FormulaEvaluator evaluator) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            return formulaValue(cell, evaluator);
        }
        return switch (type) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> numericValue(cell, cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            case BLANK -> null;
            case ERROR -> throw new IllegalArgumentException("单元格错误: " + cell.getErrorCellValue());
            default -> null;
        };
    }

    private Object formulaValue(Cell cell, FormulaEvaluator evaluator) {
        CellValue evaluated = evaluator.evaluate(cell);
        if (evaluated == null) {
            throw new IllegalArgumentException("公式没有可用计算结果: " + cell.getCellFormula());
        }
        return switch (evaluated.getCellType()) {
            case BOOLEAN -> evaluated.getBooleanValue();
            case NUMERIC -> numericValue(cell, evaluated.getNumberValue());
            case STRING -> evaluated.getStringValue();
            case BLANK -> null;
            case ERROR -> throw new IllegalArgumentException("公式计算错误: " + evaluated.getErrorValue());
            default -> null;
        };
    }

    private Object numericValue(Cell cell, double number) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }
        return BigDecimal.valueOf(number);
    }

    private List<ExcelColumnBinding> bindings(Class<?> rowType) {
        List<ExcelColumnBinding> result = new ArrayList<>();
        Set<Integer> configuredIndexes = new LinkedHashSet<>();
        Set<String> configuredTitles = new LinkedHashSet<>();
        Set<String> fieldNames = new LinkedHashSet<>();
        for (Field field : fields(rowType)) {
            ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
            if (annotation == null) {
                continue;
            }
            boolean hasTitle = StringUtils.hasText(annotation.title());
            boolean hasIndex = annotation.idx() >= 0;
            if (hasTitle == hasIndex) {
                throw new IllegalArgumentException("字段 " + field.getName() + " 的 ExcelColumn 必须且只能配置 title 或 idx");
            }
            if (!fieldNames.add(field.getName())) {
                throw new IllegalArgumentException("Excel 行类型存在重复字段名: " + field.getName());
            }
            if (hasIndex && !configuredIndexes.add(annotation.idx())) {
                throw new IllegalArgumentException("字段 " + field.getName() + " 重复映射 Excel 列 idx=" + annotation.idx());
            }
            if (hasTitle) {
                validateUniqueTitles(field, annotation, configuredTitles);
            }
            field.setAccessible(true);
            result.add(new ExcelColumnBinding(field, annotation));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Excel 行类型没有声明 @ExcelColumn: " + rowType.getName());
        }
        return result;
    }

    private void validateUniqueTitles(Field field, ExcelColumn annotation, Set<String> configuredTitles) {
        List<String> titles = new ArrayList<>();
        titles.add(annotation.title());
        titles.addAll(Arrays.asList(annotation.aliases()));
        Set<String> localTitles = new LinkedHashSet<>();
        for (String title : titles) {
            String normalized = normalizeTitle(title);
            if (normalized.isEmpty() || !localTitles.add(normalized)) {
                continue;
            }
            if (!configuredTitles.add(normalized)) {
                throw new IllegalArgumentException("字段 " + field.getName() + " 重复映射 Excel 标题: " + title);
            }
        }
    }

    private List<Field> fields(Class<?> rowType) {
        List<Field> result = new ArrayList<>();
        Class<?> current = rowType;
        while (current != null && !Object.class.equals(current)) {
            result.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return result;
    }

    private Sheet selectSheet(Workbook workbook, ExcelImportContext context) {
        if (StringUtils.hasText(context.sheetName())) {
            Sheet sheet = workbook.getSheet(context.sheetName());
            if (sheet == null) {
                throw new ExcelImportException("不存在数据 Sheet: " + context.sheetName(), List.of());
            }
            return sheet;
        }
        if (context.sheetIndex() < 0 || context.sheetIndex() >= workbook.getNumberOfSheets()) {
            throw new ExcelImportException("数据 Sheet 序号越界: " + context.sheetIndex(), List.of());
        }
        return workbook.getSheetAt(context.sheetIndex());
    }

    private Cell effectiveCell(Sheet sheet, Row row, int column) {
        if (row == null || column < 0) {
            return null;
        }
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(row.getRowNum(), column)) {
                Row firstRow = sheet.getRow(region.getFirstRow());
                if (firstRow == null) {
                    return null;
                }
                return firstRow.getCell(region.getFirstColumn());
            }
        }
        return row.getCell(column);
    }

    private boolean isEmptyRow(Sheet sheet, Row row, List<ExcelColumnBinding> bindings,
                               FormulaEvaluator evaluator, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (ExcelColumnBinding binding : bindings) {
            Cell cell = effectiveCell(sheet, row, binding.columnIndex());
            if (cell != null && StringUtils.hasText(formatter.formatCellValue(cell, evaluator))) {
                return false;
            }
        }
        return true;
    }

    private <ROW> ROW instantiate(Class<ROW> rowType) {
        try {
            return BeanUtils.instantiateClass(rowType.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("Excel 行类型必须提供无参构造: " + rowType.getName(), ex);
        }
    }

    private void fillLine(Object target, int line) {
        for (Field field : fields(target.getClass())) {
            if (!field.isAnnotationPresent(ExcelLine.class)) {
                continue;
            }
            field.setAccessible(true);
            Object value;
            if (Long.class.equals(field.getType()) || long.class.equals(field.getType())) {
                value = (long) line;
            } else if (Integer.class.equals(field.getType()) || int.class.equals(field.getType())) {
                value = line;
            } else if (String.class.equals(field.getType())) {
                value = String.valueOf(line);
            } else {
                throw new IllegalArgumentException("@ExcelLine 只支持 int、long 或 String: " + field.getName());
            }
            writeField(field, target, value);
        }
    }

    private void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("无法写入 Excel 字段: " + field.getName(), ex);
        }
    }

    private int columnFor(ExcelColumnBinding binding, int fallback) {
        if (binding.configuredIndex() >= 0) {
            return binding.configuredIndex();
        }
        return fallback;
    }

    private String importSheetName(ExcelImportContext context) {
        if (StringUtils.hasText(context.sheetName())) {
            return context.sheetName();
        }
        return "sheet1";
    }

    private Class<?> boxedType(Class<?> type) {
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        return type;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel 上传文件不能为空");
        }
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name) || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("只支持 .xlsx 文件");
        }
    }

    private String normalizeTitle(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
    }

    private ImportError batchError(String code, String message) {
        return new ImportError(0, null, message, code, null, null);
    }

    private boolean hasBatchErrors(List<ImportError> errors) {
        return errors.stream().anyMatch(error -> error.line() <= 0);
    }

    private Map<Integer, String> errorsByLine(List<ImportError> errors) {
        if (errors == null) {
            return Map.of();
        }
        return errors.stream().filter(error -> error.line() > 0)
                .collect(Collectors.groupingBy(ImportError::line, LinkedHashMap::new,
                        Collectors.mapping(ImportError::message, Collectors.joining("；"))));
    }

    private int lastUsedColumn(Sheet sheet) {
        int result = -1;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                result = Math.max(result, row.getLastCellNum() - 1);
            }
        }
        return result;
    }

    private void removeRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row != null) {
            sheet.removeRow(row);
        }
        if (rowIndex < sheet.getLastRowNum()) {
            sheet.shiftRows(rowIndex + 1, sheet.getLastRowNum(), -1);
        }
    }

    private Object primitiveDefault(Class<?> type) {
        if (boolean.class.equals(type)) {
            return false;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        if (float.class.equals(type)) {
            return 0F;
        }
        if (double.class.equals(type)) {
            return 0D;
        }
        return 0;
    }

    private String meaningfulMessage(RuntimeException ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (StringUtils.hasText(current.getMessage())) {
            return current.getMessage();
        }
        return current.getClass().getSimpleName();
    }

    private void writeClasspathTemplate(HttpServletResponse response, String location) {
        String normalized = location;
        if (location.startsWith("classpath:")) {
            normalized = location.substring("classpath:".length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Excel 模板必须是 classpath .xlsx 文件: " + location);
        }
        ClassPathResource resource = new ClassPathResource(normalized);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Excel 模板不存在: " + location);
        }
        try {
            response.setContentType(XLSX_CONTENT_TYPE);
            response.setHeader("Content-Disposition", contentDisposition(resource.getFilename()));
            resource.getInputStream().transferTo(response.getOutputStream());
        } catch (IOException ex) {
            throw new ExcelImportException("无法下载 Excel 原始模板", ex);
        }
    }

    private void writeResponse(HttpServletResponse response, String fileName, Workbook workbook) throws IOException {
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setHeader("Content-Disposition", contentDisposition(fileName));
        workbook.write(response.getOutputStream());
    }

    private String contentDisposition(String fileName) {
        String safeName = "export.xlsx";
        if (StringUtils.hasText(fileName)) {
            safeName = fileName;
        }
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

}
