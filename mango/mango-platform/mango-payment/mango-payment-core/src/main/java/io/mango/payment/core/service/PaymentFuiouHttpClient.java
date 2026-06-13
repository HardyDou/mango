package io.mango.payment.core.service;

import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentFuiouHttpClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final PaymentFuiouXmlCodec xmlCodec;

    public Map<String, String> post(String url, Map<String, String> signedFields) {
        Require.notBlank(url, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口地址不能为空");
        String xml = xmlCodec.encode(signedFields);
        String encodedXml = URLEncoder.encode(xml, PaymentFuiouSignService.FUIOU_CHARSET);
        String body = "req=" + URLEncoder.encode(encodedXml, PaymentFuiouSignService.FUIOU_CHARSET);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).
                timeout(REQUEST_TIMEOUT).
                header("Content-Type", "application/x-www-form-urlencoded; charset=GBK").
                POST(HttpRequest.BodyPublishers.ofString(body, PaymentFuiouSignService.FUIOU_CHARSET)).
                build();
        try {
            HttpResponse<String> response = HttpClient.newBuilder().
                    connectTimeout(CONNECT_TIMEOUT).
                    build().
                    send(request, HttpResponse.BodyHandlers.ofString(PaymentFuiouSignService.FUIOU_CHARSET));
            Require.isTrue(response.statusCode() >= HTTP_SUCCESS_MIN && response.statusCode() < HTTP_SUCCESS_MAX,
                    PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口 HTTP 响应异常");
            return xmlCodec.decode(java.net.URLDecoder.decode(response.body(), PaymentFuiouSignService.FUIOU_CHARSET));
        } catch (IOException ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口调用失败", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口调用被中断", ex);
        }
    }

    public String postForm(String url, Map<String, String> fields) {
        Require.notBlank(url, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口地址不能为空");
        Require.notNull(fields, PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友表单字段不能为空");
        String body = fields.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).
                timeout(REQUEST_TIMEOUT).
                header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8").
                POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).
                build();
        try {
            HttpResponse<String> response = HttpClient.newBuilder().
                    connectTimeout(CONNECT_TIMEOUT).
                    build().
                    send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            Require.isTrue(response.statusCode() >= HTTP_SUCCESS_MIN && response.statusCode() < HTTP_SUCCESS_MAX,
                    PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口 HTTP 响应异常");
            return response.body();
        } catch (IOException ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口调用失败", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "富友接口调用被中断", ex);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
