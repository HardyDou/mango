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
import java.time.Duration;
import java.util.Map;

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
        String body = "req=" + URLEncoder.encode(xml, PaymentFuiouSignService.FUIOU_CHARSET);
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
}
