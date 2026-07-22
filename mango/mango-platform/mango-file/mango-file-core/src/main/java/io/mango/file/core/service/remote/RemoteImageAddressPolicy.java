package io.mango.file.core.service.remote;

import io.mango.common.result.Require;
import io.mango.file.api.enums.FileCode;

import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates URI, DNS and network-address boundaries before every connection hop. */
public final class RemoteImageAddressPolicy {

    private static final Pattern IPV4_LITERAL = Pattern.compile("^[0-9.]+$");
    private static final int HTTP_PORT = 80;
    private static final int HTTPS_PORT = 443;
    private static final int IPV4_LENGTH = 4;
    private static final int IPV6_LENGTH = 16;
    private static final int IPV4_MULTICAST_MIN = 224;
    private static final int IPV4_CARRIER_NAT_FIRST = 100;
    private static final int IPV4_CARRIER_NAT_SECOND_MIN = 64;
    private static final int IPV4_CARRIER_NAT_SECOND_MAX = 127;
    private static final int IPV4_DOCUMENTATION_FIRST = 192;
    private static final int IPV4_DOCUMENTATION_SECOND = 0;
    private static final int IPV4_DOCUMENTATION_THIRD = 2;
    private static final int IPV4_BENCHMARK_FIRST = 198;
    private static final int IPV4_BENCHMARK_SECOND_MIN = 18;
    private static final int IPV4_BENCHMARK_SECOND_MAX = 19;
    private static final int IPV4_BENCHMARK_SECOND_LEGACY = 51;
    private static final int IPV4_DOCUMENTATION_TEST_FIRST = 203;
    private static final int IPV4_DOCUMENTATION_TEST_THIRD = 113;
    private static final int IPV6_UNIQUE_LOCAL_MASK = 0xFE;
    private static final int IPV6_UNIQUE_LOCAL_PREFIX = 0xFC;
    private static final int IPV6_DOCUMENTATION_FIRST = 0x20;
    private static final int IPV6_DOCUMENTATION_SECOND = 0x01;
    private static final int IPV6_DOCUMENTATION_THIRD = 0x0D;
    private static final int IPV6_DOCUMENTATION_FOURTH = 0xB8;
    private static final int IPV6_DOCUMENTATION_FOURTH_INDEX = 3;

    private final RemoteHostResolver hostResolver;
    private final Set<Integer> allowedPorts;

    public RemoteImageAddressPolicy(RemoteHostResolver hostResolver, Set<Integer> allowedPorts) {
        this.hostResolver = hostResolver;
        this.allowedPorts = Set.copyOf(allowedPorts);
    }

    /**
     * Normalizes and validates an untrusted source URL.
     *
     * @param sourceUrl untrusted URL
     * @return normalized target and addresses approved for the actual connection
     */
    public RemoteImageTarget validate(String sourceUrl) {
        Require.notBlank(sourceUrl, FileCode.FILE_REMOTE_URL_INVALID);
        try {
            return validate(new URI(sourceUrl.trim()));
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return Require.fail(FileCode.FILE_REMOTE_URL_INVALID);
        }
    }

    /**
     * Validates a redirect target using the same rules as the initial request.
     *
     * @param source untrusted target
     * @return normalized target and pinned addresses
     */
    public RemoteImageTarget validate(URI source) {
        Require.notNull(source, FileCode.FILE_REMOTE_URL_INVALID);
        String scheme = source.getScheme() == null ? "" : source.getScheme().toLowerCase(Locale.ROOT);
        Require.isTrue("http".equals(scheme) || "https".equals(scheme), FileCode.FILE_REMOTE_URL_INVALID);
        Require.isTrue(source.getRawUserInfo() == null && source.getRawFragment() == null,
                FileCode.FILE_REMOTE_URL_INVALID);
        String rawHost = source.getHost();
        Require.notBlank(rawHost, FileCode.FILE_REMOTE_URL_INVALID);
        Require.isFalse(isIpLiteral(rawHost), FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN);
        String host;
        try {
            host = IDN.toASCII(rawHost, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return Require.fail(FileCode.FILE_REMOTE_URL_INVALID);
        }
        Require.isFalse("localhost".equals(host) || host.endsWith(".localhost"),
                FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN);
        int port = source.getPort() < 0 ? defaultPort(scheme) : source.getPort();
        Require.isTrue(allowedPorts.contains(port), FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN);
        URI normalized = normalize(source, scheme, host);
        InetAddress[] addresses;
        try {
            addresses = hostResolver.resolve(host);
        } catch (UnknownHostException ex) {
            return Require.fail(FileCode.FILE_REMOTE_FETCH_FAILED);
        }
        if (addresses == null || addresses.length == 0) {
            return Require.fail(FileCode.FILE_REMOTE_FETCH_FAILED);
        }
        for (InetAddress address : addresses) {
            Require.notNull(address, FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN);
            Require.isFalse(isForbidden(address), FileCode.FILE_REMOTE_ADDRESS_FORBIDDEN);
        }
        return new RemoteImageTarget(normalized, addresses);
    }

    private URI normalize(URI source, String scheme, String host) {
        try {
            String path = source.getRawPath();
            return new URI(scheme, null, host, source.getPort(),
                    path == null || path.isEmpty() ? "/" : path, source.getRawQuery(), null).normalize();
        } catch (URISyntaxException ex) {
            return Require.fail(FileCode.FILE_REMOTE_URL_INVALID);
        }
    }

    private int defaultPort(String scheme) {
        return "https".equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    private boolean isIpLiteral(String host) {
        String value = host.startsWith("[") && host.endsWith("]")
                ? host.substring(1, host.length() - 1)
                : host;
        return value.indexOf(':') >= 0 || IPV4_LITERAL.matcher(value).matches();
    }

    private boolean isForbidden(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == IPV4_LENGTH) {
            return isForbiddenIpv4(bytes);
        }
        return address instanceof Inet6Address && isForbiddenIpv6(bytes);
    }

    private boolean isForbiddenIpv4(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        return isUnspecifiedOrMulticast(first)
                || isCarrierGradeNat(first, second)
                || isDocumentationNetwork(first, second, third)
                || isBenchmarkNetwork(first, second)
                || isDocumentationTestNetwork(first, second, third);
    }

    private boolean isUnspecifiedOrMulticast(int first) {
        return first == 0 || first >= IPV4_MULTICAST_MIN;
    }

    private boolean isCarrierGradeNat(int first, int second) {
        return first == IPV4_CARRIER_NAT_FIRST
                && second >= IPV4_CARRIER_NAT_SECOND_MIN
                && second <= IPV4_CARRIER_NAT_SECOND_MAX;
    }

    private boolean isDocumentationNetwork(int first, int second, int third) {
        return first == IPV4_DOCUMENTATION_FIRST
                && second == IPV4_DOCUMENTATION_SECOND
                && (third == 0 || third == IPV4_DOCUMENTATION_THIRD);
    }

    private boolean isBenchmarkNetwork(int first, int second) {
        return first == IPV4_BENCHMARK_FIRST
                && (second == IPV4_BENCHMARK_SECOND_MIN
                || second == IPV4_BENCHMARK_SECOND_MAX
                || second == IPV4_BENCHMARK_SECOND_LEGACY);
    }

    private boolean isDocumentationTestNetwork(int first, int second, int third) {
        return first == IPV4_DOCUMENTATION_TEST_FIRST
                && second == IPV4_DOCUMENTATION_SECOND
                && third == IPV4_DOCUMENTATION_TEST_THIRD;
    }

    private boolean isForbiddenIpv6(byte[] bytes) {
        if (bytes.length != IPV6_LENGTH) {
            return true;
        }
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        if ((first & IPV6_UNIQUE_LOCAL_MASK) == IPV6_UNIQUE_LOCAL_PREFIX) {
            return true;
        }
        if (first == IPV6_DOCUMENTATION_FIRST && second == IPV6_DOCUMENTATION_SECOND
                && Byte.toUnsignedInt(bytes[2]) == IPV6_DOCUMENTATION_THIRD
                && Byte.toUnsignedInt(bytes[IPV6_DOCUMENTATION_FOURTH_INDEX])
                == IPV6_DOCUMENTATION_FOURTH) {
            return true;
        }
        return false;
    }
}
