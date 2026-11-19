package io.mango.auth.starter.config;

import org.springframework.util.AntPathMatcher;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤链的来源 IP 白名单协议适配器。
 */
final class AuthIpWhitelistMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    boolean matches(AuthAccessProperties.IpWhitelist whitelist, String method, String path, String clientIp) {
        if (whitelist == null || !whitelist.isEnabled()) {
            return false;
        }
        if (blank(clientIp)) {
            return false;
        }
        List<AuthAccessProperties.Rule> rules = whitelist.getRules();
        return rules != null && rules.stream().anyMatch(rule -> matchesRule(rule, method, path, clientIp));
    }

    private boolean matchesRule(AuthAccessProperties.Rule rule, String method, String path, String clientIp) {
        if (rule == null || blank(rule.getPathPattern())) {
            return false;
        }
        if (!matchesMethod(rule.getMethods(), method)
                || !pathMatcher.match(rule.getPathPattern(), path)) {
            return false;
        }
        List<String> cidrs = rule.getCidrs();
        return cidrs != null && cidrs.stream().anyMatch(cidr -> matchesCidr(cidr, clientIp));
    }

    private boolean matchesMethod(List<String> methods, String method) {
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        return methods.stream().anyMatch(value -> "ALL".equalsIgnoreCase(value)
                || value.equalsIgnoreCase(method));
    }

    private boolean matchesCidr(String cidr, String clientIp) {
        if (blank(cidr)) {
            return false;
        }
        String trimmedCidr = cidr.trim();
        try {
            if (!trimmedCidr.contains("/")) {
                byte[] ruleBytes = addressBytes(trimmedCidr);
                byte[] clientBytes = addressBytes(clientIp.trim());
                return ruleBytes != null && clientBytes != null && Arrays.equals(ruleBytes, clientBytes);
            }
            String[] parts = trimmedCidr.split("/", 2);
            byte[] networkBytes = addressBytes(parts[0]);
            byte[] clientBytes = addressBytes(clientIp.trim());
            if (networkBytes == null || clientBytes == null || networkBytes.length != clientBytes.length) {
                return false;
            }
            int prefixLength = Integer.parseInt(parts[1].trim());
            int maxBits = networkBytes.length * Byte.SIZE;
            if (prefixLength < 0 || prefixLength > maxBits) {
                return false;
            }
            BigInteger network = new BigInteger(1, networkBytes);
            BigInteger client = new BigInteger(1, clientBytes);
            BigInteger mask = prefixLength == 0
                    ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(maxBits).subtract(BigInteger.ONE)
                    .shiftRight(maxBits - prefixLength)
                    .shiftLeft(maxBits - prefixLength);
            return network.and(mask).equals(client.and(mask));
        } catch (UnknownHostException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析地址字节，并把 IPv4 映射的 IPv6 地址（{@code ::ffff:127.0.0.1}）归一为 IPv4，
     * 避免双栈容器里同一来源因地址族不同而与 IPv4 规则失配。
     *
     * @param ip 地址或域名
     * @return 4 或 16 字节地址；无法解析时返回 null
     */
    private byte[] addressBytes(String ip) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(normalizeLoopback(ip)).getAddress();
        if (bytes.length == 16 && isIpv4Mapped(bytes)) {
            return Arrays.copyOfRange(bytes, 12, 16);
        }
        return bytes;
    }

    private boolean isIpv4Mapped(byte[] bytes) {
        for (int index = 0; index < 10; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }

    private String normalizeLoopback(String clientIp) {
        String normalizedIp = clientIp.trim();
        if ("0:0:0:0:0:0:0:1".equals(normalizedIp)) {
            return "::1";
        }
        return normalizedIp;
    }

    private BigInteger subnetMask(int prefix, int bits) {
        if (prefix == 0) {
            return BigInteger.ZERO;
        }
        return BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE)
                .shiftRight(bits - prefix).shiftLeft(bits - prefix);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
