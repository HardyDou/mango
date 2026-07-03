package io.mango.link.core.support;

import io.mango.common.result.Require;
import io.mango.link.api.enums.LinkCategoryScope;
import io.mango.link.api.enums.LinkOpenMode;
import io.mango.link.api.enums.LinkStatus;
import io.mango.link.api.enums.LinkVisibilityScope;
import io.mango.link.api.enums.LinkVisibilityTargetType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public final class LinkSupport {

    private LinkSupport() {
    }

    public static String status(LinkStatus status) {
        return (status == null ? LinkStatus.ENABLED : status).name();
    }

    public static String enabled() {
        return LinkStatus.ENABLED.name();
    }

    public static String companyCategory() {
        return LinkCategoryScope.COMPANY.name();
    }

    public static String personalCategory() {
        return LinkCategoryScope.PERSONAL.name();
    }

    public static String disabled() {
        return LinkStatus.DISABLED.name();
    }

    public static String newWindow() {
        return LinkOpenMode.NEW_WINDOW.name();
    }

    public static String scope(LinkVisibilityScope scope) {
        Require.notNull(scope, "可见范围不能为空");
        return scope.name();
    }

    public static LinkStatus toStatus(String status) {
        return status == null ? null : LinkStatus.valueOf(status);
    }

    public static LinkCategoryScope toCategoryScope(String scope) {
        return scope == null ? null : LinkCategoryScope.valueOf(scope);
    }

    public static LinkVisibilityScope toScope(String scope) {
        return scope == null ? null : LinkVisibilityScope.valueOf(scope);
    }

    public static LinkOpenMode toOpenMode(String openMode) {
        return openMode == null ? null : LinkOpenMode.valueOf(openMode);
    }

    public static LinkVisibilityTargetType toTargetType(String targetType) {
        return targetType == null ? null : LinkVisibilityTargetType.valueOf(targetType);
    }

    public static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<String> normalized = tags.stream()
                .map(LinkContextSupport::trimToNull)
                .filter(tag -> tag != null)
                .peek(tag -> Require.isTrue(!tag.contains(","), "标签不能包含英文逗号"))
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    public static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }

    public static String normalizeUrl(String url) {
        String normalized = LinkContextSupport.trimRequired(url, "网址地址不能为空");
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            Require.isTrue(("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null, "请输入正确的网址地址");
            return normalized;
        } catch (URISyntaxException e) {
            return Require.fail(400, "请输入正确的网址地址");
        }
    }
}
