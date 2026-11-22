package io.mango.authorization.data.grant;

import io.mango.authorization.api.annotation.DataGrant;
import io.mango.common.vo.PageResult;
import io.mango.common.result.R;
import io.mango.infra.web.support.InternalCallAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Grant 校验拦截器。
 * <p>
 * 只对「带 {@link DataGrant} 且能解析出单个记录 ID 的端点」生效：列表端点没有记录 ID，直接放行（由
 * {@link DataGrantResponseAdvice} 负责签发）；内部调用已由网关验签，跳过校验。
 * <p>
 * 记录 ID 不在 Grant 集合内时**在进入 Controller 之前**返回 404，文案与「资源不存在」完全一致，
 * 不先查库再判权，避免通过响应时间或错误文案区分「记录不存在」与「记录存在但无权」。
 * <p>
 * 前端可能一次性回传多个数据域的 Grant，因此请求头按{@link DataGrantTokens 多值}解析；
 * 头里只有其它数据域令牌或整体超限时与「未携带」完全一致，不把「带了别人的 Grant」当成凭证无效。
 */
@Slf4j
public class DataGrantInterceptor implements HandlerInterceptor {

    private final DataGrantProperties properties;
    private final DataGrantVerifier verifier;

    /**
     * 构造拦截器。
     *
     * @param properties Grant 配置。
     * @param verifier   校验器。
     */
    public DataGrantInterceptor(DataGrantProperties properties, DataGrantVerifier verifier) {
        this.properties = properties;
        this.verifier = verifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!properties.isEnabled() || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        DataGrant annotation = resolveAnnotation(handlerMethod);
        if (annotation == null) {
            return true;
        }
        if (request.getAttribute(InternalCallAttributes.VERIFIED) != null) {
            return true;
        }
        Long recordId = resolveRecordId(request, handlerMethod, annotation);
        if (recordId == null) {
            return true;
        }
        List<String> tokens = DataGrantTokens.parse(Collections.list(request.getHeaders(DataGrantHeaders.GRANT)));
        if (tokens.isEmpty()) {
            return handleAbsent(request, response, annotation);
        }
        DataGrantVerification verification = verifier.verify(tokens, annotation.domain(), recordId);
        if (verification.isPassed()) {
            return true;
        }
        if (verification.status() == DataGrantVerification.Status.ABSENT) {
            // 头里全是其它数据域的令牌：与「未携带」完全一致，optional 放行、enforce 拒绝。
            return handleAbsent(request, response, annotation);
        }
        log.warn("数据权限 Grant 校验未通过, domain={}, recordId={}, status={}, path={}",
                annotation.domain(), recordId, verification.status(), request.getRequestURI());
        return switch (verification.status()) {
            case ID_NOT_FOUND -> reject(response, annotation.domain(), HttpStatus.NOT_FOUND,
                    DataGrantHeaders.MESSAGE_RESOURCE_NOT_FOUND, false);
            case EXPIRED -> reject(response, annotation.domain(), HttpStatus.FORBIDDEN, verification.message(), true);
            case SCOPE_CHANGED -> reject(response, annotation.domain(), HttpStatus.FORBIDDEN, verification.message(), true);
            default -> reject(response, annotation.domain(), HttpStatus.FORBIDDEN,
                    DataGrantHeaders.MESSAGE_GRANT_INVALID, false);
        };
    }

    private boolean handleAbsent(HttpServletRequest request, HttpServletResponse response, DataGrant annotation)
            throws IOException {
        if (properties.getMode() == DataGrantProperties.Mode.ENFORCE) {
            log.warn("强制模式下缺少数据权限 Grant, domain={}, path={}",
                    annotation.domain(), request.getRequestURI());
            return reject(response, annotation.domain(), HttpStatus.FORBIDDEN,
                    DataGrantHeaders.MESSAGE_GRANT_REQUIRED, true);
        }
        response.setHeader(DataGrantHeaders.DOMAIN, annotation.domain());
        response.setHeader(DataGrantHeaders.GRANT, DataGrantHeaders.ABSENT);
        log.warn("缺少数据权限 Grant，按 optional 模式继续处理, domain={}, path={}",
                annotation.domain(), request.getRequestURI());
        return true;
    }

    /**
     * 解析端点的 {@link DataGrant} 注解，方法级优先、类级兜底。
     */
    private static DataGrant resolveAnnotation(HandlerMethod handlerMethod) {
        DataGrant annotation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), DataGrant.class);
        if (annotation != null) {
            return annotation;
        }
        return AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), DataGrant.class);
    }

    /**
     * 解析请求访问的单条记录 ID。
     * <p>
     * 顺序：注解 {@code idField} 同名参数 → 任意数值型 {@code @RequestParam}（如 {@code applyId}）→
     * 路径变量 {@code {id}}。都取不到说明是列表端点，返回 null。
     */
    private static Long resolveRecordId(HttpServletRequest request, HandlerMethod handlerMethod,
            DataGrant annotation) {
        String idField = annotation.idField();
        String idParameter = request.getParameter(idField);
        if (isPresent(idParameter)) {
            return parseId(idParameter);
        }
        String parameterName = findIdParameterName(handlerMethod.getMethod(), idField);
        if (parameterName != null) {
            String raw = request.getParameter(parameterName);
            if (isPresent(raw)) {
                return parseId(raw);
            }
        }
        String pathVariable = templateVariable(request, idField);
        if (isPresent(pathVariable)) {
            return parseId(pathVariable);
        }
        if (isPagedHandler(handlerMethod.getMethod())) {
            return null;
        }
        throw new DataGrantConfigurationException(
                "无法从请求中解析记录主键: " + handlerMethod.getBeanType().getName() + "#"
                        + handlerMethod.getMethod().getName() + "，请在 @DataGrant 上显式声明 idField");
    }

    /**
     * 读取 URI 模板变量，优先按注解 {@code idField} 取名，模板变量唯一时取该唯一值。
     */
    private static String templateVariable(HttpServletRequest request, String name) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables) || variables.isEmpty()) {
            return null;
        }
        Object value = variables.get(name);
        if (value == null && variables.size() == 1) {
            value = variables.values().iterator().next();
        }
        return value == null ? null : value.toString();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 查找承载记录 ID 的请求参数名：优先注解 {@code idField} 同名参数，其次唯一的数值型参数。
     */
    private static String findIdParameterName(Method method, String idField) {
        String numericCandidate = null;
        int numericCount = 0;
        for (Parameter parameter : method.getParameters()) {
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam == null || !isNumeric(parameter.getType())) {
                continue;
            }
            String name = requestParam.value().isBlank() ? parameter.getName() : requestParam.value();
            if (idField.equals(name)) {
                return name;
            }
            numericCandidate = name;
            numericCount++;
        }
        return numericCount == 1 ? numericCandidate : null;
    }

    private static boolean isNumeric(Class<?> type) {
        return type == long.class || type == int.class || type == short.class
                || Number.class.isAssignableFrom(type);
    }

    private static Long parseId(String raw) {
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException exception) {
            log.debug("记录 ID 参数不是合法数值，交由参数解析处理, value={}", raw);
            return null;
        }
    }

    /**
     * 写出与 {@code AuthFilter} 一致的错误体，并按需标记「需要刷新 Grant」。
     *
     * <p>响应总是带上 {@link DataGrantHeaders#DOMAIN}，前端才能把「该域需刷新」精确落到自己的缓存键上。</p>
     */
    private static boolean reject(HttpServletResponse response, String domain, HttpStatus status, String message,
            boolean grantRequired) throws IOException {
        response.setHeader(DataGrantHeaders.DOMAIN, domain);
        if (grantRequired) {
            response.setHeader(DataGrantHeaders.REQUIRED, "1");
        }
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status.value() + ",\"message\":\"" + escape(message) + "\"}");
        return false;
    }

    private static String escape(String text) {
        StringBuilder builder = new StringBuilder(text.length() + 8);
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * 判断 handler 是否是分页列表端点（返回 {@code R<PageResult<...>>}）。
     *
     * <p>分页端点由 Advice 负责签发，不进拦截器校验；因此取不到记录主键是正常情况。
     * 非分页端点带 {@link io.mango.authorization.api.annotation.DataGrant} 却解析不出主键时属于配置错误，
     * 必须 fail-closed，不能静默放行。</p>
     *
     * @param method handler 方法
     * @return 返回类型为 {@code R<PageResult<...>>} 时返回 true
     */
    private static boolean isPagedHandler(Method method) {
        Type generic = method.getGenericReturnType();
        if (!(generic instanceof ParameterizedType parameterized)) {
            return false;
        }
        if (!R.class.equals(parameterized.getRawType())) {
            return false;
        }
        Type[] arguments = parameterized.getActualTypeArguments();
        if (arguments.length != 1 || !(arguments[0] instanceof ParameterizedType dataType)) {
            return false;
        }
        return PageResult.class.equals(dataType.getRawType());
    }
}
