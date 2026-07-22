package io.mango.file.starter.remoteimport;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.file.api.enums.FileCode;
import io.mango.file.core.config.FileProperties;
import io.mango.file.core.service.remote.IRemoteImageFetcher;
import io.mango.file.core.service.remote.RemoteImageAddressPolicy;
import io.mango.file.core.service.remote.RemoteImageContent;
import io.mango.file.core.service.remote.RemoteImageTarget;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.util.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

/** Apache HttpClient adapter that pins each validated DNS result to the actual connection. */
public final class ApacheRemoteImageFetcher implements IRemoteImageFetcher {

    private static final String USER_AGENT = "Mango-Remote-Image-Importer/1.0";
    private static final int BUFFER_SIZE = 8192;

    private final RemoteImageAddressPolicy addressPolicy;
    private final FileProperties.RemoteImport properties;

    public ApacheRemoteImageFetcher(RemoteImageAddressPolicy addressPolicy,
                                    FileProperties.RemoteImport properties) {
        this.addressPolicy = addressPolicy;
        this.properties = properties;
    }

    @Override
    public RemoteImageContent fetch(String sourceUrl) {
        Require.isTrue(properties.isEnabled(), FileCode.FILE_REMOTE_FETCH_FAILED,
                "远程图片导入已关闭");
        RemoteImageTarget target = addressPolicy.validate(sourceUrl);
        long deadlineNanos = System.nanoTime() + Duration.ofMillis(properties.getReadTimeoutMillis()).toNanos();
        int redirects = 0;
        while (true) {
            HopResponse response = execute(target, deadlineNanos);
            if (!response.redirect()) {
                return validateImage(response.body(), response.contentType());
            }
            Require.isTrue(redirects < properties.getMaxRedirects(), FileCode.FILE_REMOTE_FETCH_FAILED);
            URI redirectTarget;
            try {
                redirectTarget = target.uri().resolve(response.location());
            } catch (IllegalArgumentException ex) {
                return Require.fail(FileCode.FILE_REMOTE_URL_INVALID);
            }
            target = addressPolicy.validate(redirectTarget);
            redirects += 1;
        }
    }

    private HopResponse execute(RemoteImageTarget target, long deadlineNanos) {
        long remainingMillis = remainingMillis(deadlineNanos);
        InetAddress[] approvedAddresses = target.addresses();
        String approvedHost = target.uri().getHost();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(pinnedDnsResolver(approvedHost, approvedAddresses))
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(Math.min(
                                properties.getConnectTimeoutMillis(), remainingMillis)))
                        .setSocketTimeout(Timeout.ofMilliseconds(remainingMillis))
                        .build())
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(remainingMillis))
                .setResponseTimeout(Timeout.ofMilliseconds(remainingMillis))
                .build();
        try (CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .disableCookieManagement()
                .disableRedirectHandling()
                .build()) {
            HttpGet request = new HttpGet(target.uri());
            request.setHeader(HttpHeaders.ACCEPT, "image/*");
            request.setHeader(HttpHeaders.USER_AGENT, USER_AGENT);
            return client.execute(request, response -> readResponse(response, deadlineNanos));
        } catch (BizException ex) {
            throw ex;
        } catch (IOException ex) {
            return Require.fail(FileCode.FILE_REMOTE_FETCH_FAILED);
        }
    }

    private InetAddress[] pinnedAddresses(String requestedHost,
                                          String approvedHost,
                                          InetAddress[] approvedAddresses) throws UnknownHostException {
        if (!approvedHost.equalsIgnoreCase(requestedHost)) {
            throw new UnknownHostException("Remote host was not approved");
        }
        return Arrays.copyOf(approvedAddresses, approvedAddresses.length);
    }

    private DnsResolver pinnedDnsResolver(String approvedHost, InetAddress[] approvedAddresses) {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return pinnedAddresses(host, approvedHost, approvedAddresses);
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                pinnedAddresses(host, approvedHost, approvedAddresses);
                return approvedHost;
            }
        };
    }

    private HopResponse readResponse(org.apache.hc.core5.http.ClassicHttpResponse response,
                                     long deadlineNanos) throws IOException {
        int status = response.getCode();
        if (status >= 300 && status < 400) {
            var locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
            Require.notNull(locationHeader, FileCode.FILE_REMOTE_FETCH_FAILED);
            String location = locationHeader.getValue();
            Require.notBlank(location, FileCode.FILE_REMOTE_FETCH_FAILED);
            return HopResponse.redirect(location);
        }
        Require.isTrue(status >= 200 && status < 300, FileCode.FILE_REMOTE_FETCH_FAILED);
        HttpEntity entity = response.getEntity();
        Require.notNull(entity, FileCode.FILE_REMOTE_CONTENT_INVALID);
        long declaredLength = entity.getContentLength();
        Require.isTrue(declaredLength < 0 || declaredLength <= properties.getMaxSize(),
                FileCode.FILE_REMOTE_IMAGE_TOO_LARGE);
        var contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        Require.notNull(contentTypeHeader, FileCode.FILE_REMOTE_CONTENT_INVALID);
        String contentType = normalizeContentType(contentTypeHeader.getValue());
        Require.isTrue(contentType.startsWith("image/"), FileCode.FILE_REMOTE_CONTENT_INVALID);
        try (InputStream input = entity.getContent()) {
            return HopResponse.success(readLimited(input, deadlineNanos), contentType);
        }
    }

    private byte[] readLimited(InputStream input, long deadlineNanos) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            remainingMillis(deadlineNanos);
            total += read;
            Require.isTrue(total <= properties.getMaxSize(), FileCode.FILE_REMOTE_IMAGE_TOO_LARGE);
            output.write(buffer, 0, read);
        }
        Require.isTrue(total > 0, FileCode.FILE_REMOTE_CONTENT_INVALID);
        return output.toByteArray();
    }

    private RemoteImageContent validateImage(byte[] bytes, String declaredContentType) {
        DetectedImage detected = detect(bytes);
        Require.notNull(detected, FileCode.FILE_REMOTE_CONTENT_INVALID);
        Require.isTrue(detected.matches(declaredContentType), FileCode.FILE_REMOTE_CONTENT_INVALID);
        return new RemoteImageContent(bytes, detected.contentType(), detected.extension());
    }

    private DetectedImage detect(byte[] bytes) {
        if (startsWith(bytes, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return new DetectedImage("image/png", "png");
        }
        if (startsWith(bytes, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return new DetectedImage("image/jpeg", "jpg");
        }
        if (startsWith(bytes, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                || startsWith(bytes, "GIF89a".getBytes(StandardCharsets.US_ASCII))) {
            return new DetectedImage("image/gif", "gif");
        }
        if (bytes.length >= 12
                && matchesAt(bytes, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
                && matchesAt(bytes, 8, "WEBP".getBytes(StandardCharsets.US_ASCII))) {
            return new DetectedImage("image/webp", "webp");
        }
        return null;
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        return matchesAt(source, 0, prefix);
    }

    private boolean matchesAt(byte[] source, int offset, byte[] expected) {
        if (source.length < offset + expected.length) return false;
        for (int index = 0; index < expected.length; index += 1) {
            if (source[offset + index] != expected[index]) return false;
        }
        return true;
    }

    private String normalizeContentType(String value) {
        int separator = value.indexOf(';');
        String result = separator >= 0 ? value.substring(0, separator) : value;
        return result.trim().toLowerCase(Locale.ROOT);
    }

    private long remainingMillis(long deadlineNanos) {
        long remaining = Duration.ofNanos(deadlineNanos - System.nanoTime()).toMillis();
        Require.isTrue(remaining > 0, FileCode.FILE_REMOTE_FETCH_FAILED);
        return remaining;
    }

    private record HopResponse(boolean redirect, String location, byte[] body, String contentType) {

        private static HopResponse redirect(String location) {
            return new HopResponse(true, location, new byte[0], "");
        }

        private static HopResponse success(byte[] body, String contentType) {
            return new HopResponse(false, "", body, contentType);
        }
    }

    private record DetectedImage(String contentType, String extension) {

        private boolean matches(String declaredContentType) {
            return contentType.equals(declaredContentType)
                    || ("image/jpeg".equals(contentType) && "image/jpg".equals(declaredContentType));
        }
    }
}
