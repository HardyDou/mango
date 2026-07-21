package io.mango.notice.support.context;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.notice.api.command.NoticeSendEventCommand;

import java.util.Objects;

/**
 * Restores the application context captured by a notice event for local and remote delivery.
 */
public final class NoticeEventContextExecutor {

    private NoticeEventContextExecutor() {
    }

    public static void run(NoticeSendEventCommand event, Runnable action) {
        Objects.requireNonNull(event, "notice event must not be null");
        Objects.requireNonNull(action, "notice event action must not be null");
        String tenantId = firstText(event.getTenantId(), MangoContextHolder.tenantId());
        if (tenantId == null) {
            throw new IllegalArgumentException("notice event tenantId must not be blank");
        }
        String appCode = firstText(event.getAppCode(), MangoContextHolder.appCode());
        String realm = firstText(event.getRealm(), MangoContextHolder.get().realm());
        event.setTenantId(tenantId);
        event.setAppCode(appCode);
        event.setRealm(realm);
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withSecurity(
                    null, null, tenantId, null, realm, null, null, null, appCode));
            action.run();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private static String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }
}
