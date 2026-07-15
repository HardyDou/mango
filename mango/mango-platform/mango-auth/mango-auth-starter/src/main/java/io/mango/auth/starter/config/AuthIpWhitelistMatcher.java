package io.mango.auth.starter.config;

import org.springframework.util.AntPathMatcher;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
        try {
            String normalizedIp = normalizeLoopback(clientIp);
            String normalizedCidr = cidr.trim();
            if (!normalizedCidr.contains("/")) {
                return InetAddress.getByName(normalizedCidr).equals(InetAddress.getByName(normalizedIp));
            }
            String[] parts = normalizedCidr.split("/", 2);
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();
            byte[] clientBytes = InetAddress.getByName(normalizedIp).getAddress();
            int prefix = Integer.parseInt(parts[1]);
            int bits = networkBytes.length * Byte.SIZE;
            if (networkBytes.length != clientBytes.length || prefix < 0 || prefix > bits) {
                return false;
            }
            BigInteger mask = subnetMask(prefix, bits);
            return new BigInteger(1, networkBytes).and(mask).equals(new BigInteger(1, clientBytes).and(mask));
        } catch (UnknownHostException | NumberFormatException exception) {
            return false;
        }
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
