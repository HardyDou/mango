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
public class DefaultWecomAccessTokenClient implements WecomAccessTokenClient {

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public DefaultWecomAccessTokenClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    DefaultWecomAccessTokenClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public WecomAccessToken fetch(String corpId, String corpSecret) {
        String uri = GET_TOKEN_URL + "?corpid=" + encode(corpId) + "&corpsecret=" + encode(corpSecret);
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new WecomApiException("WECOM_TOKEN_HTTP_" + response.statusCode(), "企业微信 access_token 获取失败", true);
        }
        int errCode = WecomJsonSupport.readErrCode(response.body());
        if (errCode != 0) {
            throw new WecomApiException("WECOM_TOKEN_" + errCode, sanitizeError("企业微信 access_token 获取失败", response.body()), errCode == -1);
        }
        String token = WecomJsonSupport.readString(response.body(), "access_token");
        if (token == null || token.isBlank()) {
            throw new WecomApiException("WECOM_TOKEN_EMPTY", "企业微信 access_token 响应为空", true);
        }
        return new WecomAccessToken(token, WecomJsonSupport.readLong(response.body(), "expires_in", 7200));
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new WecomApiException("WECOM_TOKEN_IO_ERROR", "企业微信 access_token 获取网络异常", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WecomApiException("WECOM_TOKEN_INTERRUPTED", "企业微信 access_token 获取被中断", true);
        }
    }

    private String sanitizeError(String prefix, String responseBody) {
        String errmsg = WecomJsonSupport.readString(responseBody, "errmsg");
        return errmsg == null || errmsg.isBlank() ? prefix : prefix + "：" + errmsg;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
