package io.mango.authorization.data.grant;

import io.mango.common.result.Require;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Grant 签发器。
 * <p>
 * 只依赖当前请求上下文（租户、账号）与「本页记录 ID 集合」，不查询数据库、不缓存令牌，
 * 因此可以在任意多实例部署下水平扩展：校验方只需要同一份 {@code secret}。
 * <p>
 * 记录数超过 {@code max-ids} 时**不签发**并返回 {@link DataGrantIssueResult#SKIP_OVERSIZE}，
 * 由响应通知写 {@code X-Mango-Data-Grant-Skipped: oversize}，绝不截断集合后继续签发。
 */
@Slf4j
public class DataGrantIssuer {

    private final DataGrantProperties properties;
    private final DataGrantCodec codec;
    private final ObjectProvider<DataScopeProvider> dataScopeProvider;

    /**
     * 构造签发器。
     *
     * @param properties        Grant 配置。
     * @param codec             编解码器。
     * @param dataScopeProvider 数据范围提供者，可选。
     */
    public DataGrantIssuer(
            DataGrantProperties properties,
            DataGrantCodec codec,
            ObjectProvider<DataScopeProvider> dataScopeProvider) {
        Require.notNull(properties, "Grant 配置不能为空");
        Require.notNull(codec, "Grant 编解码器不能为空");
        Require.notNull(dataScopeProvider, "数据范围提供者包装不能为空");
        this.properties = properties;
        this.codec = codec;
        this.dataScopeProvider = dataScopeProvider;
    }

    /**
     * 为一次列表结果签发 Grant。
     *
     * @param domain    数据权限域标识。
     * @param recordIds 本页记录 ID 集合。
     * @return 签发结果，未签发时带原因。
     */
    public DataGrantIssueResult issue(String domain, Collection<Long> recordIds) {
        Require.notBlank(domain, "数据权限域不能为空");
        Require.notNull(recordIds, "Grant 记录集合不能为空");
        if (!properties.isEnabled()) {
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_DISABLED);
        }
        if (!properties.hasSecret()) {
            log.warn("未配置 mango.data-grant.secret，跳过 Grant 签发, domain={}", domain);
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_NO_SECRET);
        }
        Set<Long> ids = normalize(recordIds);
        if (ids.isEmpty()) {
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_EMPTY);
        }
        if (ids.size() > properties.getMaxIds()) {
            log.warn("Grant 记录数超过上限，跳过签发, domain={}, size={}, maxIds={}",
                    domain, ids.size(), properties.getMaxIds());
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_OVERSIZE);
        }
        String tenantId = MangoContextHolder.tenantId();
        Long memberId = MangoContextHolder.memberId();
        if (tenantId == null || tenantId.isBlank() || memberId == null) {
            log.warn("缺少租户或账号上下文，跳过 Grant 签发, domain={}", domain);
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_NO_CONTEXT);
        }
        long issuedAt = Instant.now().getEpochSecond();
        String permissionVersion;
        try {
            permissionVersion = PermissionVersion.resolve(dataScopeProvider.getIfAvailable(), domain);
        } catch (DataGrantScopeUnresolvedException exception) {
            log.warn("数据范围实现存在但取不到规则，本次不签发 Grant, domain={}", domain, exception);
            return DataGrantIssueResult.skipped(DataGrantIssueResult.SKIP_SCOPE_UNRESOLVED);
        }
        DataGrantPayload payload = new DataGrantPayload(
                tenantId,
                memberId,
                domain,
                ids,
                issuedAt,
                issuedAt + properties.getTtlSeconds(),
                permissionVersion);
        return DataGrantIssueResult.issued(codec.encode(payload));
    }

    private static Set<Long> normalize(Collection<Long> recordIds) {
        Set<Long> ids = new TreeSet<>();
        for (Long recordId : recordIds) {
            if (recordId != null) {
                ids.add(recordId);
            }
        }
        return new LinkedHashSet<>(ids);
    }
}
