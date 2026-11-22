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
 * 多值回传时**不属于当前数据域的令牌直接跳过**：前端会把本地缓存的全部数据域 Grant 一起回传，
 * 如果把别的域的 Grant 当成 INVALID，同一个浏览器里的任意第二个数据域都会让整次详情请求失败。
 * 只有全部令牌都不属于当前数据域时才按「未携带」处理，由调用方决定 optional 放行还是 enforce 拒绝。
 * <p>
 * 同一个数据域内最多接受 4 个令牌，超过即判 INVALID：多值不是「取第一个」的捷径，也不能用来绕过
 * 过期、数据范围指纹等排序。
 * <p>
 * 记录 ID 的判定只读取载荷中的集合，不查询数据库：详情端点必须能在 Controller 之前得到 404，
 * 避免「先查库再判权」把无权记录是否存在泄漏出去。
 */
@Slf4j
public class DataGrantVerifier {

    private final DataGrantCodec codec;
    private final ObjectProvider<DataScopeProvider> dataScopeProvider;

    /** 同域令牌数量上限：超过即判无效，防止多值回传成为绕过 strongest 排序的口子。 */
    private static final int MAX_TOKENS_PER_DOMAIN = 4;

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
        int matching = 0;
        int unattributable = 0;
        int signatureFailures = 0;
        boolean passed = false;
        for (String token : tokens) {
            DataGrantPayload payload;
            try {
                payload = codec.decode(token);
            } catch (DataGrantException exception) {
                // 解码失败时无法判定期望的数据域：可能是其它域的旧令牌（密钥轮换、环境串用），
                // 若直接判 INVALID，无关域的坏令牌会否决一个本域有效令牌的请求。因此只统计并告警，
                // 不参与 strongest 收敛；当全部令牌都不可解码时按「未携带」处理，强制模式下同样拒绝。
                unattributable += 1;
                if (DataGrantCodec.isSignatureFailure(exception)) {
                    signatureFailures += 1;
                }
                log.warn("Grant 解码失败且无法判定数据域，忽略该令牌, domain={}, reason={}", domain,
                        exception.getMessage());
                continue;
            }
            if (!Objects.equals(domain, payload.domain())) {
                log.debug("Grant 数据域不匹配，跳过该令牌, expected={}, actual={}", domain, payload.domain());
                continue;
            }
            matching += 1;
            if (matching > MAX_TOKENS_PER_DOMAIN) {
                // 同域多值不再按 strongest 收敛：超过上限说明携带方异常，直接拒绝，避免多值成为绕过排序的口子。
                log.warn("同域 Grant 数量超过上限, domain={}, count={}, limit={}", domain, matching,
                        MAX_TOKENS_PER_DOMAIN);
                return DataGrantVerification.of(DataGrantVerification.Status.INVALID,
                        DataGrantHeaders.MESSAGE_GRANT_INVALID);
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
                passed = true;
                continue;
            }
            log.warn("记录不在 Grant 集合内, domain={}, recordId={}", domain, recordId);
            best = stronger(best, DataGrantVerification.Status.ID_NOT_FOUND);
        }
        if (unattributable > 0) {
            log.warn("请求携带了无法解码的 Grant 令牌，已按无法判定数据域忽略, domain={}, ignored={}, "
                    + "signatureFailures={}, total={}", domain, unattributable, signatureFailures, tokens.size());
        }
        if (best == DataGrantVerification.Status.INVALID) {
            // 无法确定数据域的坏令牌不能因为另一个令牌通过而被忽略：多值里的坏值必须仍然 fail-closed。
            return DataGrantVerification.of(DataGrantVerification.Status.INVALID,
                    DataGrantHeaders.MESSAGE_GRANT_INVALID);
        }
        if (passed) {
            return DataGrantVerification.passed();
        }
        if (best == null) {
            // 全部令牌都属于其它数据域：等价于「未携带」，不能让别的域的 Grant 变成整次请求失败。
            log.debug("请求携带的 Grant 均不属于当前数据域，按未携带处理, domain={}", domain);
            return DataGrantVerification.of(DataGrantVerification.Status.ABSENT,
                    DataGrantHeaders.MESSAGE_GRANT_REQUIRED);
        }
        return DataGrantVerification.of(best, messageOf(best));
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
            case PASS, INVALID -> DataGrantHeaders.MESSAGE_GRANT_INVALID;
            case ABSENT -> DataGrantHeaders.MESSAGE_GRANT_REQUIRED;
        };
    }
}
