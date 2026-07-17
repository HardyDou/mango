package io.mango.link.starter.resource;

import io.mango.resource.support.ResourceProvider;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.support.builder.ResourceDeclarationBuilder;
import io.mango.resource.support.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/** 向统一资源中心声明函数式公开跳转路由，确保匿名请求能够通过访问控制。 */
@Component
public class LinkPublicRedirectResourceProvider implements ResourceProvider {

    static final String MODULE_CODE = "link-public-redirect-api";
    static final String REDIRECT_PATH = "/link/open/redirect";
    static final String JUMP_PATH = "/link/open/jump";
    static final String VISIBLE_REDIRECT_PATH = "/link/visible-links/redirect";
    static final String VISIBLE_JUMP_PATH = "/link/visible-links/jump";

    @Override
    public List<String> moduleCodes() {
        return List.of(MODULE_CODE);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return List.of(route("2026071700600010001", REDIRECT_PATH, "redirect", "按公开网址 ID 跳转", "PUBLIC"),
                route("2026071700600010002", JUMP_PATH, "jump", "按公开目标地址跳转", "PUBLIC"),
                route("2026071700600010003", VISIBLE_REDIRECT_PATH, "redirect", "按可见网址 ID 跳转", "LOGIN"),
                route("2026071700600010004", VISIBLE_JUMP_PATH, "jump", "按可见目标地址跳转", "LOGIN"));
    }

    private ResourceDeclaration route(
            String id, String path, String handlerMethod, String description, String accessMode) {
        return ResourceDeclarationBuilder.create(ResourceTypes.API_RESOURCE)
                .id(id)
                .version(1)
                .module(MODULE_CODE, "网址导航公开跳转接口")
                .bizKey("api.link.GET." + path.substring(1).replace('/', '.'))
                .name(description)
                .targetModule("authorization")
                .string("moduleName", "mango-link")
                .string("httpMethod", "GET")
                .string("pathPattern", path)
                .string("resourceCode", "GET:" + path)
                .string("accessMode", accessMode)
                .string("handlerClass", "io.mango.link.starter.endpoint.LinkRedirectEndpoint")
                .string("handlerMethod", handlerMethod)
                .string("description", description)
                .build();
    }
}
