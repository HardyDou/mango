package io.mango.infra.persistence.web.starter.excel;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果。
 */
@Data
public class ImportResult {

    private int total;

    private int success;

    private int failed;

    private List<ImportError> errors = new ArrayList<>();

    public static ImportResult success(int total) {
        ImportResult result = new ImportResult();
        result.setTotal(total);
        result.setSuccess(total);
        return result;
    }

    public static ImportResult failed(int total, List<ImportError> errors) {
        return partial(total, 0, errors);
    }

    public static ImportResult partial(int total, int success, List<ImportError> errors) {
        ImportResult result = new ImportResult();
        result.setTotal(total);
        result.setSuccess(Math.max(success, 0));
        result.setFailed(countFailedRows(errors));
        result.setErrors(errors == null ? new ArrayList<>() : new ArrayList<>(errors));
        return result;
    }

    private static int countFailedRows(List<ImportError> errors) {
        if (errors == null || errors.isEmpty()) {
            return 0;
        }
        return (int) errors.stream()
                .map(ImportError::line)
                .distinct()
                .count();
    }
}
