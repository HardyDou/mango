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
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_REDIRECT_MIN = 300;
    private static final int HTTP_REDIRECT_MAX = 400;
    private static final int WEBP_MIN_LENGTH = 12;
    private static final int WEBP_TYPE_OFFSET = 8;
    private static final byte[] PNG_SIGNATURE = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF_SIGNATURE = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_SIGNATURE = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF_87A_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF_89A_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);

    private final RemoteImageAddressPolicy addressPolicy;
    private final boolean enabled;
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;
    private final long maxSize;
    private final int maxRedirects;

    public ApacheRemoteImageFetcher(RemoteImageAddressPolicy addressPolicy,
                                    FileProperties.RemoteImport properties) {
        this.addressPolicy = addressPolicy;
        this.enabled = properties.isEnabled();
        this.connectTimeoutMillis = properties.getConnectTimeoutMillis();
        this.readTimeoutMillis = properties.getReadTimeoutMillis();
        this.maxSize = properties.getMaxSize();
        this.maxRedirects = properties.getMaxRedirects();
    }

    @Override
    public RemoteImageContent fetch(String sourceUrl) {
        Require.isTrue(enabled, FileCode.FILE_REMOTE_FETCH_FAILED,
                "远程图片导入已关闭");
        RemoteImageTarget target = addressPolicy.validate(sourceUrl);
        long deadlineNanos = System.nanoTime() + Duration.ofMillis(readTimeoutMillis).toNanos();
        int redirects = 0;
        while (true) {
            HopResult response = execute(target, deadlineNanos);
            if (!response.redirect()) {
                return validateImage(response.body(), response.contentType());
            }
            Require.isTrue(redirects < maxRedirects, FileCode.FILE_REMOTE_FETCH_FAILED);
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

    private HopResult execute(RemoteImageTarget target, long deadlineNanos) {
        long remainingMillis = remainingMillis(deadlineNanos);
        InetAddress[] approvedAddresses = target.addresses();
        String approvedHost = target.uri().getHost();
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(pinnedDnsResolver(approvedHost, approvedAddresses))
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(Math.min(
                                connectTimeoutMillis, remainingMillis)))
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

    private HopResult readResponse(org.apache.hc.core5.http.ClassicHttpResponse response,
                                   long deadlineNanos) throws IOException {
        int status = response.getCode();
        if (status >= HTTP_REDIRECT_MIN && status < HTTP_REDIRECT_MAX) {
            var locationHeader = response.getFirstHeader(HttpHeaders.LOCATION);
            Require.notNull(locationHeader, FileCode.FILE_REMOTE_FETCH_FAILED);
            String location = locationHeader.getValue();
            Require.notBlank(location, FileCode.FILE_REMOTE_FETCH_FAILED);
            return HopResult.redirect(location);
        }
        Require.isTrue(status >= HTTP_SUCCESS_MIN && status < HTTP_REDIRECT_MIN,
                FileCode.FILE_REMOTE_FETCH_FAILED);
        HttpEntity entity = response.getEntity();
        Require.notNull(entity, FileCode.FILE_REMOTE_CONTENT_INVALID);
        long declaredLength = entity.getContentLength();
        Require.isTrue(declaredLength < 0 || declaredLength <= maxSize,
                FileCode.FILE_REMOTE_IMAGE_TOO_LARGE);
        var contentTypeHeader = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        Require.notNull(contentTypeHeader, FileCode.FILE_REMOTE_CONTENT_INVALID);
        String contentType = normalizeContentType(contentTypeHeader.getValue());
        Require.isTrue(contentType.startsWith("image/"), FileCode.FILE_REMOTE_CONTENT_INVALID);
        try (InputStream input = entity.getContent()) {
            return HopResult.success(readLimited(input, deadlineNanos), contentType);
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
            Require.isTrue(total <= maxSize, FileCode.FILE_REMOTE_IMAGE_TOO_LARGE);
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
        if (startsWith(bytes, PNG_SIGNATURE)) {
            return new DetectedImage("image/png", "png");
        }
        if (startsWith(bytes, JPEG_SIGNATURE)) {
            return new DetectedImage("image/jpeg", "jpg");
        }
        if (startsWith(bytes, GIF_87A_SIGNATURE) || startsWith(bytes, GIF_89A_SIGNATURE)) {
            return new DetectedImage("image/gif", "gif");
        }
        if (bytes.length >= WEBP_MIN_LENGTH
                && matchesAt(bytes, 0, RIFF_SIGNATURE)
                && matchesAt(bytes, WEBP_TYPE_OFFSET, WEBP_SIGNATURE)) {
            return new DetectedImage("image/webp", "webp");
        }
        return null;
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
        return matchesAt(source, 0, prefix);
    }

    private boolean matchesAt(byte[] source, int offset, byte[] expected) {
        if (source.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index += 1) {
            if (source[offset + index] != expected[index]) {
                return false;
            }
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

    private static final class HopResult {

        private final boolean redirect;
        private final String location;
        private final byte[] body;
        private final String contentType;

        private HopResult(boolean redirect, String location, byte[] body, String contentType) {
            this.redirect = redirect;
            this.location = location;
            this.body = body;
            this.contentType = contentType;
        }

        private static HopResult redirect(String location) {
            return new HopResult(true, location, new byte[0], "");
        }

        private static HopResult success(byte[] body, String contentType) {
            return new HopResult(false, "", body, contentType);
        }

        private boolean redirect() {
            return redirect;
        }

        private String location() {
            return location;
        }

        private byte[] body() {
            return body;
        }

        private String contentType() {
            return contentType;
        }
    }

    private static final class DetectedImage {

        private final String contentType;
        private final String extension;

        private DetectedImage(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        private String contentType() {
            return contentType;
        }

        private String extension() {
            return extension;
        }

        private boolean matches(String declaredContentType) {
            return contentType.equals(declaredContentType)
                    || ("image/jpeg".equals(contentType) && "image/jpg".equals(declaredContentType));
        }
    }
}
