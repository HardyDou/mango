package io.mango.payment.starter.resource;

import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.builder.ResourceDeclarationBuilder;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/** 向统一资源中心声明函数式公网回调路由，确保匿名访问策略可被同步。 */
@Component
public class PaymentPublicCallbackResourceProvider implements ResourceProvider {

    static final String MODULE_CODE = "payment-public-callback-api";
    static final String CALLBACK_PATH = "/payment/channel-callbacks/public";

    @Override
    public List<String> moduleCodes() {
        return List.of(MODULE_CODE);
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return List.of(callbackResource("GET", "2026071300400010001"),
                callbackResource("POST", "2026071300400010002"));
    }

    private ResourceDeclaration callbackResource(String method, String id) {
        return ResourceDeclarationBuilder.create(ResourceTypes.API_RESOURCE)
                .id(id)
                .version(1)
                .module(MODULE_CODE, "支付公网回调接口")
                .bizKey("api.payment." + method + ".payment.channel-callbacks.public")
                .name("支付通道公网回调")
                .targetModule("authorization")
                .string("moduleName", "payment")
                .string("httpMethod", method)
                .string("pathPattern", CALLBACK_PATH)
                .string("resourceCode", method + ":" + CALLBACK_PATH)
                .string("accessMode", "PUBLIC")
                .string("handlerClass", "io.mango.payment.starter.endpoint.PaymentChannelPublicCallbackEndpoint")
                .string("handlerMethod", "handle")
                .string("description", "支付通道公网回调")
                .build();
    }
}
