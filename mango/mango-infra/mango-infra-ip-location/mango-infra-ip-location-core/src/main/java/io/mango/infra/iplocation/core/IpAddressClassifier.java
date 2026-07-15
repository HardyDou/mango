package io.mango.infra.iplocation.core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 地址分类工具。
 */
public final class IpAddressClassifier {

    private static final int IPV4_PART_COUNT = 4;
    private static final int IPV4_PART_MAX_LENGTH = 3;
    private static final int MAX_UNSIGNED_BYTE = 255;
    private static final int UNSIGNED_BYTE_MASK = 0xff;
    private static final int CARRIER_GRADE_NAT_FIRST = 100;
    private static final int CARRIER_GRADE_NAT_SECOND_MIN = 64;
    private static final int CARRIER_GRADE_NAT_SECOND_MAX = 127;
    private static final int LINK_LOCAL_FIRST = 169;
    private static final int LINK_LOCAL_SECOND = 254;
    private static final int PRIVATE_172_FIRST = 172;
    private static final int PRIVATE_172_SECOND_MIN = 16;
    private static final int PRIVATE_172_SECOND_MAX = 31;
    private static final int PRIVATE_192_FIRST = 192;
    private static final int PRIVATE_192_SECOND = 168;
    private static final int IPV6_UNIQUE_LOCAL_MASK = 0xfe;
    private static final int IPV6_UNIQUE_LOCAL_PREFIX = 0xfc;

    private IpAddressClassifier() {
    }

    public static boolean isBlank(String ip) {
        return ip == null || ip.isBlank();
    }

    public static boolean isInvalid(String ip) {
        return parseLiteral(ip) == null;
    }

    public static boolean isPrivateOrLocal(String ip) {
        InetAddress address = parseLiteral(ip);
        if (address == null) {
            return false;
        }
        if (isStandardLocal(address)) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            return isSpecialIpv4Range(bytes);
        }
        return (bytes[0] & IPV6_UNIQUE_LOCAL_MASK) == IPV6_UNIQUE_LOCAL_PREFIX;
    }

    private static boolean isStandardLocal(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
            return true;
        }
        return address.isLinkLocalAddress() || address.isSiteLocalAddress();
    }

    private static boolean isSpecialIpv4Range(byte[] bytes) {
        int first = bytes[0] & UNSIGNED_BYTE_MASK;
        int second = bytes[1] & UNSIGNED_BYTE_MASK;
        if (first == CARRIER_GRADE_NAT_FIRST) {
            return second >= CARRIER_GRADE_NAT_SECOND_MIN && second <= CARRIER_GRADE_NAT_SECOND_MAX;
        }
        if (first == LINK_LOCAL_FIRST) {
            return second == LINK_LOCAL_SECOND;
        }
        if (first == PRIVATE_172_FIRST) {
            return second >= PRIVATE_172_SECOND_MIN && second <= PRIVATE_172_SECOND_MAX;
        }
        return first == PRIVATE_192_FIRST && second == PRIVATE_192_SECOND;
    }

    private static InetAddress parseLiteral(String ip) {
        if (isBlank(ip)) {
            return null;
        }
        String value = ip.trim();
        try {
            if (value.indexOf(':') >= 0) {
                return parseIpv6(value);
            }
            return parseIpv4(value);
        } catch (IllegalArgumentException | UnknownHostException e) {
            return null;
        }
    }

    private static InetAddress parseIpv4(String value) throws UnknownHostException {
        String[] parts = value.split("\\.", -1);
        if (parts.length != IPV4_PART_COUNT) {
            throw new IllegalArgumentException("IPv4 address must contain four parts");
        }
        byte[] address = new byte[IPV4_PART_COUNT];
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()
                    || part.length() > IPV4_PART_MAX_LENGTH
                    || !part.chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("IPv4 address contains an invalid part");
            }
            int number = Integer.parseInt(part);
            if (number > MAX_UNSIGNED_BYTE) {
                throw new IllegalArgumentException("IPv4 address part exceeds 255");
            }
            address[index] = (byte) number;
        }
        return InetAddress.getByAddress(address);
    }

    private static InetAddress parseIpv6(String value) throws UnknownHostException {
        if (value.indexOf('%') >= 0 || !value.matches("[0-9A-Fa-f:.]+")) {
            throw new IllegalArgumentException("IPv6 address contains invalid characters");
        }
        return InetAddress.getByName(value);
    }
}
