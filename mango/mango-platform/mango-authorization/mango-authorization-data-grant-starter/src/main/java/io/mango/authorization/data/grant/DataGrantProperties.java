package io.mango.authorization.data.grant;

import io.mango.common.result.Require;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 无状态数据权限 Grant 配置。
 * <p>
 * <p><strong>当前状态：半成品，默认关闭。</strong>完整能力（刷新/直链进入详情页后数据权限可恢复）尚未完成，
 * 详见 issue #1420；在完成前禁止在生产环境启用。默认 {@code enabled=false}，需要显式配置才生效。</p>
 *
 * <p>安全默认值：未配置 {@code secret} 时 {@code optional} 模式不签发也不校验 Grant（只记 warn），
 * {@code enforce} 模式则拒绝启动，避免出现「以为开启了强校验、实际所有请求都被放行」的静默降级。</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = DataGrantProperties.PREFIX)
public class DataGrantProperties {

    /** 配置前缀。 */
    public static final String PREFIX = "mango.data-grant";

    /** 是否启用 Grant 签发与校验，默认关闭（能力未完成，禁止生产启用）。 */
    private boolean enabled = false;

    /** 校验模式，默认 {@link Mode#OPTIONAL}。 */
    private Mode mode = Mode.OPTIONAL;

    /**
     * HMAC-SHA256 共享密钥，必须由部署环境通过配置中心或环境变量注入，禁止写入版本控制。
     */
    private String secret;

    /** 签名密钥标识，用于后续密钥轮换，默认 {@code k1}。 */
    private String keyId = "k1";

    /** Grant 有效期（秒），默认 300 秒；过期后必须重新拉取列表。 */
    private long ttlSeconds = 300;

    /** Grant 可承载的最大记录 ID 数量，默认 100；超过时不签发。 */
    private int maxIds = 100;

    /**
     * 校验配置合法性。
     * <p>
     * 在 Bean 初始化阶段执行，配置非法时直接启动失败，而不是运行期退化为放行。
     */
    @PostConstruct
    public void validate() {
        Require.isTrue(ttlSeconds > 0, "mango.data-grant.ttl-seconds 必须大于 0");
        Require.isTrue(maxIds > 0, "mango.data-grant.max-ids 必须大于 0");
        Require.notBlank(keyId, "mango.data-grant.key-id 不能为空");
        Require.notNull(mode, "mango.data-grant.mode 不能为空");
        Require.isTrue(
                hasSecret() || mode == Mode.OPTIONAL,
                "mango.data-grant.secret 在 enforce 模式下必须配置，否则无法签发与校验 Grant");
    }

    /**
     * 是否已配置签名密钥。
     *
     * @return 密钥非空白时返回 true。
     */
    public boolean hasSecret() {
        return secret != null && !secret.isBlank();
    }

    /**
     * Grant 校验模式。
     */
    public enum Mode {

        /** 未携带 Grant 时保持原有行为，仅在响应头标记 {@code X-Mango-Data-Grant: absent}。 */
        OPTIONAL,

        /** 未携带 Grant 时直接拒绝，响应头标记 {@code X-Mango-Data-Grant-Required: 1}。 */
        ENFORCE
    }
}
