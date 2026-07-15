package io.mango.infra.excel.starter;

import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelHeadGenerator;
import io.mango.infra.persistence.web.starter.excel.ExcelImportException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Apache POI 导出实现，负责字段选择、表头和原生单元格类型。
 */
final class PoiExcelExporter {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int EXCEL_SAFE_INTEGER_DIGITS = 15;

    private final ApplicationContext applicationContext;

    PoiExcelExporter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    <ROW> void write(HttpServletResponse response, ExcelExportContext context, Class<ROW> rowType,
                     List<ExcelColumnBinding> allBindings, List<ROW> rows) {
        ExcelExportContext safeContext = safeContext(context);
        rejectUnsupportedTemplate(safeContext);
        List<ExcelColumnBinding> bindings = selectBindings(allBindings, safeContext);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(safeContext.sheetName());
            List<Integer> columns = physicalColumns(bindings);
            int headerRows = writeHeader(sheet, rowType, bindings, columns, safeContext);
            writeRows(workbook, sheet, headerRows, bindings, columns, rows);
            response.setContentType(XLSX_CONTENT_TYPE);
            response.setHeader("Content-Disposition", contentDisposition(safeContext.fileName()));
            workbook.write(response.getOutputStream());
        } catch (IOException ex) {
            throw new ExcelImportException("无法写出 xlsx 工作簿", ex);
        }
    }

    private ExcelExportContext safeContext(ExcelExportContext context) {
        if (context == null) {
            return new ExcelExportContext("export.xlsx", "", "", "sheet1", List.of(), List.of(),
                    ExcelHeadGenerator.class);
        }
        String fileName = normalized(context.fileName(), "export.xlsx");
        String sheetName = normalized(context.sheetName(), "sheet1");
        String templateKey = normalized(context.templateKey(), "");
        String templateLocation = normalized(context.templateLocation(), "");
        List<String> include = context.include();
        if (include == null) {
            include = List.of();
        }
        List<String> exclude = context.exclude();
        if (exclude == null) {
            exclude = List.of();
        }
        return new ExcelExportContext(fileName, templateKey, templateLocation, sheetName,
                include, exclude, context.headGenerator());
    }

    private String normalized(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim();
    }

    private void rejectUnsupportedTemplate(ExcelExportContext context) {
        if (StringUtils.hasText(context.templateKey()) || StringUtils.hasText(context.templateLocation())) {
            throw new IllegalArgumentException(
                    "POI Excel 默认导出不支持 templateKey/templateLocation，不能静默忽略；请提供自定义 ExcelAdapter");
        }
    }

    private List<ExcelColumnBinding> selectBindings(List<ExcelColumnBinding> allBindings,
                                                     ExcelExportContext context) {
        Map<String, ExcelColumnBinding> bindingsByField = allBindings.stream()
                .collect(java.util.stream.Collectors.toMap(binding -> binding.field().getName(), binding -> binding,
                        (left, right) -> left, java.util.LinkedHashMap::new));
        Set<String> include = normalizedNames(context.include());
        Set<String> exclude = normalizedNames(context.exclude());
        validateNames("include", include, bindingsByField.keySet());
        validateNames("exclude", exclude, bindingsByField.keySet());

        List<ExcelColumnBinding> selected = new ArrayList<>();
        for (ExcelColumnBinding binding : allBindings) {
            String fieldName = binding.field().getName();
            if ((include.isEmpty() || include.contains(fieldName)) && !exclude.contains(fieldName)) {
                selected.add(binding);
            }
        }
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Excel 导出字段选择结果不能为空");
        }
        return List.copyOf(selected);
    }

    private Set<String> normalizedNames(List<String> names) {
        Set<String> result = new LinkedHashSet<>();
        if (names == null) {
            return result;
        }
        for (String name : names) {
            if (StringUtils.hasText(name)) {
                result.add(name.trim());
            }
        }
        return result;
    }

    private void validateNames(String option, Set<String> names, Set<String> availableNames) {
        Set<String> unknown = new LinkedHashSet<>(names);
        unknown.removeAll(availableNames);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Excel 导出 " + option + " 包含未知字段: " + unknown);
        }
    }

    private List<Integer> physicalColumns(List<ExcelColumnBinding> bindings) {
        Set<Integer> fixed = bindings.stream()
                .map(ExcelColumnBinding::configuredIndex)
                .filter(index -> index >= 0)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Integer> assigned = new LinkedHashSet<>();
        List<Integer> result = new ArrayList<>();
        int next = 0;
        for (ExcelColumnBinding binding : bindings) {
            int column = binding.configuredIndex();
            if (column < 0) {
                while (fixed.contains(next) || assigned.contains(next)) {
                    next++;
                }
                column = next;
                next++;
            }
            if (!assigned.add(column)) {
                throw new IllegalArgumentException("Excel 导出字段重复映射列 idx=" + column);
            }
            result.add(column);
        }
        return List.copyOf(result);
    }

    private int writeHeader(Sheet sheet, Class<?> rowType, List<ExcelColumnBinding> bindings,
                            List<Integer> physicalColumns, ExcelExportContext context) {
        List<List<String>> heads = customHead(context, rowType, bindings);
        int levels = heads.stream().mapToInt(List::size).max().orElse(1);
        for (int level = 0; level < levels; level++) {
            Row row = sheet.createRow(level);
            for (int ordinal = 0; ordinal < heads.size(); ordinal++) {
                List<String> path = heads.get(ordinal);
                String title = "";
                if (level < path.size()) {
                    title = path.get(level);
                }
                row.createCell(physicalColumns.get(ordinal)).setCellValue(title);
            }
        }
        return levels;
    }

    private List<List<String>> customHead(ExcelExportContext context, Class<?> rowType,
                                          List<ExcelColumnBinding> bindings) {
        if (!context.hasCustomHeadGenerator()) {
            return bindings.stream().map(binding -> List.of(binding.displayTitle())).toList();
        }
        List<List<String>> head = headGenerator(context.headGenerator()).head(rowType);
        if (head == null || head.size() != bindings.size()) {
            throw new IllegalArgumentException("Excel 自定义表头列数必须与导出字段数一致");
        }
        List<List<String>> normalized = new ArrayList<>();
        for (List<String> path : head) {
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("Excel 自定义表头的每一列至少包含一级标题");
            }
            List<String> normalizedPath = new ArrayList<>();
            for (String title : path) {
                String normalizedTitle = title;
                if (normalizedTitle == null) {
                    normalizedTitle = "";
                }
                normalizedPath.add(normalizedTitle);
            }
            normalized.add(List.copyOf(normalizedPath));
        }
        return List.copyOf(normalized);
    }

    private ExcelHeadGenerator headGenerator(Class<? extends ExcelHeadGenerator> type) {
        Map<String, ? extends ExcelHeadGenerator> beans = applicationContext.getBeansOfType(type);
        if (beans.size() == 1) {
            return beans.values().iterator().next();
        }
        if (beans.size() > 1) {
            throw new IllegalStateException("自定义 Excel HeadGenerator 存在多个 Bean: " + type.getName());
        }
        try {
            return BeanUtils.instantiateClass(type.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("自定义 Excel HeadGenerator 必须注册为唯一 Bean 或提供无参构造: "
                    + type.getName(), ex);
        }
    }

    private <ROW> void writeRows(Workbook workbook, Sheet sheet, int firstRow,
                                 List<ExcelColumnBinding> bindings, List<Integer> physicalColumns, List<ROW> rows) {
        List<ROW> safeRows = rows;
        if (safeRows == null) {
            safeRows = List.of();
        }
        CellStyle dateStyle = dateStyle(workbook, "yyyy-mm-dd");
        CellStyle dateTimeStyle = dateStyle(workbook, "yyyy-mm-dd hh:mm:ss");
        for (int rowIndex = 0; rowIndex < safeRows.size(); rowIndex++) {
            Row row = sheet.createRow(firstRow + rowIndex);
            for (int column = 0; column < bindings.size(); column++) {
                ExcelColumnBinding binding = bindings.get(column);
                Object value = readField(binding.field(), safeRows.get(rowIndex));
                writeCell(row.createCell(physicalColumns.get(column)), value, dateStyle, dateTimeStyle);
            }
        }
    }

    private CellStyle dateStyle(Workbook workbook, String pattern) {
        CreationHelper helper = workbook.getCreationHelper();
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(helper.createDataFormat().getFormat(pattern));
        return style;
    }

    private void writeCell(Cell cell, Object value, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (writeScalar(cell, value)) {
            return;
        }
        if (writeTemporal(cell, value, dateStyle, dateTimeStyle)) {
            return;
        }
        if (value instanceof Enum<?> actual) {
            cell.setCellValue(actual.name());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private boolean writeScalar(Cell cell, Object value) {
        if (value instanceof Boolean actual) {
            cell.setCellValue(actual);
        } else if (value instanceof BigDecimal actual) {
            writeDecimal(cell, actual);
        } else if (value instanceof BigInteger actual) {
            writeInteger(cell, actual);
        } else if (value instanceof Number actual) {
            writeNumber(cell, actual);
        } else {
            return false;
        }
        return true;
    }

    private boolean writeTemporal(Cell cell, Object value, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (value instanceof LocalDate actual) {
            cell.setCellValue(actual);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime actual) {
            cell.setCellValue(actual);
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof Instant actual) {
            cell.setCellValue(LocalDateTime.ofInstant(actual, ZoneId.systemDefault()));
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof Date actual) {
            cell.setCellValue(actual);
            cell.setCellStyle(dateTimeStyle);
        } else {
            return false;
        }
        return true;
    }

    private void writeDecimal(Cell cell, BigDecimal value) {
        if (value.precision() <= EXCEL_SAFE_INTEGER_DIGITS) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(value.toPlainString());
        }
    }

    private void writeInteger(Cell cell, BigInteger value) {
        if (value.abs().toString().length() <= EXCEL_SAFE_INTEGER_DIGITS) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void writeNumber(Cell cell, Number value) {
        boolean integral = value instanceof Long || value instanceof Integer;
        integral = integral || value instanceof Short || value instanceof Byte;
        boolean unsafeIntegral = String.valueOf(value).replace("-", "").length() > EXCEL_SAFE_INTEGER_DIGITS;
        if (integral && unsafeIntegral) {
            cell.setCellValue(String.valueOf(value));
        } else {
            cell.setCellValue(value.doubleValue());
        }
    }

    private Object readField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("无法读取 Excel 字段: " + field.getName(), ex);
        }
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
