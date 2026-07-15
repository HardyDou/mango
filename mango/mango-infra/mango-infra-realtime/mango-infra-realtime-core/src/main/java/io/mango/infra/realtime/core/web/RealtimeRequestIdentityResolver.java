package io.mango.infra.realtime.core.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves realtime request identity without allowing client input to override an authenticated principal.
 */
public final class RealtimeRequestIdentityResolver {

    public static final String TENANT_ID_ATTRIBUTE = "tenantId";
    public static final String USER_ID_ATTRIBUTE = "userId";

    private RealtimeRequestIdentityResolver() {
    }

    public static String resolveTenantId(HttpServletRequest request, String... fallbacks) {
        Object authenticatedTenantId = requestAttribute(request, TENANT_ID_ATTRIBUTE);
        if (authenticatedTenantId != null) {
            return normalizeTenantId(String.valueOf(authenticatedTenantId));
        }
        return normalizeTenantId(firstText(fallbacks));
    }

    public static String resolveTenantId(String... fallbacks) {
        return resolveTenantId(currentRequest(), fallbacks);
    }

    public static Long resolveUserId(HttpServletRequest request, Object... fallbacks) {
        Object authenticatedUserId = requestAttribute(request, USER_ID_ATTRIBUTE);
        if (authenticatedUserId != null) {
            return parseLong(authenticatedUserId);
        }
        for (Object fallback : fallbacks) {
            Long parsed = parseLong(fallback);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    public static Long resolveUserId(Object... fallbacks) {
        return resolveUserId(currentRequest(), fallbacks);
    }

    public static String currentRequestParameter(String name) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return request.getParameter(name);
    }

    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "default";
        }
        return tenantId.trim();
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Object requestAttribute(HttpServletRequest request, String name) {
        if (request == null) {
            return null;
        }
        return request.getAttribute(name);
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
