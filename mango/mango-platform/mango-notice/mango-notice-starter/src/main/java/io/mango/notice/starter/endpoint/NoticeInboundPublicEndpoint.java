package io.mango.notice.starter.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.result.Require;
import io.mango.notice.api.InboundNoticeMessage;
import io.mango.notice.api.NoticeInboundReceiver;
import io.mango.notice.api.NoticeInboundWebhookProvider;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.channel.wecom.WecomInboundConfig;
import io.mango.notice.channel.wecom.WecomInboundMessageAdapter;
import io.mango.notice.channel.wecom.WecomInboundRequest;
import io.mango.notice.core.service.NoticeInboundChannelConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Public WeCom and inbound-mail callback endpoint with provider-specific plain-text ACKs. */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoticeInboundPublicEndpoint {

    private static final int MAX_CALLBACK_BYTES = 2 * 1024 * 1024;

    private final NoticeInboundChannelConfigService configService;
    private final NoticeInboundReceiver receiver;
    private final WecomInboundMessageAdapter wecomAdapter;
    private final List<NoticeInboundWebhookProvider> webhookProviders;
    private final ObjectMapper objectMapper;

    public ServerResponse handleWecom(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        Long channelConfigId = channelConfigId(serverRequest);
        var config = configService.resolve(channelConfigId, NoticeChannelType.WECOM);
        WecomInboundConfig wecomConfig = wecomConfig(config.materializedConfigJson());
        WecomInboundRequest inboundRequest = new WecomInboundRequest(
                firstParameter(request, "msg_signature", "signature"),
                request.getParameter("timestamp"), request.getParameter("nonce"),
                request.getParameter("echostr"), rawBody(request));
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String echo = wecomAdapter.verifyUrl(inboundRequest, wecomConfig);
            return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body(echo);
        }
        InboundNoticeMessage message = wecomAdapter.parseMessage(
                inboundRequest, wecomConfig, config.tenantId(), config.id());
        receiver.receive(message);
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body("success");
    }

    public ServerResponse handleMail(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        Long channelConfigId = channelConfigId(serverRequest);
        var config = configService.resolve(channelConfigId, NoticeChannelType.EMAIL);
        Map<String, String> headers = headers(request);
        Map<String, String> parameters = parameters(request);
        String body = rawBody(request);
        NoticeInboundWebhookProvider provider = webhookProviders.stream()
                .filter(NoticeInboundWebhookProvider::supportsInboundReception)
                .filter(candidate -> candidate.providerCode().equalsIgnoreCase(config.providerCode()))
                .filter(candidate -> candidate.supports(headers, parameters, body))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "邮箱服务商未提供经过验真的真实入站收件适配器: " + config.providerCode()));
        InboundNoticeMessage message = provider.parse(
                headers, parameters, body, config.materializedConfigJson(), config.tenantId(), config.id());
        receiver.receive(message);
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).body("success");
    }

    private Long channelConfigId(ServerRequest request) {
        String value = request.param("channelConfigId").orElse(null);
        Require.notBlank(value, "接收渠道配置 ID 不能为空");
        Require.isTrue(value.matches("\\d+"), "接收渠道配置 ID 格式非法");
        return Long.valueOf(value);
    }

    private WecomInboundConfig wecomConfig(String json) {
        Map<String, String> config = jsonMap(json);
        String corpId = firstText(config.get("corpId"), config.get("receiveId"));
        String token = firstText(config.get("callbackToken"), config.get("token"));
        String aesKey = firstText(config.get("encodingAesKey"), config.get("callbackEncodingAesKey"));
        Require.notBlank(token, "企业微信回调 Token 未配置");
        Require.notBlank(aesKey, "企业微信回调 EncodingAESKey 未配置");
        return new WecomInboundConfig(corpId, token, aesKey);
    }

    private Map<String, String> jsonMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException("接收渠道配置 JSON 格式错误", failure);
        }
    }

    private String rawBody(HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod()) || isForm(request)) {
            return null;
        }
        try {
            byte[] bytes = request.getInputStream().readNBytes(MAX_CALLBACK_BYTES + 1);
            Require.isTrue(bytes.length <= MAX_CALLBACK_BYTES, "入站回调请求体超过限制");
            return bytes.length == 0 ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalArgumentException("入站回调请求体读取失败", failure);
        }
    }

    private boolean isForm(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.toLowerCase(Locale.ROOT)
                .startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private Map<String, String> parameters(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) ->
                result.put(key, values == null || values.length == 0 ? null : values[0]));
        return Map.copyOf(result);
    }

    private Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(name -> result.put(name, request.getHeader(name)));
        return Map.copyOf(result);
    }

    private String firstParameter(HttpServletRequest request, String first, String second) {
        return firstText(request.getParameter(first), request.getParameter(second));
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }
}
