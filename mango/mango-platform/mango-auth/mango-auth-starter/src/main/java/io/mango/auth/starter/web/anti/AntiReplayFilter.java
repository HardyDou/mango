package io.mango.auth.starter.web.anti;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.api.enums.AuthCode;
import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import io.mango.common.result.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在 Servlet 链入口完成防重放、签名和幂等校验。
 *
 * <p>签名校验必须先缓存请求体，再把可重复读取的请求继续传给 MVC，避免消费 JSON 请求体。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The ObjectMapper is an intentionally shared, thread-safe Spring collaborator")
public class AntiReplayFilter extends OncePerRequestFilter {

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000;
    private static final String HEADER_TIMESTAMP = "X-Request-Timestamp";
    private static final String HEADER_NONCE = "X-Replay-Nonce";
    private static final String HEADER_IDEM_KEY = "X-Idempotency-Key";
    private static final String HEADER_SIGN_ALGO = "X-Sign-Algorithm";
    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_SIGN = "X-Sign";
    private static final String IDEMPOTENCY_PROCESSING = "PROCESSING";

    private final ReplayGuard replayGuard;
    private final IdempotencyGuard idempotencyGuard;
    private final SignatureValidator signatureValidator;
    private final AppSecretProvider appSecretProvider;
    private final ObjectMapper objectMapper;
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        if (!validateTimestamp(timestamp, response)) {
            return;
        }
        if (!validateNonce(request.getHeader(HEADER_NONCE), response)) {
            return;
        }

        HttpServletRequest requestToUse = validateSignature(request, response, timestamp);
        if (requestToUse == null) {
            return;
        }

        String idempotencyKey = resolveIdempotencyKey(request);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(requestToUse, response);
            return;
        }
        if (!idempotencyGuard.tryAcquire(idempotencyKey)) {
            writeDuplicateResponse(response, idempotencyKey);
            return;
        }

        filterIdempotentRequest(requestToUse, response, filterChain, idempotencyKey);
    }

    private HttpServletRequest validateSignature(HttpServletRequest request,
                                                 HttpServletResponse response,
                                                 String timestamp) throws IOException {
        String signAlgo = request.getHeader(HEADER_SIGN_ALGO);
        String appKey = request.getHeader(HEADER_APP_KEY);
        String sign = request.getHeader(HEADER_SIGN);
        if (!hasSignatureHeaders(signAlgo, appKey, sign)) {
            return request;
        }

        CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);
        String secret = secretCache.computeIfAbsent(appKey, appSecretProvider::findSecret);
        String signatureTimestamp = timestamp;
        if (signatureTimestamp == null) {
            signatureTimestamp = "";
        }
        if (signatureValidator.validate(signAlgo, appKey, secret,
                signatureTimestamp, cachedRequest.bodyText(), sign)) {
            return cachedRequest;
        }
        log.warn("Signature validation failed: appKey={}", appKey);
        writeError(response, HttpStatus.UNAUTHORIZED.value(), AuthCode.REQUEST_SIGNATURE_INVALID);
        return null;
    }

    private boolean hasSignatureHeaders(String algorithm, String appKey, String signature) {
        return algorithm != null && appKey != null && signature != null;
    }

    private String resolveIdempotencyKey(HttpServletRequest request) {
        if (!writeMethod(request.getMethod())) {
            return null;
        }
        return request.getHeader(HEADER_IDEM_KEY);
    }

    private void writeDuplicateResponse(HttpServletResponse response, String idempotencyKey) throws IOException {
        String cached = idempotencyGuard.getResponse(idempotencyKey);
        if (cached != null && !IDEMPOTENCY_PROCESSING.equals(cached)) {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json");
            response.getWriter().write(cached);
            return;
        }
        writeError(response, HttpStatus.CONFLICT.value(), AuthCode.DUPLICATE_REQUEST);
    }

    private void filterIdempotentRequest(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain,
                                         String idempotencyKey) throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
            if (HttpStatusCode.valueOf(responseWrapper.getStatus()).is2xxSuccessful()) {
                idempotencyGuard.saveResponse(idempotencyKey,
                        new String(responseWrapper.getContentAsByteArray(), responseCharset(responseWrapper)));
            } else {
                idempotencyGuard.releaseProcessing(idempotencyKey);
            }
        } catch (IOException | ServletException | RuntimeException exception) {
            idempotencyGuard.releaseProcessing(idempotencyKey);
            throw exception;
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean validateTimestamp(String timestamp, HttpServletResponse response) throws IOException {
        if (timestamp == null || timestamp.isBlank()) {
            return true;
        }
        try {
            long requestTime = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - requestTime) <= MAX_TIMESTAMP_DIFF_MS) {
                return true;
            }
            writeError(response, HttpStatus.UNAUTHORIZED.value(), AuthCode.REQUEST_EXPIRED);
        } catch (NumberFormatException exception) {
            writeError(response, HttpStatus.BAD_REQUEST.value(), AuthCode.REQUEST_TIMESTAMP_INVALID);
        }
        return false;
    }

    private boolean validateNonce(String nonce, HttpServletResponse response) throws IOException {
        if (nonce == null || nonce.isBlank() || replayGuard.tryAcquire(nonce)) {
            return true;
        }
        writeError(response, HttpStatus.CONFLICT.value(), AuthCode.DUPLICATE_REQUEST);
        return false;
    }

    private boolean writeMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private Charset responseCharset(ContentCachingResponseWrapper response) {
        String encoding = response.getCharacterEncoding();
        if (encoding == null) {
            return StandardCharsets.UTF_8;
        }
        return Charset.forName(encoding);
    }

    private void writeError(HttpServletResponse response, int status, AuthCode code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code)));
    }

    private static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        private String bodyText() {
            return new String(body, requestCharset());
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // In-memory input is always ready; asynchronous callbacks are unnecessary.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), requestCharset()));
        }

        private Charset requestCharset() {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                return StandardCharsets.UTF_8;
            }
            return Charset.forName(encoding);
        }
    }
}
