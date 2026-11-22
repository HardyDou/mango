package io.mango.authorization.data.grant;

import io.mango.authorization.api.annotation.DataGrant;
import io.mango.common.result.R;
import io.mango.common.vo.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grant 签发通知：对成功且数据为 {@link PageResult} 的响应追加 {@code X-Mango-Data-Grant}。
 * <p>
 * {@link ResponseBodyAdvice#beforeBodyWrite} 不提供 {@code HandlerMethod}，因此从
 * {@link HandlerMapping#BEST_MATCHING_HANDLER_ATTRIBUTE} 取；只有 {@code ServletServerHttpRequest}
 * 才带 Servlet request 属性，非 Servlet 容器直接跳过签发。
 * <p>
 * 一期只支持分页响应（{@link PageResult}）；直接返回 {@code List} 的端点暂不签发 Grant，
 * 见模块 README 的已知限制。
 */
@Slf4j
@ControllerAdvice
public class DataGrantResponseAdvice implements ResponseBodyAdvice<Object> {

    private final DataGrantProperties properties;
    private final DataGrantIssuer issuer;

    /**
     * 构造响应通知。
     *
     * @param properties Grant 配置。
     * @param issuer     签发器。
     */
    public DataGrantResponseAdvice(DataGrantProperties properties, DataGrantIssuer issuer) {
        this.properties = properties;
        this.issuer = issuer;
    }

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return properties.isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(body instanceof R<?> result) || !result.isSuccess()) {
            return body;
        }
        if (!(result.getData() instanceof PageResult<?> page)) {
            return body;
        }
        HandlerMethod handlerMethod = resolveHandlerMethod(request);
        if (handlerMethod == null) {
            log.warn("无法解析当前 HandlerMethod，跳过 Grant 签发, path={}", request.getURI().getPath());
            return body;
        }
        DataGrant annotation = resolveAnnotation(handlerMethod);
        if (annotation == null) {
            return body;
        }
        List<?> records = page.getList();
        if (records == null || records.isEmpty()) {
            return body;
        }
        Set<Long> ids = extractIds(records, annotation);
        if (ids == null) {
            log.warn("响应记录缺少 ID 字段，跳过 Grant 签发, domain={}, idField={}, handler={}#{}",
                    annotation.domain(), annotation.idField(), handlerMethod.getBeanType().getSimpleName(),
                    handlerMethod.getMethod().getName());
            return body;
        }
        DataGrantIssueResult issued = issuer.issue(annotation.domain(), ids);
        if (issued.isIssued()) {
            if (!response.getHeaders().containsKey(DataGrantHeaders.GRANT)) {
                response.getHeaders().set(DataGrantHeaders.GRANT, issued.token());
            }
        } else if (DataGrantIssueResult.SKIP_OVERSIZE.equals(issued.skipReason())) {
            response.getHeaders().set(DataGrantHeaders.SKIPPED, DataGrantHeaders.SKIPPED_OVERSIZE);
        }
        return body;
    }

    /**
     * 从 Servlet request 属性中取最佳匹配的 {@link HandlerMethod}。
     *
     * @param request 当前响应请求。
     * @return HandlerMethod，非 Servlet 请求或未匹配到时返回 null。
     */
    static HandlerMethod resolveHandlerMethod(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }
        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
        Object handler = httpServletRequest.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return handler instanceof HandlerMethod handlerMethod ? handlerMethod : null;
    }

    private static DataGrant resolveAnnotation(HandlerMethod handlerMethod) {
        DataGrant annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), DataGrant.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), DataGrant.class);
    }

    /**
     * 提取记录 ID 集合，任一记录缺少或无法解析 ID 时返回 null（宁可不签发，也不签发不完整集合）。
     */
    private static Set<Long> extractIds(List<?> records, DataGrant annotation) {
        Set<Long> ids = new TreeSet<>();
        for (Object record : records) {
            Object value = readIdField(record, annotation.idField());
            if (value == null) {
                return null;
            }
            Long id = toLong(value);
            if (id == null) {
                return null;
            }
            ids.add(id);
        }
        return ids;
    }

    private static Object readIdField(Object record, String idField) {
        if (record == null || idField == null || idField.isBlank()) {
            return null;
        }
        Class<?> type = record.getClass();
        String getterName = "get" + Character.toUpperCase(idField.charAt(0)) + idField.substring(1);
        Method getter = findMethod(type, getterName);
        if (getter != null) {
            try {
                getter.setAccessible(true);
                return getter.invoke(record);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                log.warn("读取记录 ID 字段失败, type={}, idField={}", type.getName(), idField, exception);
                return null;
            }
        }
        Field field = findField(type, idField);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(record);
        } catch (IllegalAccessException exception) {
            log.warn("读取记录 ID 字段失败, type={}, idField={}", type.getName(), idField, exception);
            return null;
        }
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.valueOf(text.trim());
            } catch (NumberFormatException exception) {
                log.warn("记录 ID 字段不是合法数值, value={}", text);
                return null;
            }
        }
        log.warn("记录 ID 字段类型不受支持, type={}", value.getClass().getName());
        return null;
    }
}
