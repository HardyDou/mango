package io.mango.infra.iplocation.core;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * IP 地址分类工具。
 */
public final class IpAddressClassifier {

    private IpAddressClassifier() {
    }

    public static boolean isBlank(String ip) {
        return ip == null || ip.isBlank();
    }

    public static boolean isInvalid(String ip) {
        if (isBlank(ip)) {
            return true;
        }
        try {
            InetAddress.getByName(ip.trim());
            return false;
        } catch (UnknownHostException e) {
            return true;
        }
    }

    public static boolean isPrivateOrLocal(String ip) {
        if (isBlank(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return true;
            }
            if (address instanceof Inet4Address) {
                byte[] bytes = address.getAddress();
                int first = bytes[0] & 0xff;
                int second = bytes[1] & 0xff;
                return first == 100 && second >= 64 && second <= 127
                        || first == 169 && second == 254
                        || first == 172 && second >= 16 && second <= 31
                        || first == 192 && second == 168;
            }
            String normalized = ip.trim().toLowerCase(Locale.ROOT);
            return normalized.startsWith("fc") || normalized.startsWith("fd");
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
