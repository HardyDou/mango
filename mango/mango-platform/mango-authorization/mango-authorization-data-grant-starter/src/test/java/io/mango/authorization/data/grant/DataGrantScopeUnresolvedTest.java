package io.mango.authorization.data.grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 数据范围指纹失败语义与未签发原因不变量测试。
 *
 * <p>「没有数据范围实现」与「有实现但取不到规则」必须区分：前者的指纹为空、照常签发；后者无法证明数据范围，
 * 必须按未签发处理并对外可见，不能静默留空。</p>
 */
class DataGrantScopeUnresolvedTest {

    /** 会对外暴露 {@code X-Mango-Data-Grant-Skipped} 的未签发原因。 */
    private static final Set<String> EXPOSED_SKIP_REASONS = Set.of(
            DataGrantIssueResult.SKIP_OVERSIZE, DataGrantIssueResult.SKIP_SCOPE_UNRESOLVED);

    /** 故意不对外暴露的未签发原因：属于部署或缺省配置，暴露细节会被前端当成数据问题。 */
    private static final Set<String> SILENT_SKIP_REASONS = Set.of(
            DataGrantIssueResult.SKIP_DISABLED,
            DataGrantIssueResult.SKIP_NO_SECRET,
            DataGrantIssueResult.SKIP_NO_CONTEXT,
            DataGrantIssueResult.SKIP_EMPTY);

    private DataGrantProperties properties;

    private DataGrantCodec codec;

    @BeforeEach
    void setUp() {
        properties = GrantTestSupport.properties();
        codec = GrantTestSupport.codec(properties);
        GrantTestSupport.context(GrantTestSupport.TENANT_ID, GrantTestSupport.MEMBER_ID);
    }

    @AfterEach
    void tearDown() {
        GrantTestSupport.clearContext();
    }

    @Test
    void everySkipReasonIsClassifiedAsExposedOrSilent() throws Exception {
        List<String> reasons = declaredSkipReasons();

        assertThat(reasons).isNotEmpty();
        for (String reason : reasons) {
            assertThat(EXPOSED_SKIP_REASONS.contains(reason) || SILENT_SKIP_REASONS.contains(reason))
                    .as("未签发原因 %s 必须显式归入「对外可见」或「静默」两套之一，新增分支时不要漏判", reason)
                    .isTrue();
        }
    }

    @Test
    void providerFailureFailsClosedInsteadOfLeavingFingerprintEmpty() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(failingProvider()));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(result.isIssued()).isFalse();
        assertThat(result.token()).isNull();
        assertThat(result.skipReason()).isEqualTo(DataGrantIssueResult.SKIP_SCOPE_UNRESOLVED);
    }

    @Test
    void providerFailureRaisesDedicatedException() {
        assertThatThrownBy(() -> PermissionVersion.resolve(failingProvider(), GrantTestSupport.DOMAIN))
                .isInstanceOf(DataGrantScopeUnresolvedException.class)
                .hasMessageContaining(GrantTestSupport.DOMAIN);
    }

    @Test
    void missingProviderStillIssuesWithoutFingerprint() {
        DataGrantIssuer issuer = new DataGrantIssuer(properties, codec, GrantTestSupport.provider(null));

        DataGrantIssueResult result = issuer.issue(GrantTestSupport.DOMAIN, Set.of(1001L));

        assertThat(result.isIssued()).isTrue();
        assertThat(codec.decode(result.token()).permissionVersion()).isNull();
    }

    private static DataScopeProvider failingProvider() {
        return resourceCode -> {
            throw new IllegalStateException("数据范围服务不可用");
        };
    }

    private static List<String> declaredSkipReasons() throws Exception {
        List<String> reasons = new java.util.ArrayList<>();
        for (Field field : DataGrantIssueResult.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && field.getType() == String.class && field.getName().startsWith("SKIP_")) {
                field.setAccessible(true);
                reasons.add((String) field.get(null));
            }
        }
        return reasons;
    }
}
