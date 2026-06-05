package io.mango.auth.core.service.impl;

import io.mango.auth.core.service.WecomLoginClient;
import io.mango.common.exception.BizException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DefaultWecomLoginClient implements WecomLoginClient {

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public DefaultWecomLoginClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    DefaultWecomLoginClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getUserId(String corpId, String secret, String code) {
        String accessToken = fetchAccessToken(corpId, secret);
        String uri = GET_USER_INFO_URL + "?access_token=" + encode(accessToken) + "&code=" + encode(code);
        String body = sendGet(uri, "企业微信登录身份解析失败");
        int errCode = readErrCode(body);
        if (errCode != 0) {
            throw new BizException(1400, sanitizeError("企业微信登录身份解析失败", body));
        }
        String userId = firstText(readString(body, "userid"), readString(body, "UserId"));
        if (!StringUtils.hasText(userId)) {
            throw new BizException(1400, "企业微信登录未返回成员 userid，请确认扫码账号属于当前企业");
        }
        return userId;
    }

    private String fetchAccessToken(String corpId, String secret) {
        String uri = GET_TOKEN_URL + "?corpid=" + encode(corpId) + "&corpsecret=" + encode(secret);
        String body = sendGet(uri, "企业微信 access_token 获取失败");
        int errCode = readErrCode(body);
        if (errCode != 0) {
            throw new BizException(1501, sanitizeError("企业微信 access_token 获取失败", body));
        }
        String token = readString(body, "access_token");
        if (!StringUtils.hasText(token)) {
            throw new BizException(1501, "企业微信 access_token 响应为空");
        }
        return token;
    }

    private String sendGet(String uri, String message) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).timeout(TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BizException(1501, message);
            }
            return response.body();
        } catch (IOException ex) {
            throw new BizException(1501, message + "：网络异常", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(1501, message + "：请求被中断", ex);
        }
    }

    private int readErrCode(String json) {
        String value = readNumber(json, "errcode");
        return value == null ? 0 : Integer.parseInt(value);
    }

    private String readString(String json, String key) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String readNumber(String json, String key) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String sanitizeError(String prefix, String responseBody) {
        String errmsg = readString(responseBody, "errmsg");
        return StringUtils.hasText(errmsg) ? prefix + "：" + errmsg : prefix;
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
