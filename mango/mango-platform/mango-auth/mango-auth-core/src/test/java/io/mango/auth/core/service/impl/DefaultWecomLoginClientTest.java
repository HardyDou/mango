package io.mango.auth.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.auth.core.service.WecomLoginClient;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultWecomLoginClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void parsesStructuredJsonWithoutDependingOnFieldOrder() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = response(
                "{\"expires_in\":7200,\"access_token\":\"access-token\",\"errcode\":0}");
        HttpResponse<String> userResponse = response(
                "{\"DeviceId\":\"device\",\"userid\":\"specified-user\",\"errcode\":0}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse, userResponse);
        DefaultWecomLoginClient client = new DefaultWecomLoginClient(httpClient, new ObjectMapper());

        assertThat(client.getUserId("corp", "secret", "code")).isEqualTo("specified-user");
    }

    @Test
    @SuppressWarnings("unchecked")
    void rejectsInvalidJsonInsteadOfGuessingAnExternalIdentity() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> invalidResponse = response("not-json");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(invalidResponse);
        DefaultWecomLoginClient client = new DefaultWecomLoginClient(httpClient, new ObjectMapper());

        assertThatThrownBy(() -> client.getUserId("corp", "secret", "code"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("响应格式无效");
    }

    @Test
    @SuppressWarnings("unchecked")
    void readsCurrentMemberNameAndAvatarWithoutListingTheDirectory() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = response(
                "{\"access_token\":\"access-token\",\"errcode\":0}");
        HttpResponse<String> profileResponse = response(
                "{\"errcode\":0,\"userid\":\"specified-user\",\"name\":\"企业微信张三\","
                        + "\"avatar\":\"https://wework.qpic.cn/avatar.png\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse, profileResponse);
        DefaultWecomLoginClient client = new DefaultWecomLoginClient(httpClient, new ObjectMapper());

        WecomLoginClient.WecomUserProfile profile = client.getUserProfile("corp", "secret", "specified-user");

        assertThat(profile.userId()).isEqualTo("specified-user");
        assertThat(profile.displayName()).isEqualTo("企业微信张三");
        assertThat(profile.avatarUrl()).isEqualTo("https://wework.qpic.cn/avatar.png");
    }

    @Test
    @SuppressWarnings("unchecked")
    void explainsMissingMemberProfilePermission() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> tokenResponse = response(
                "{\"access_token\":\"access-token\",\"errcode\":0}");
        HttpResponse<String> profileResponse = response(
                "{\"errcode\":48002,\"errmsg\":\"api forbidden\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse, profileResponse);
        DefaultWecomLoginClient client = new DefaultWecomLoginClient(httpClient, new ObjectMapper());

        assertThatThrownBy(() -> client.getUserProfile("corp", "secret", "specified-user"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("没有成员资料读取权限");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }
}
