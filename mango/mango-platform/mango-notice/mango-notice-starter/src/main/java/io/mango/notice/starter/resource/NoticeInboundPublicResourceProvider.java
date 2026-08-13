package io.mango.notice.starter.resource;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/** Declares the function-based inbound callbacks as anonymous API resources. */
@Component
public class NoticeInboundPublicResourceProvider implements ResourceProvider {

    public static final String MODULE_CODE = "notice-inbound-public-api";
    public static final String WECOM_PATH = "/notice/inbound-callbacks/public";
    public static final String MAIL_PATH = "/notice/inbound-mail-callbacks/public";

    @Override
    public List<String> moduleCodes() {
        return List.of(MODULE_CODE);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return List.of(
                callback("GET", WECOM_PATH, "2026081307650000001", "企业微信回调验证", "handleWecom"),
                callback("POST", WECOM_PATH, "2026081307650000002", "企业微信消息接收", "handleWecom"),
                callback("POST", MAIL_PATH, "2026081307650000003", "邮箱推送接收", "handleMail"));
    }

    private ResourceDeclaration callback(
            String method, String path, String id, String name, String handlerMethod) {
        return ResourceDeclarationBuilder.create(ResourceTypes.API_RESOURCE)
                .id(id)
                .version(1)
                .module(MODULE_CODE, "通知入站公网接口")
                .bizKey("api.notice." + method + "." + path.substring(1).replace('/', '.'))
                .name(name)
                .targetModule("authorization")
                .string("moduleName", "notice")
                .string("httpMethod", method)
                .string("pathPattern", path)
                .string("resourceCode", method + ":" + path)
                .string("accessMode", "PUBLIC")
                .string("handlerClass", "io.mango.notice.starter.endpoint.NoticeInboundPublicEndpoint")
                .string("handlerMethod", handlerMethod)
                .string("description", name)
                .build();
    }
}
