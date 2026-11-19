package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 来源 IP 白名单配置自检。
 * <p>
 * 白名单是「放行」条件，配置写错时请求只会持续返回 401，没有其它线索。这里在启动期检查
 * 明显不可能命中的 CIDR：{@code 10/8}、{@code 172.16/12}、{@code 192.168/16} 这类缩写会被
 * {@link InetAddress#getByName(String)} 解析成 {@code 0.0.0.10}、{@code 172.0.0.16}、
 * {@code 192.0.0.168}，网段随之变成 {@code 0.0.0.0/8}、{@code 172.0.0.0/12}、
 * {@code 192.0.0.0/16}，与预期网段不符且不会报错。
 * <p>
 * 只告警不改配置：老部署可能同时保留多条规则，其中一条写错不影响其它规则生效。
 *
 * @author Mango
 */
@Slf4j
public class IpWhitelistDiagnostics {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");

    /**
     * 检查白名单规则并输出告警。
     *
     * @param whitelist 白名单配置，允许为空
     */
    public void inspect(AccessProperties.IpWhitelist whitelist) {
        if (whitelist == null || whitelist.getRules() == null) {
            return;
        }
        for (AccessProperties.Rule rule : whitelist.getRules()) {
            inspectRule(rule);
        }
    }

    private void inspectRule(AccessProperties.Rule rule) {
        if (rule == null) {
            return;
        }
        List<String> cidrs = rule.getCidrs();
        if (cidrs == null) {
            return;
        }
        for (String cidr : cidrs) {
            if (!isSuspicious(cidr)) {
                continue;
            }
            log.warn("IP 白名单 CIDR 不是完整网段写法，该条规则不会按预期网段命中: pathPattern={}, cidr={}, "
                    + "示例=10.0.0.0/8", rule.getPathPattern(), cidr);
        }
    }

    /**
     * 判断 CIDR 是否不可能按预期网段命中。
     * <p>
     * IPv4 必须是四段点分写法，IPv6 必须能被解析；前缀长度必须落在协议允许范围内。
     *
     * @param cidr 配置值
     * @return true 表示该值不会被解析成预期网段
     */
    public boolean isSuspicious(String cidr) {
        if (!StringUtils.hasText(cidr)) {
            return false;
        }
        String value = cidr.trim();
        int slashIndex = value.indexOf('/');
        String network = (slashIndex < 0 ? value : value.substring(0, slashIndex)).trim();
        if (network.isEmpty()) {
            return true;
        }
        if (network.contains(":")) {
            return isSuspiciousAddress(network, 16) || isSuspiciousMask(value, slashIndex, 128);
        }
        if (!IPV4_PATTERN.matcher(network).matches()) {
            return true;
        }
        return isSuspiciousAddress(network, 4) || isSuspiciousMask(value, slashIndex, 32);
    }

    private boolean isSuspiciousAddress(String network, int expectedLength) {
        try {
            return InetAddress.getByName(network).getAddress().length != expectedLength;
        } catch (UnknownHostException exception) {
            return true;
        }
    }

    private boolean isSuspiciousMask(String value, int slashIndex, int maxMask) {
        if (slashIndex < 0) {
            return false;
        }
        String mask = value.substring(slashIndex + 1).trim();
        if (!mask.matches("\\d{1,3}")) {
            return true;
        }
        int bits = Integer.parseInt(mask);
        return bits > maxMask;
    }
}
