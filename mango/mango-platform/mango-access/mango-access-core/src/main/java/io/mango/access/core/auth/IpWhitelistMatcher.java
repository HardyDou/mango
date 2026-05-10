package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 来源 IP 白名单匹配器。
 *
 * @author Mango
 */
public class IpWhitelistMatcher {

    private static final String ALL_METHODS = "ALL";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean matches(AccessProperties.IpWhitelist whitelist,
                           String httpMethod,
                           String path,
                           String clientIp) {
        if (whitelist == null || !whitelist.isEnabled()) {
            return false;
        }
        if (!StringUtils.hasText(clientIp)) {
            return false;
        }
        for (AccessProperties.Rule rule : whitelist.getRules()) {
            if (matchesRule(rule, httpMethod, path, clientIp)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(AccessProperties.Rule rule,
                                String httpMethod,
                                String path,
                                String clientIp) {
        if (rule == null || !StringUtils.hasText(rule.getPathPattern())) {
            return false;
        }
        return matchesMethod(rule.getMethods(), httpMethod)
                && pathMatcher.match(rule.getPathPattern(), path)
                && matchesCidr(rule.getCidrs(), clientIp);
    }

    private boolean matchesMethod(List<String> methods, String httpMethod) {
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        for (String method : methods) {
            if (ALL_METHODS.equalsIgnoreCase(method)
                    || (StringUtils.hasText(httpMethod) && method.equalsIgnoreCase(httpMethod))) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(List<String> cidrs, String clientIp) {
        if (cidrs == null || cidrs.isEmpty()) {
            return false;
        }
        for (String cidr : cidrs) {
            if (matchesCidr(cidr, clientIp)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String cidr, String clientIp) {
        if (!StringUtils.hasText(cidr)) {
            return false;
        }
        String normalized = normalizeLoopback(clientIp.trim());
        String trimmedCidr = cidr.trim();
        try {
            if (!trimmedCidr.contains("/")) {
                return InetAddress.getByName(trimmedCidr).equals(InetAddress.getByName(normalized));
            }
            String[] parts = trimmedCidr.split("/", 2);
            InetAddress networkAddress = InetAddress.getByName(parts[0]);
            InetAddress clientAddress = InetAddress.getByName(normalized);
            byte[] networkBytes = networkAddress.getAddress();
            byte[] clientBytes = clientAddress.getAddress();
            if (networkBytes.length != clientBytes.length) {
                return false;
            }
            int prefixLength = Integer.parseInt(parts[1]);
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

    private String normalizeLoopback(String clientIp) {
        if ("0:0:0:0:0:0:0:1".equals(clientIp)) {
            return "::1";
        }
        return clientIp;
    }
}
