package io.mango.cms.core.service.impl;

import io.mango.cms.api.enums.CmsStatus;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

final class CmsSupport {

    static final String ENABLED = CmsStatus.ENABLED.name();
    static final String DISABLED = CmsStatus.DISABLED.name();
    static final long ROOT_PARENT_ID = 0L;

    private CmsSupport() {
    }

    static String currentTenantId() {
        String tenantId = MangoContextHolder.tenantId();
        Require.notBlank(tenantId, "租户上下文不能为空");
        return tenantId;
    }

    static String currentTenantIdOrNull() {
        return trimToNull(MangoContextHolder.tenantId());
    }

    static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    static String trimRequired(String value, String message) {
        Require.notBlank(value, message);
        return value.trim();
    }

    static String defaultStatus(String status) {
        String value = trimToNull(status);
        return value == null ? ENABLED : value;
    }

    static Integer defaultSort(Integer sort) {
        return sort == null ? 0 : sort;
    }

    static Long defaultParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    static boolean isEffective(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        return (startTime == null || !startTime.isAfter(now))
                && (endTime == null || endTime.isAfter(now));
    }

    static <E extends Enum<E>> String enumName(Class<E> enumType, String value, String message) {
        String resolved = trimRequired(value, message);
        try {
            return Enum.valueOf(enumType, resolved).name();
        } catch (IllegalArgumentException ex) {
            throw new BizException(message);
        }
    }
}
