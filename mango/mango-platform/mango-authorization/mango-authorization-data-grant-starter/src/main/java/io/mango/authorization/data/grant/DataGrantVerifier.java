package io.mango.authorization.data.grant;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Grant 校验器。
 * <p>
 * 校验顺序固定为「签名与格式 → 数据域 → 租户 → 账号 → 过期 → 数据范围指纹 → 记录 ID」，
 * 任一环节失败立即给出对应状态，不继续做更宽松的匹配；多个 Grant 头中同一数据域的任一令牌通过即视为通过，
 * 以便列表页同时展示多个数据域时前端只回传相关令牌。
 * <p>
 * 记录 ID 的判定只读取载荷中的集合，不查询数据库：详情端点必须能在 Controller 之前得到 404，
 * 避免「先查库再判权」把无权记录是否存在泄漏出去。
 */
@Slf4j
public class DataGrantVerifier {

    private final DataGrantCodec codec;
    private final ObjectProvider<DataScopeProvider> dataScopeProvider;

    /**
     * 构造校验器。
     *
     * @param codec             编解码器。
     * @param dataScopeProvider 数据范围提供者，可选。
     */
    public DataGrantVerifier(DataGrantCodec codec, ObjectProvider<DataScopeProvider> dataScopeProvider) {
        Require.notNull(codec, "Grant 编解码器不能为空");
        Require.notNull(dataScopeProvider, "数据范围提供者包装不能为空");
        this.codec = codec;
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 校验 Grant 是否允许访问指定记录。
     *
     * @param tokens   请求携带的 Grant 令牌列表，可为空。
     * @param domain   当前端点的数据权限域。
     * @param recordId 请求访问的记录 ID，为 null 时只校验令牌本身（不校验集合）。
     * @return 校验结果。
     */
    public DataGrantVerification verify(List<String> tokens, String domain, Long recordId) {
        Require.notBlank(domain, "数据权限域不能为空");
        if (tokens == null || tokens.isEmpty()) {
            return DataGrantVerification.of(DataGrantVerification.Status.ABSENT, DataGrantHeaders.MESSAGE_GRANT_REQUIRED);
        }
        DataGrantVerification.Status best = null;
        for (String token : tokens) {
            DataGrantPayload payload;
            try {
                payload = codec.decode(token);
            } catch (DataGrantException exception) {
                log.warn("Grant 解码失败, domain={}, reason={}", domain, exception.getMessage());
                best = stronger(best, DataGrantVerification.Status.INVALID);
                continue;
            }
            if (!Objects.equals(domain, payload.domain())) {
                log.warn("Grant 数据域不匹配, expected={}, actual={}", domain, payload.domain());
                best = stronger(best, DataGrantVerification.Status.INVALID);
                continue;
            }
            if (!Objects.equals(payload.tenantId(), MangoContextHolder.tenantId())) {
                log.warn("Grant 租户不匹配, domain={}, grantTenant={}", domain, payload.tenantId());
                best = stronger(best, DataGrantVerification.Status.INVALID);
                continue;
            }
            if (!Objects.equals(payload.memberId(), MangoContextHolder.memberId())) {
                log.warn("Grant 账号不匹配, domain={}, grantMember={}", domain, payload.memberId());
                best = stronger(best, DataGrantVerification.Status.INVALID);
                continue;
            }
            if (payload.expiresAt() <= Instant.now().getEpochSecond()) {
                log.warn("Grant 已过期, domain={}, expiresAt={}", domain, payload.expiresAt());
                best = stronger(best, DataGrantVerification.Status.EXPIRED);
                continue;
            }
            if (!permissionVersionMatches(payload, domain)) {
                log.warn("Grant 数据范围已变更, domain={}", domain);
                best = stronger(best, DataGrantVerification.Status.SCOPE_CHANGED);
                continue;
            }
            if (recordId == null || payload.containsId(recordId)) {
                return DataGrantVerification.passed();
            }
            log.warn("记录不在 Grant 集合内, domain={}, recordId={}", domain, recordId);
            best = stronger(best, DataGrantVerification.Status.ID_NOT_FOUND);
        }
        DataGrantVerification.Status status = best == null ? DataGrantVerification.Status.INVALID : best;
        return DataGrantVerification.of(status, messageOf(status));
    }

    private boolean permissionVersionMatches(DataGrantPayload payload, String domain) {
        String expected = payload.permissionVersion();
        if (expected == null) {
            return true;
        }
        String current = PermissionVersion.resolve(dataScopeProvider.getIfAvailable(), domain);
        return current == null || current.equals(expected);
    }

    private static DataGrantVerification.Status stronger(
            DataGrantVerification.Status current,
            DataGrantVerification.Status candidate) {
        if (current == null) {
            return candidate;
        }
        return rank(candidate) > rank(current) ? candidate : current;
    }

    private static int rank(DataGrantVerification.Status status) {
        return switch (status) {
            case INVALID -> 4;
            case EXPIRED -> 3;
            case SCOPE_CHANGED -> 2;
            case ID_NOT_FOUND -> 1;
            case PASS, ABSENT -> 0;
        };
    }

    private static String messageOf(DataGrantVerification.Status status) {
        return switch (status) {
            case EXPIRED -> DataGrantHeaders.MESSAGE_GRANT_EXPIRED;
            case SCOPE_CHANGED -> DataGrantHeaders.MESSAGE_GRANT_SCOPE_CHANGED;
            case ID_NOT_FOUND -> DataGrantHeaders.MESSAGE_RESOURCE_NOT_FOUND;
            case PASS, ABSENT, INVALID -> DataGrantHeaders.MESSAGE_GRANT_INVALID;
        };
    }
}
