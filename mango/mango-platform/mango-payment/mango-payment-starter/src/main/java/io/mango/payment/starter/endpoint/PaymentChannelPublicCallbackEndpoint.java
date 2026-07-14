package io.mango.payment.starter.endpoint;

import io.mango.common.exception.BizException;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.core.service.IPaymentChannelCallbackHandlerService;
import io.mango.payment.core.service.PaymentChannelCallbackHandleResult;
import io.mango.payment.core.service.PaymentChannelRawCallback;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 支付通道公网原始协议入口，保留通道要求的纯文本 ACK。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentChannelPublicCallbackEndpoint {

    private final IPaymentChannelCallbackHandlerService callbackHandlerService;

    public ServerResponse handle(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        String channelCode = serverRequest.param("channelCode").orElse(null);
        if (channelCode == null || channelCode.isBlank()) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道编码不能为空");
        }
        Map<String, String> params = params(request);
        PaymentChannelRawCallback callback;
        try {
            callback = rawCallback(channelCode, request, params);
        } catch (IOException ex) {
            log.warn("Payment channel callback body read failed: channelCode={}, method={}, uri={}, remoteAddr={}, paramKeys={}",
                    channelCode, request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), params.keySet(), ex);
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "支付通道回调请求体读取失败", ex);
        }
        log.info("Payment channel callback received: channelCode={}, method={}, uri={}, contentType={}, remoteAddr={}, paramKeys={}",
                channelCode, request.getMethod(), request.getRequestURI(), request.getContentType(), request.getRemoteAddr(), params.keySet());
        PaymentChannelCallbackHandleResult result = callbackHandlerService.handle(callback);
        log.info("Payment channel callback handled: channelCode={}, uri={}", channelCode, request.getRequestURI());
        return ServerResponse.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.responseBody());
    }

    private Map<String, String> params(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) ->
                params.put(key, values == null || values.length == 0 ? null : values[0]));
        return params;
    }

    private PaymentChannelRawCallback rawCallback(
            String channelCode, HttpServletRequest request, Map<String, String> params) throws IOException {
        return new PaymentChannelRawCallback(
                channelCode,
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getContentType(),
                request.getRemoteAddr(),
                params,
                rawBody(request),
                LocalDateTime.now());
    }

    private String rawBody(HttpServletRequest request) throws IOException {
        if (isFormRequest(request) || "GET".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        byte[] bytes = request.getInputStream().readAllBytes();
        return bytes.length == 0 ? null : new String(bytes, requestCharset(request));
    }

    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT)
                        .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private Charset requestCharset(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }
}
