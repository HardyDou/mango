package io.mango.authorization.data.grant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.authorization.data.grant.DataGrantProperties.Mode;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * 数据权限 Grant 测试夹具。
 *
 * <p>统一构造配置、编解码器、数据范围提供者与上下文快照，避免各测试类重复样板代码。</p>
 */
final class GrantTestSupport {

    /** 测试密钥。 */
    static final String SECRET = "unit-test-secret-977";

    /** 测试数据域。 */
    static final String DOMAIN = "fixture:item";

    /** 测试租户。 */
    static final String TENANT_ID = "1";

    /** 测试账号 memberId。 */
    static final Long MEMBER_ID = 10023L;

    /** 测试签发时间（秒）。 */
    static final long NOW = 1_700_000_000L;

    private GrantTestSupport() {
    }

    /**
     * 默认配置：optional 模式、最大 100 条 ID。
     *
     * @return 配置
     */
    static DataGrantProperties properties() {
        return properties(Mode.OPTIONAL, 100);
    }

    /**
     * 构造配置并执行前置校验。
     *
     * @param mode 模式
     * @param maxIds 最大 ID 数量
     * @return 配置
     */
    static DataGrantProperties properties(Mode mode, int maxIds) {
        DataGrantProperties properties = new DataGrantProperties();
        properties.setSecret(SECRET);
        properties.setMode(mode);
        properties.setMaxIds(maxIds);
        properties.setTtlSeconds(300);
        properties.validate();
        return properties;
    }

    /**
     * 构造编解码器。
     *
     * @param properties 配置
     * @return 编解码器
     */
    static DataGrantCodec codec(DataGrantProperties properties) {
        return new DataGrantCodec(properties, new ObjectMapper());
    }

    /**
     * 构造数据范围提供者包装，provider 为空时表示容器中没有该 Bean。
     *
     * @param provider 数据范围提供者，可为 null
     * @return ObjectProvider
     */
    static ObjectProvider<DataScopeProvider> provider(DataScopeProvider provider) {
        return providerOf(DataScopeProvider.class, provider);
    }

    /**
     * 构造指定类型的 ObjectProvider 包装，bean 为空时表示容器中没有该 Bean。
     *
     * @param type bean 类型
     * @param bean bean 实例，可为 null
     * @param <T> bean 类型
     * @return ObjectProvider
     */
    static <T> ObjectProvider<T> providerOf(Class<T> type, T bean) {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        if (bean != null) {
            factory.registerSingleton(type.getName(), bean);
        }
        return factory.getBeanProvider(type);
    }

    /**
     * 写入当前请求上下文。
     *
     * @param tenantId 租户
     * @param memberId 账号
     */
    static void context(String tenantId, Long memberId) {
        MangoContextHolder.set(new MangoContextSnapshot(null, null, tenantId, 1L, memberId, "tester", null,
                null, null, null, null, null));
    }

    /**
     * 清理请求上下文。
     */
    static void clearContext() {
        MangoContextHolder.clear();
    }

    /**
     * 构造 Grant 令牌。
     *
     * @param codec 编解码器
     * @param tenantId 租户
     * @param memberId 账号
     * @param domain 数据域
     * @param ids 记录 ID 集合
     * @param issuedAt 签发时间（秒）
     * @param expiresAt 过期时间（秒）
     * @param permissionVersion 权限版本指纹，可为 null
     * @return 令牌
     */
    static String token(DataGrantCodec codec, String tenantId, Long memberId, String domain, Set<Long> ids,
            long issuedAt, long expiresAt, String permissionVersion) {
        return codec.encode(new DataGrantPayload(tenantId, memberId, domain, ids, issuedAt, expiresAt,
                permissionVersion));
    }

    /**
     * 固定数据范围规则，用于 pv 指纹断言。
     *
     * @param values 数据范围值
     * @param selfIncluded 是否包含本人数据
     * @return 规则
     */
    static DataScopeRule rule(Set<String> values, boolean selfIncluded) {
        return new DataScopeRule(DataScopeRule.Mode.CUSTOM, values, selfIncluded);
    }
}
