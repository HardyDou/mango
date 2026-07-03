package io.mango.home.core.service.impl;

final class HomeRouteKeys {

    private static final String TEMPLATE_PREFIX = "template:";

    private HomeRouteKeys() {
    }

    static String user(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    static String template(Long id) {
        return id == null ? null : TEMPLATE_PREFIX + id;
    }

    static boolean isTemplate(String routeKey) {
        return routeKey != null && routeKey.startsWith(TEMPLATE_PREFIX);
    }

    static Long parseTemplateId(String routeKey) {
        if (!isTemplate(routeKey)) {
            return null;
        }
        return parseLong(routeKey.substring(TEMPLATE_PREFIX.length()));
    }

    static Long parseUserPageId(String routeKey) {
        if (routeKey == null || routeKey.isBlank() || isTemplate(routeKey)) {
            return null;
        }
        return parseLong(routeKey);
    }

    private static Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
