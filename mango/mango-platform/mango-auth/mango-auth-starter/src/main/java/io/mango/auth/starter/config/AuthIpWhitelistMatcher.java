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

    private static final int IPV6_BYTES = 16;

    private static final int IPV4_MAPPED_OFFSET = 12;

    private static final int IPV4_MAPPED_MARKER_INDEX = 10;

    private static final int BYTE_MASK = 0xFF;

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
     * @return 4 或 16 字节地址；无法解析时抛出异常
     */
    private byte[] addressBytes(String ip) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(ip.trim()).getAddress();
        if (bytes.length == IPV6_BYTES && isIpv4Mapped(bytes)) {
            return Arrays.copyOfRange(bytes, IPV4_MAPPED_OFFSET, IPV6_BYTES);
        }
        return bytes;
    }

    private boolean isIpv4Mapped(byte[] bytes) {
        for (int index = 0; index < IPV4_MAPPED_MARKER_INDEX; index++) {
            if (bytes[index] != 0) {
                return false;
            }
        }
        return (bytes[IPV4_MAPPED_MARKER_INDEX] & BYTE_MASK) == BYTE_MASK
                && (bytes[IPV4_MAPPED_MARKER_INDEX + 1] & BYTE_MASK) == BYTE_MASK;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
