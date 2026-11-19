package io.mango.access.core.auth;

import io.mango.access.core.config.AccessProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/**
 * 来源 IP 白名单匹配器。
 *
 * @author Mango
 */
public class IpWhitelistMatcher {

    private static final String ALL_METHODS = "ALL";

    private static final int IPV6_BYTES = 16;

    private static final int IPV4_MAPPED_OFFSET = 12;

    private static final int IPV4_MAPPED_MARKER_INDEX = 10;

    private static final int BYTE_MASK = 0xFF;

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
}
