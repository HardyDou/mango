package io.mango.infra.web.starter;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 * 统一返回 code + message
 *
 * @author Mango
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int BAD_REQUEST_CODE = HttpStatus.BAD_REQUEST.value();
    private static final int NOT_FOUND_CODE = HttpStatus.NOT_FOUND.value();
    private static final int METHOD_NOT_ALLOWED_CODE = HttpStatus.METHOD_NOT_ALLOWED.value();
    private static final int INTERNAL_SERVER_ERROR_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private static final int FILE_TOO_LARGE_CODE = 3406;
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_JSON_PATH_LENGTH = 256;
    private static final Pattern SAFE_JSON_FIELD = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,63}");
    private static final String MALFORMED_JSON_MESSAGE = "请求体格式错误，请检查 JSON 语法和字段格式";

    // ==================== 业务异常 ====================

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBizException(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    // ==================== 参数校验 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(BAD_REQUEST_CODE, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return R.fail(BAD_REQUEST_CODE, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .collect(Collectors.joining(", "));
        return R.fail(BAD_REQUEST_CODE, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        return R.fail(BAD_REQUEST_CODE, "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return R.fail(BAD_REQUEST_CODE, "参数类型错误: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        String message = jsonRequestErrorMessage(e);
        log.warn("请求体解析失败, method={}, uri={}, query={}, category={}, exception={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(), jsonErrorCategory(e),
                e.getClass().getName(), e);
        return R.fail(BAD_REQUEST_CODE, message);
    }

    private String jsonRequestErrorMessage(HttpMessageNotReadableException exception) {
        UnrecognizedPropertyException unrecognized = findCause(exception, UnrecognizedPropertyException.class);
        if (unrecognized != null) {
            String path = jsonPath(unrecognized, unrecognized.getPropertyName());
            return path == null ? MALFORMED_JSON_MESSAGE : "请求字段 " + path + " 不受支持";
        }

        InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
        if (invalidFormat != null) {
            String path = jsonPath(invalidFormat, null);
            if (path == null) {
                return MALFORMED_JSON_MESSAGE;
            }
            if (isDateTimeType(invalidFormat.getTargetType())) {
                return "请求字段 " + path + " 日期时间格式不正确";
            }
            return typeMismatchMessage(path, invalidFormat.getTargetType());
        }

        MismatchedInputException mismatchedInput = findCause(exception, MismatchedInputException.class);
        if (mismatchedInput != null) {
            String path = jsonPath(mismatchedInput, null);
            if (path == null) {
                return MALFORMED_JSON_MESSAGE;
            }
            if (isDateTimeType(mismatchedInput.getTargetType())) {
                return "请求字段 " + path + " 日期时间格式不正确";
            }
            return typeMismatchMessage(path, mismatchedInput.getTargetType());
        }
        return MALFORMED_JSON_MESSAGE;
    }

    private String jsonErrorCategory(HttpMessageNotReadableException exception) {
        if (findCause(exception, UnrecognizedPropertyException.class) != null) {
            return "unsupported-field";
        }
        InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
        if (invalidFormat != null && isDateTimeType(invalidFormat.getTargetType())) {
            return "date-time-format";
        }
        if (invalidFormat != null || findCause(exception, MismatchedInputException.class) != null) {
            return "type-mismatch";
        }
        return "malformed-json";
    }

    private String typeMismatchMessage(String path, Class<?> targetType) {
        return "请求字段 " + path + " 类型不正确，期望 " + safeTypeCategory(targetType);
    }

    private String safeTypeCategory(Class<?> targetType) {
        if (targetType == null) {
            return "正确的字段类型";
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return "BOOLEAN";
        }
        if (Number.class.isAssignableFrom(targetType) || isNumericPrimitive(targetType)) {
            return "NUMBER";
        }
        if (targetType.isArray() || Collection.class.isAssignableFrom(targetType)) {
            return "ARRAY";
        }
        if (Map.class.isAssignableFrom(targetType)) {
            return "OBJECT";
        }
        if (targetType.isEnum()) {
            return "ENUM";
        }
        if (CharSequence.class.isAssignableFrom(targetType) || targetType == char.class
                || targetType == Character.class) {
            return "STRING";
        }
        return "OBJECT";
    }

    private boolean isNumericPrimitive(Class<?> targetType) {
        return targetType == byte.class || targetType == short.class || targetType == int.class
                || targetType == long.class || targetType == float.class || targetType == double.class;
    }

    private boolean isDateTimeType(Class<?> targetType) {
        return targetType != null && (TemporalAccessor.class.isAssignableFrom(targetType)
                || Date.class.isAssignableFrom(targetType)
                || Calendar.class.isAssignableFrom(targetType));
    }

    private String jsonPath(JsonMappingException exception, String trailingField) {
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : exception.getPath()) {
            String fieldName = safeFieldName(reference.getFieldName());
            if (reference.getFieldName() != null && fieldName == null) {
                return null;
            }
            if (fieldName != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(fieldName);
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
            if (path.length() > MAX_JSON_PATH_LENGTH) {
                return null;
            }
        }

        String safeTrailingField = safeFieldName(trailingField);
        if (trailingField != null && safeTrailingField == null) {
            return null;
        }
        if (safeTrailingField != null && !pathEndsWithField(path, safeTrailingField)) {
            if (!path.isEmpty()) {
                path.append('.');
            }
            path.append(safeTrailingField);
        }
        return path.isEmpty() || path.length() > MAX_JSON_PATH_LENGTH ? null : path.toString();
    }

    private String safeFieldName(String fieldName) {
        return fieldName != null && SAFE_JSON_FIELD.matcher(fieldName).matches() ? fieldName : null;
    }

    private boolean pathEndsWithField(StringBuilder path, String fieldName) {
        int start = path.length() - fieldName.length();
        return start >= 0 && path.substring(start).equals(fieldName)
                && (start == 0 || path.charAt(start - 1) == '.');
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }

    // ==================== 404 ====================

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return R.fail(NOT_FOUND_CODE, "资源不存在");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(METHOD_NOT_ALLOWED_CODE, "不支持的请求方法: " + e.getMethod());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return R.fail(FILE_TOO_LARGE_CODE, "文件大小超过限制");
    }

    // ==================== 数据库异常 ====================

    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleSqlException(SQLException e, HttpServletRequest request) {
        log.warn("数据库异常, method={}, uri={}, query={}, exception={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(), e.getClass().getName(), e);
        return R.fail(INTERNAL_SERVER_ERROR_CODE, "数据库操作异常");
    }

    // ==================== 其它异常 ====================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.warn("系统异常, method={}, uri={}, query={}, exception={}",
                request.getMethod(), request.getRequestURI(), request.getQueryString(), e.getClass().getName(), e);
        return R.fail(INTERNAL_SERVER_ERROR_CODE, "系统异常");
    }
}
