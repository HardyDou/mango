package io.mango.auth.core.service.impl;

import io.mango.auth.api.enums.AuthCode;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DefaultWecomLoginClient implements WecomLoginClient {

    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
    private static final String GET_USER_PROFILE_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/get";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int IDENTITY_ERROR_CODE = 1400;
    private static final int SERVICE_ERROR_CODE = 1501;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;
    private static final int WECOM_PROFILE_PERMISSION_DENIED = 48002;
    private static final int WECOM_TRUSTED_IP_MISSING = 60020;
    private static final int WECOM_MEMBER_NOT_FOUND = 60111;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DefaultWecomLoginClient() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), new ObjectMapper());
    }

    DefaultWecomLoginClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getUserId(String corpId, String secret, String code) {
        String accessToken = fetchAccessToken(corpId, secret);
        String uri = GET_USER_INFO_URL + "?access_token=" + encode(accessToken) + "&code=" + encode(code);
        String body = sendGet(uri, "企业微信登录身份解析失败");
        int errCode = readErrCode(body);
        if (errCode != 0) {
            throw new BizException(IDENTITY_ERROR_CODE, sanitizeError("企业微信登录身份解析失败", body));
        }
        String userId = firstText(readString(body, "userid"), readString(body, "UserId"));
        if (!StringUtils.hasText(userId)) {
            throw new BizException(IDENTITY_ERROR_CODE, "企业微信登录未返回成员 userid，请确认扫码账号属于当前企业");
        }
        return userId;
    }

    @Override
    public WecomUserProfile getUserProfile(String corpId, String secret, String userId) {
        String accessToken = fetchAccessToken(corpId, secret);
        String uri = GET_USER_PROFILE_URL + "?access_token=" + encode(accessToken) + "&userid=" + encode(userId);
        String body = sendGet(uri, "企业微信成员资料获取失败");
        int errCode = readErrCode(body);
        if (errCode != 0) {
            throw new BizException(AuthCode.WECOM_PROFILE_SYNC_FAILED.getCode(), profileError(errCode, body));
        }
        String displayName = readString(body, "name");
        if (!StringUtils.hasText(displayName)) {
            throw new BizException(AuthCode.WECOM_PROFILE_SYNC_FAILED.getCode(),
                    "企业微信未返回成员昵称，请检查应用的成员可见范围和资料读取权限");
        }
        String responseUserId = firstText(readString(body, "userid"), userId);
        String avatarUrl = firstText(readString(body, "avatar"), readString(body, "thumb_avatar"));
        return new WecomUserProfile(responseUserId, displayName.trim(), avatarUrl);
    }

    private String fetchAccessToken(String corpId, String secret) {
        String uri = GET_TOKEN_URL + "?corpid=" + encode(corpId) + "&corpsecret=" + encode(secret);
        String body = sendGet(uri, "企业微信 access_token 获取失败");
        int errCode = readErrCode(body);
        if (errCode != 0) {
            throw new BizException(SERVICE_ERROR_CODE, sanitizeError("企业微信 access_token 获取失败", body));
        }
        String token = readString(body, "access_token");
        if (!StringUtils.hasText(token)) {
            throw new BizException(SERVICE_ERROR_CODE, "企业微信 access_token 响应为空");
        }
        return token;
    }

    private String sendGet(String uri, String message) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).timeout(TIMEOUT).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < HTTP_SUCCESS_MIN || response.statusCode() >= HTTP_SUCCESS_MAX) {
                throw new BizException(SERVICE_ERROR_CODE, message);
            }
            return response.body();
        } catch (IOException ex) {
            throw new BizException(SERVICE_ERROR_CODE, message + "：网络异常", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(SERVICE_ERROR_CODE, message + "：请求被中断", ex);
        }
    }

    private int readErrCode(String json) {
        JsonNode value = readJson(json).get("errcode");
        return value == null ? 0 : value.asInt();
    }

    private String readString(String json, String key) {
        JsonNode value = readJson(json).get(key);
        return value == null || value.isNull() ? null : value.asText();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new BizException(SERVICE_ERROR_CODE, "企业微信响应格式无效", exception);
        }
    }

    private String sanitizeError(String prefix, String responseBody) {
        String errmsg = readString(responseBody, "errmsg");
        return StringUtils.hasText(errmsg) ? prefix + "：" + errmsg : prefix;
    }

    private String profileError(int errCode, String responseBody) {
        return switch (errCode) {
            case WECOM_PROFILE_PERMISSION_DENIED -> "企业微信应用没有成员资料读取权限";
            case WECOM_TRUSTED_IP_MISSING -> "企业微信应用未配置当前服务器可信 IP";
            case WECOM_MEMBER_NOT_FOUND -> "企业微信成员不存在或不在当前应用可见范围内";
            default -> sanitizeError("企业微信成员资料获取失败（" + errCode + "）", responseBody);
        };
    }

    private String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
