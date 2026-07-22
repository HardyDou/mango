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
        Require.isTrue(addresses != null && addresses.length > 0, FileCode.FILE_REMOTE_FETCH_FAILED);
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
        return "https".equals(scheme) ? 443 : 80;
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
        if (address instanceof Inet4Address && bytes.length == 4) {
            return isForbiddenIpv4(bytes);
        }
        return address instanceof Inet6Address && isForbiddenIpv6(bytes);
    }

    private boolean isForbiddenIpv4(byte[] bytes) {
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        if (first == 0 || first >= 224) return true;
        if (first == 100 && second >= 64 && second <= 127) return true;
        if (first == 192 && second == 0 && (third == 0 || third == 2)) return true;
        if (first == 198 && (second == 18 || second == 19 || second == 51)) return true;
        return first == 203 && second == 0 && third == 113;
    }

    private boolean isForbiddenIpv6(byte[] bytes) {
        if (bytes.length != 16) return true;
        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        if ((first & 0xFE) == 0xFC) return true;
        if (first == 0x20 && second == 0x01
                && Byte.toUnsignedInt(bytes[2]) == 0x0D && Byte.toUnsignedInt(bytes[3]) == 0xB8) {
            return true;
        }
        return false;
    }
}
