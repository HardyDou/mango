package io.mango.common.credential;

import java.util.Locale;

/**
 * 详情访问入口标识的规范化工具。
 *
 * <p>入口标识统一为「方法码 + 冒号 + 路径」，例如 {@code G:/cms/open/contents/detail}。
 * 方法码只有一位：G/P/U/D/H/O。票据中使用 {@code ~} 分隔多个入口，
 * 因此路径中不允许出现 {@code ~} 和 {@code .}。
 *
 * @author Mango
 */
public final class DetailTicketEndpoints {

    private static final char SEPARATOR = ':';
    private static final String FORBIDDEN = "~.";

    private DetailTicketEndpoints() {
    }

    /**
     * 规范化入口标识。
     *
     * @param httpMethod HTTP 方法，例如 GET
     * @param path 路径或路径模式
     * @return 规范化后的入口标识
     */
    public static String key(String httpMethod, String path) {
        if (httpMethod == null || httpMethod.isBlank()) {
            throw new IllegalArgumentException("接口方法不能为空");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("接口路径不能为空");
        }
        String normalizedPath = path.trim();
        for (int index = 0; index < FORBIDDEN.length(); index++) {
            char forbidden = FORBIDDEN.charAt(index);
            if (normalizedPath.indexOf(forbidden) >= 0) {
                throw new IllegalArgumentException("接口路径不能包含字符 " + forbidden + ": " + normalizedPath);
            }
        }
        return String.valueOf(methodCode(httpMethod)) + SEPARATOR + normalizedPath;
    }

    /**
     * 解析声明值。
     *
     * @param declaration 形如 {@code GET /cms/open/contents/detail}
     * @return 规范化后的入口标识
     */
    public static String fromDeclaration(String declaration) {
        if (declaration == null) {
            throw new IllegalArgumentException("入口声明不能为空");
        }
        String trimmed = declaration.trim();
        int space = trimmed.indexOf(' ');
        if (space <= 0 || space == trimmed.length() - 1) {
            throw new IllegalArgumentException("入口声明必须为「方法 路径」格式: " + declaration);
        }
        return key(trimmed.substring(0, space), trimmed.substring(space + 1).trim());
    }

    private static char methodCode(String httpMethod) {
        String upper = httpMethod.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "GET" -> 'G';
            case "POST" -> 'P';
            case "PUT" -> 'U';
            case "DELETE" -> 'D';
            case "HEAD" -> 'H';
            case "OPTIONS" -> 'O';
            default -> throw new IllegalArgumentException("不支持的接口方法: " + httpMethod);
        };
    }
}
