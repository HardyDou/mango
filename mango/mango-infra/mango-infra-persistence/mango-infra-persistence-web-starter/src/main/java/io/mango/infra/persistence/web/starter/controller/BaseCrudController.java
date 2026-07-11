package io.mango.infra.persistence.web.starter.controller;

import io.mango.common.result.R;
import io.mango.infra.persistence.api.crud.BatchDeleteCommand;
import io.mango.infra.persistence.api.crud.DeleteCommand;
import io.mango.infra.persistence.api.crud.MangoCrudService;
import io.mango.infra.persistence.api.query.PersistencePageResult;
import io.mango.infra.persistence.web.starter.excel.ExcelAdapter;
import io.mango.infra.persistence.web.starter.excel.ExcelExport;
import io.mango.infra.persistence.web.starter.excel.ExcelExportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelFailureFileStore;
import io.mango.infra.persistence.web.starter.excel.ExcelImport;
import io.mango.infra.persistence.web.starter.excel.ExcelImportContext;
import io.mango.infra.persistence.web.starter.excel.ExcelImportMode;
import io.mango.infra.persistence.web.starter.excel.ExcelReadResult;
import io.mango.infra.persistence.web.starter.excel.ExcelLines;
import io.mango.infra.persistence.web.starter.excel.ExportableService;
import io.mango.infra.persistence.web.starter.excel.ImportError;
import io.mango.infra.persistence.web.starter.excel.ImportResult;
import io.mango.infra.persistence.web.starter.excel.ImportStatus;
import io.mango.infra.persistence.web.starter.excel.ImportableService;
import io.mango.infra.web.util.JacksonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标准 CRUD Controller 基类。
 *
 * @param <S> 业务服务类型。
 * @param <C> 创建命令类型。
 * @param <U> 更新命令类型。
 * @param <Q> 查询参数类型。
 */
public abstract class BaseCrudController<S extends MangoCrudService, C, U, Q> {

    protected final S service;

    @Autowired(required = false)
    private ExcelAdapter excelAdapter;

    @Autowired(required = false)
    private Validator validator;

    @Autowired(required = false)
    private ExcelFailureFileStore excelFailureFileStore;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    protected BaseCrudController(S service) {
        this.service = service;
    }

    @PostMapping("/create")
    @Operation(summary = "新增记录", description = "标准 CRUD 接口。提交创建命令并新增记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "创建命令对象，字段以具体资源定义为准")
    public R<Object> create(@RequestBody @Valid C command) {
        return R.ok(service.createByCommand(command));
    }

    @PostMapping("/update")
    @Operation(summary = "修改记录", description = "标准 CRUD 接口。提交更新命令并修改记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "更新命令对象，字段以具体资源定义为准")
    public R<Boolean> update(@RequestBody @Valid U command) {
        return R.ok(service.updateByCommand(command));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除记录", description = "标准 CRUD 接口。按主键删除单条记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "删除命令对象")
    public R<Boolean> delete(@RequestBody @Valid DeleteCommand command) {
        return R.ok(service.deleteById(command.getId()));
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除记录", description = "标准 CRUD 接口。按主键列表批量删除记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "批量删除命令对象")
    public R<Boolean> batchDelete(@RequestBody @Valid BatchDeleteCommand command) {
        return R.ok(service.batchDeleteByIds(command.getIds()));
    }

    @GetMapping("/detail")
    @Operation(summary = "获取记录详情", description = "标准 CRUD 接口。按主键查询记录详情")
    public R<Object> detail(
            @Parameter(description = "记录主键ID")
            @RequestParam("id") Long id) {
        return R.ok(service.detailById(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询记录", description = "标准 CRUD 接口。按查询条件分页查询记录")
    public R<PersistencePageResult<?>> page(@ParameterObject @ModelAttribute Q query) {
        return R.ok(service.pageByQuery(query));
    }

    @PostMapping("/export")
    @ExcelExport
    @Operation(summary = "导出记录", description = "标准 CRUD 接口。按查询条件导出记录")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "导出查询条件，字段以具体资源查询对象定义为准")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void export(@RequestBody @Valid Map<String, Object> body,
                       @Parameter(hidden = true) HttpServletResponse response) {
        if (!(service instanceof ExportableService exportableService)) {
            throw new IllegalStateException("当前资源未启用导出能力");
        }
        if (excelAdapter == null) {
            throw new IllegalStateException("Excel 导出能力未启用");
        }
        Q query = queryType().cast(convert(body, queryType()));
        List rows = exportableService.exportRows(query);
        excelAdapter.write(response, excelExportContext(exportableService.exportFileName()),
                exportableService.exportRowType(), rows);
    }

    @PostMapping("/import")
    @ExcelImport
    @Operation(summary = "导入记录", description = "标准 CRUD 接口。上传 Excel 文件并导入记录")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public R<ImportResult> importData(@Parameter(hidden = true) MultipartHttpServletRequest request) {
        if (!(service instanceof ImportableService importableService)) {
            throw new IllegalStateException("当前资源未启用导入能力");
        }
        if (excelAdapter == null) {
            throw new IllegalStateException("Excel 导入能力未启用");
        }
        ExcelImportContext context = excelImportContext("importData", MultipartHttpServletRequest.class)
                .withMode(requestMode(request));
        MultipartFile file = request.getFile(context.fileName());
        if (file == null) {
            throw new IllegalStateException("未找到上传文件: " + context.fileName());
        }
        ExcelReadResult readResult = excelAdapter.readResult(file, context, importableService.importRowType());
        List rows = readResult.rows();
        ExcelLines.fillLineNumbers(rows, context);
        ImportResult validationResult = validateImportRows(rows, context, importableService);
        List<ImportError> rowErrors = new ArrayList<>();
        List<ImportError> batchErrors = new ArrayList<>();
        collectErrors(readResult.errors(), rowErrors, batchErrors);
        collectErrors(validationResult.getErrors(), rowErrors, batchErrors);
        collectErrors(validationResult.getBatchErrors(), rowErrors, batchErrors);
        validationResult = ImportResult.failed(rows.size(), rowErrors);
        validationResult.setBatchErrors(batchErrors);
        if (rowErrors.isEmpty() && batchErrors.isEmpty()) {
            ImportResult result = importRows(importableService, rows, context);
            return R.ok(completeImportResult(file, context, rows.size(), result));
        }
        if (!batchErrors.isEmpty() || ExcelImportMode.ALL_SUCCESS.equals(context.mode())) {
            return R.ok(completeImportResult(file, context, rows.size(), validationResult));
        }
        List validRows = validRows(rows, context, validationResult.getErrors());
        if (!validRows.isEmpty()) {
            ImportResult importResult = importRows(importableService, validRows, context);
            mergeImportResult(validationResult, importResult, validRows.size());
        }
        return R.ok(completeImportResult(file, context, rows.size(), validationResult));
    }

    private void collectErrors(List<ImportError> source, List<ImportError> rowErrors,
                               List<ImportError> batchErrors) {
        if (source == null) {
            return;
        }
        for (ImportError error : source) {
            if (error != null && error.line() > 0) {
                rowErrors.add(error);
            } else if (error != null) {
                batchErrors.add(error);
            }
        }
    }

    @GetMapping("/import-template")
    @ExcelImport
    @Operation(summary = "下载导入模板", description = "标准 CRUD 接口。下载当前资源的 Excel 导入模板")
    public void importTemplate(@Parameter(hidden = true) HttpServletResponse response) {
        if (!(service instanceof ImportableService<?> importableService)) {
            throw new IllegalStateException("当前资源未启用导入能力");
        }
        if (excelAdapter == null) {
            throw new IllegalStateException("Excel 导入能力未启用");
        }
        excelAdapter.writeImportTemplate(response, excelImportContext("importTemplate", HttpServletResponse.class),
                importableService.importRowType());
    }

    protected abstract Class<Q> queryType();

    protected Object convert(Object value, Class<?> targetType) {
        if (targetType == null || Object.class.equals(targetType) || value == null || targetType.isInstance(value)) {
            return value;
        }
        return JacksonUtils.convertValue(value, targetType);
    }

    private ExcelExportContext excelExportContext(String fallbackFileName) {
        Method method = findControllerMethod("export", Map.class, HttpServletResponse.class);
        ExcelExport annotation = method == null ? null : AnnotatedElementUtils.findMergedAnnotation(method,
                ExcelExport.class);
        return ExcelExportContext.of(annotation, fallbackFileName);
    }

    private ExcelImportContext excelImportContext(String name, Class<?>... parameterTypes) {
        Method method = findControllerMethod(name, parameterTypes);
        ExcelImport annotation = method == null ? null : AnnotatedElementUtils.findMergedAnnotation(method,
                ExcelImport.class);
        return ExcelImportContext.of(annotation);
    }

    private Method findControllerMethod(String name, Class<?>... parameterTypes) {
        try {
            return getClass().getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            try {
                return BaseCrudController.class.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }

    private ExcelImportMode requestMode(MultipartHttpServletRequest request) {
        String mode = request.getParameter("importMode");
        if (mode == null || mode.isBlank()) {
            mode = request.getParameter("mode");
        }
        if (mode == null || mode.isBlank()) {
            return null;
        }
        return ExcelImportMode.valueOf(mode.trim().toUpperCase());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ImportResult validateImportRows(List<?> rows, ExcelImportContext context,
                                            ImportableService importableService) {
        if (rows == null || rows.isEmpty()) {
            return ImportResult.success(rows == null ? 0 : rows.size());
        }
        List<ImportError> errors = new ArrayList<>();
        if (validator != null) {
            for (int i = 0; i < rows.size(); i++) {
                Object row = rows.get(i);
                Set<ConstraintViolation<Object>> violations = validator.validate(row);
                int line = ExcelLines.lineNumber(context, i);
                for (ConstraintViolation<Object> violation : violations) {
                    errors.add(ImportError.of(line, violation.getPropertyPath().toString(),
                            violation.getMessage()));
                }
            }
        }
        List<ImportError> businessErrors = importableService.validateImportRows(rows, context);
        if (businessErrors != null && !businessErrors.isEmpty()) {
            errors.addAll(businessErrors);
        }
        if (errors.isEmpty()) {
            return ImportResult.success(rows.size());
        }
        return ImportResult.failed(rows.size(), errors);
    }

    private List<?> validRows(List<?> rows, ExcelImportContext context, List<ImportError> errors) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Set<Integer> failedLines = new HashSet<>();
        if (errors != null) {
            for (ImportError error : errors) {
                failedLines.add(error.line());
            }
        }
        List<Object> validRows = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (!failedLines.contains(ExcelLines.lineNumber(context, i))) {
                validRows.add(rows.get(i));
            }
        }
        return validRows;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ImportResult importRows(ImportableService service, List<?> rows, ExcelImportContext context) {
        try {
            ImportResult result;
            if (transactionManager == null) {
                result = service.importRows(rows, context);
            } else {
                result = new TransactionTemplate(transactionManager).execute(status -> service.importRows(rows, context));
            }
            return result == null ? ImportResult.success(rows.size()) : result;
        } catch (RuntimeException ex) {
            ImportError error = new ImportError(0, null, meaningfulMessage(ex), "IMPORT_BATCH_FAILED", null, null);
            ImportResult result = ImportResult.failed(rows.size(), List.of());
            result.setBatchErrors(new ArrayList<>(List.of(error)));
            result.setStatus(ImportStatus.FAILED);
            return result;
        }
    }

    private void mergeImportResult(ImportResult target, ImportResult source, int attemptedRows) {
        int success = source.getSuccess();
        List<ImportError> sourceErrors = safeErrors(source.getErrors());
        List<ImportError> sourceBatchErrors = safeErrors(source.getBatchErrors());
        if (success == 0 && source.getFailed() == 0 && sourceErrors.isEmpty() && sourceBatchErrors.isEmpty()) {
            success = attemptedRows;
        }
        target.setSuccess(Math.max(success, 0));
        target.getErrors().addAll(sourceErrors);
        target.getBatchErrors().addAll(sourceBatchErrors);
    }

    private ImportResult completeImportResult(MultipartFile file, ExcelImportContext context, int total,
                                              ImportResult result) {
        result.setErrors(new ArrayList<>(safeErrors(result.getErrors())));
        result.setBatchErrors(new ArrayList<>(safeErrors(result.getBatchErrors())));
        result.setTotal(total);
        Set<Integer> failedLines = result.getErrors().stream()
                .map(ImportError::line)
                .filter(line -> line > 0)
                .collect(java.util.stream.Collectors.toSet());
        result.setFailed(failedLines.size());
        if (result.getSuccess() == 0 && (!result.getBatchErrors().isEmpty()
                || ExcelImportMode.ALL_SUCCESS.equals(context.mode()) && !result.getErrors().isEmpty())) {
            result.setFailed(total);
        }
        if (!result.getErrors().isEmpty()) {
            storeFailureWorkbook(file, context, result);
        }
        if (result.getSuccess() > 0 && (result.getFailed() > 0 || !result.getBatchErrors().isEmpty())) {
            result.setStatus(ImportStatus.PARTIAL_SUCCESS);
        } else if (result.getFailed() > 0 || !result.getBatchErrors().isEmpty()) {
            result.setStatus(ImportStatus.FAILED);
        } else {
            result.setStatus(ImportStatus.SUCCESS);
        }
        return result;
    }

    private void storeFailureWorkbook(MultipartFile file, ExcelImportContext context, ImportResult result) {
        if (excelFailureFileStore == null) {
            return;
        }
        try {
            byte[] content = excelAdapter.createFailureWorkbook(file, context, result.getErrors());
            String fileName = failureFileName(file.getOriginalFilename());
            Long fileId = excelFailureFileStore.store(fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content, context);
            result.setFailureFileId(fileId);
        } catch (RuntimeException ex) {
            result.getBatchErrors().add(new ImportError(0, null, meaningfulMessage(ex),
                    "FAILURE_FILE_STORE_FAILED", null, null));
        }
    }

    private List<ImportError> safeErrors(List<ImportError> errors) {
        return errors == null ? List.of() : errors;
    }

    private String failureFileName(String originalFileName) {
        String name = originalFileName == null || originalFileName.isBlank() ? "import" : originalFileName.trim();
        if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            name = name.substring(0, name.length() - 5);
        }
        return name + "-failed.xlsx";
    }

    private String meaningfulMessage(RuntimeException exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName()
                : current.getMessage();
    }
}
