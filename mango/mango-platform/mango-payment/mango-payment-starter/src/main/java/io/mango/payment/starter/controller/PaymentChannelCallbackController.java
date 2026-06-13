package io.mango.payment.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.annotation.InternalApi;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.PaymentChannelCallbackApi;
import io.mango.payment.api.command.PaymentChannelCallbackCommand;
import io.mango.payment.api.vo.PaymentChannelCallbackResultVO;
import io.mango.payment.core.service.PaymentChannelCallbackHandleResult;
import io.mango.payment.core.service.PaymentChannelCallbackHandlerRegistry;
import io.mango.payment.core.service.PaymentChannelRawCallback;
import io.mango.payment.core.service.PaymentChannelCallbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "支付通道标准化回调", description = "支付通道适配器验签后提交的标准化回调接口")
public class PaymentChannelCallbackController implements PaymentChannelCallbackApi {

    private final PaymentChannelCallbackService callbackService;
    private final PaymentChannelCallbackHandlerRegistry callbackHandlerRegistry;

    @Override
    @InternalApi(desc = "支付通道标准化回调")
    @PostMapping("/payment/channel-callbacks")
    @Operation(summary = "处理支付通道标准化回调", description = "由具体通道适配器完成验签后调用，推进支付或退款订单状态并触发业务通知")
    public R<PaymentChannelCallbackResultVO> handle(@Valid @RequestBody PaymentChannelCallbackCommand command) {
        return R.ok(callbackService.handle(command));
    }

    @ApiAccess(mode = ApiResourceAccessMode.PUBLIC, desc = "支付通道公网回调")
    @RequestMapping(
            value = {"/payment/channel-callbacks/{channelCode}", "/api/payment/channel-callbacks/{channelCode}"},
            method = {RequestMethod.POST, RequestMethod.GET})
    @Operation(summary = "处理支付通道公网回调", description = "按通道编码路由到具体通道处理器，由通道处理器完成解析、验签和 ACK")
    public ResponseEntity<String> handlePublic(
            @PathVariable String channelCode,
            HttpServletRequest request) {
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
        try {
            PaymentChannelCallbackHandleResult result = callbackHandlerRegistry.handle(callback);
            log.info("Payment channel callback handled: channelCode={}, uri={}", channelCode, request.getRequestURI());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.contentType()))
                    .body(result.responseBody());
        } catch (RuntimeException ex) {
            log.warn("Payment channel callback failed: channelCode={}, uri={}, paramKeys={}",
                    channelCode, request.getRequestURI(), params.keySet(), ex);
            throw ex;
        }
    }

    private Map<String, String> params(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values == null || values.length == 0 ? null : values[0]));
        return params;
    }

    private PaymentChannelRawCallback rawCallback(
            String channelCode,
            HttpServletRequest request,
            Map<String, String> params) throws IOException {
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
        if (bytes.length == 0) {
            return null;
        }
        return new String(bytes, requestCharset(request));
    }

    private boolean isFormRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private Charset requestCharset(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
    }
}
