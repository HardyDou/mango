package io.mango.notice.channel.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultWecomDirectoryClient implements WecomDirectoryClient {

    private static final String USER_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/list";
    private static final String DEPARTMENT_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/department/list";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final long ROOT_DEPARTMENT_ID = 1L;

    private final WecomAccessTokenProvider accessTokenProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Override
    public List<WecomDirectoryUser> listUsers(String corpId, String secret) {
        return listUsers(corpId, secret, ROOT_DEPARTMENT_ID, true);
    }

    @Override
    public List<WecomDirectoryUser> listUsers(String corpId, String secret, Long departmentId, boolean fetchChild) {
        if (!StringUtils.hasText(corpId) || !StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("企业微信 CorpId 和通讯录 Secret 不能为空");
        }
        String accessToken = accessTokenProvider.getAccessToken(corpId, secret);
        long targetDepartmentId = departmentId == null ? ROOT_DEPARTMENT_ID : departmentId;
        String uri = USER_LIST_URL
                + "?access_token=" + encode(accessToken)
                + "&department_id=" + targetDepartmentId
                + "&fetch_child=" + (fetchChild ? 1 : 0);
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(uri))
                .timeout(TIMEOUT)
                .GET()
                .build());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new WecomApiException("WECOM_DIRECTORY_HTTP_" + response.statusCode(), "企业微信通讯录获取失败", true);
        }
        return parseUsers(response.body());
    }

    @Override
    public List<WecomDepartment> listDepartments(String corpId, String secret, Long departmentId) {
        if (!StringUtils.hasText(corpId) || !StringUtils.hasText(secret)) {
            throw new IllegalArgumentException("企业微信 CorpId 和通讯录 Secret 不能为空");
        }
        String accessToken = accessTokenProvider.getAccessToken(corpId, secret);
        String uri = DEPARTMENT_LIST_URL + "?access_token=" + encode(accessToken);
        if (departmentId != null) {
            uri += "&id=" + departmentId;
        }
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(uri))
                .timeout(TIMEOUT)
                .GET()
                .build());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new WecomApiException("WECOM_DEPARTMENT_HTTP_" + response.statusCode(), "企业微信部门获取失败", true);
        }
        return parseDepartments(response.body());
    }

    private List<WecomDirectoryUser> parseUsers(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errmsg = root.path("errmsg").asText("企业微信通讯录获取失败");
                throw new WecomApiException("WECOM_DIRECTORY_" + errCode, "企业微信通讯录获取失败：" + errmsg, errCode == -1);
            }
            List<WecomDirectoryUser> users = new ArrayList<>();
            JsonNode userList = root.path("userlist");
            if (!userList.isArray()) {
                return users;
            }
            for (JsonNode item : userList) {
                users.add(toUser(item));
            }
            return users;
        } catch (IOException ex) {
            throw new WecomApiException("WECOM_DIRECTORY_PARSE_ERROR", "企业微信通讯录响应解析失败", true);
        }
    }

    private List<WecomDepartment> parseDepartments(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            int errCode = root.path("errcode").asInt(0);
            if (errCode != 0) {
                String errmsg = root.path("errmsg").asText("企业微信部门获取失败");
                throw new WecomApiException("WECOM_DEPARTMENT_" + errCode, "企业微信部门获取失败：" + errmsg, errCode == -1);
            }
            List<WecomDepartment> departments = new ArrayList<>();
            JsonNode departmentList = root.path("department");
            if (!departmentList.isArray()) {
                return departments;
            }
            for (JsonNode item : departmentList) {
                departments.add(toDepartment(item));
            }
            return departments;
        } catch (IOException ex) {
            throw new WecomApiException("WECOM_DEPARTMENT_PARSE_ERROR", "企业微信部门响应解析失败", true);
        }
    }

    private WecomDepartment toDepartment(JsonNode node) {
        return new WecomDepartment(
                node.has("id") ? node.path("id").asLong() : null,
                text(node, "name"),
                node.has("parentid") ? node.path("parentid").asLong() : null,
                node.has("order") ? node.path("order").asInt() : null);
    }

    private WecomDirectoryUser toUser(JsonNode node) {
        return new WecomDirectoryUser(
                text(node, "userid"),
                text(node, "name"),
                departments(node.path("department")),
                text(node, "position"),
                text(node, "mobile"),
                text(node, "gender"),
                text(node, "email"),
                text(node, "biz_mail"),
                text(node, "avatar"),
                text(node, "alias"),
                node.has("status") ? node.path("status").asInt() : null);
    }

    private List<Long> departments(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<Long> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.canConvertToLong()) {
                values.add(item.asLong());
            }
        }
        return values;
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new WecomApiException("WECOM_DIRECTORY_IO_ERROR", "企业微信通讯录获取网络异常", true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new WecomApiException("WECOM_DIRECTORY_INTERRUPTED", "企业微信通讯录获取被中断", true);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
