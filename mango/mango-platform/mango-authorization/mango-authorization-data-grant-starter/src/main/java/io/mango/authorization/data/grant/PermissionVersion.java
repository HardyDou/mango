package io.mango.authorization.data.grant;

import io.mango.infra.persistence.api.scope.DataScopeProvider;
import io.mango.infra.persistence.api.scope.DataScopeRule;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据范围指纹（{@code pv}）计算。
 * <p>
 * 指纹只覆盖「角色数据范围配置」本身（模式、排序后的范围值、是否含本人），因此能挡住
 * 「管理员改了角色数据范围但用户手里的 Grant 还没过期」；挡不住「用户调岗」与「记录改归属」，
 * 这两类只能靠 Grant 的 TTL（默认 300 秒）收敛。
 * <p>
 * {@link DataScopeProvider} 在本模块是可选依赖：业务应用没有该实现时指纹为 null，载荷不写 {@code pv}。
 */
@Slf4j
final class PermissionVersion {

    private static final int FINGERPRINT_LENGTH = 8;

    private PermissionVersion() {
    }

    /**
     * 解析数据域当前的数据范围指纹。
     *
     * @param provider 数据范围提供者，可为 null。
     * @param domain   数据权限域标识。
     * @return 指纹；提供者缺失、无规则或解析异常时返回 null。
     */
    static String resolve(DataScopeProvider provider, String domain) {
        if (provider == null) {
            return null;
        }
        Optional<DataScopeRule> rule;
        try {
            rule = provider.resolve(domain);
        } catch (RuntimeException exception) {
            log.warn("数据范围指纹解析失败，本次不写入 pv, domain={}", domain, exception);
            return null;
        }
        if (rule == null || rule.isEmpty()) {
            return null;
        }
        return fingerprint(rule.get());
    }

    /**
     * 计算数据范围规则指纹。
     *
     * @param rule 数据范围规则。
     * @return 指纹（sha256 前 8 位十六进制）。
     */
    static String fingerprint(DataScopeRule rule) {
        String values = rule.values() == null
                ? ""
                : rule.values().stream().sorted().collect(Collectors.joining(","));
        String material = rule.mode() + "|" + values + "|" + rule.selfIncluded();
        return sha256Hex(material).substring(0, FINGERPRINT_LENGTH);
    }

    private static String sha256Hex(String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Failed to calculate data scope fingerprint", exception);
        }
    }
}
