package io.mango.notice.channel.wecom;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class DefaultWecomMessageClient implements WecomMessageClient {

    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public DefaultWecomMessageClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    DefaultWecomMessageClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public WecomMessageSendResponse sendText(String accessToken, WecomTextMessageRequest request) {
        String uri = SEND_MESSAGE_URL + "?access_token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(uri))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(buildBody(request), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(httpRequest);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new WecomApiException("WECOM_SEND_HTTP_" + response.statusCode(), "企业微信消息发送失败", true);
        }
        int errCode = WecomJsonSupport.readErrCode(response.body());
        if (errCode != 0) {
            throw new WecomApiException("WECOM_SEND_" + errCode, sanitizeError("企业微信消息发送失败", response.body()), errCode == -1);
        }
        return new WecomMessageSendResponse(response.body(), WecomJsonSupport.readString(response.body(), "msgid"));
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new WecomApiException("WECOM_SEND_IO_ERROR", "企业微信消息发送网络异常", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WecomApiException("WECOM_SEND_INTERRUPTED", "企业微信消息发送被中断", true);
        }
    }

    private String buildBody(WecomTextMessageRequest request) {
        return """
                {"touser":"%s","msgtype":"text","agentid":%d,"text":{"content":"%s"},"safe":0,"enable_duplicate_check":0}
                """.formatted(
                WecomJsonSupport.escape(request.toUser()),
                request.agentId(),
                WecomJsonSupport.escape(request.content())).trim();
    }

    private String sanitizeError(String prefix, String responseBody) {
        String errmsg = WecomJsonSupport.readString(responseBody, "errmsg");
        return errmsg == null || errmsg.isBlank() ? prefix : prefix + "：" + errmsg;
    }
}
