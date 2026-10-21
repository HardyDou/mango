package io.mango.information.starter;

import io.mango.captcha.api.CaptchaApi;
import io.mango.identity.api.IdentityUserApi;
import io.mango.information.core.mapper.InformationFormMapper;
import io.mango.information.core.service.InformationActorNameProvider;
import io.mango.information.starter.actor.IdentityInformationActorNameProvider;
import io.mango.information.starter.config.InformationSubmitProtectionProperties;
import io.mango.information.starter.endpoint.InformationPublicFileEndpoint;
import io.mango.information.starter.security.InformationSubmitProtection;
import io.mango.infra.kv.api.IKvStore;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/** 信息采集自动配置。 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mango.information", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("io.mango.information.core.mapper")
@EnableConfigurationProperties(InformationSubmitProtectionProperties.class)
@ComponentScan({
    "io.mango.information.core.service",
    "io.mango.information.starter.controller",
    "io.mango.information.starter.endpoint",
    "io.mango.information.starter.resource"
})
public class InformationAutoConfiguration {

    /** 公开文件入口以函数式路由声明，避免把二进制协议塞进管理接口契约。 */
    @Bean
    public InformationSubmitProtection informationSubmitProtection(
            ObjectProvider<IKvStore> kvStoreProvider,
            ObjectProvider<CaptchaApi> captchaApiProvider,
            InformationSubmitProtectionProperties properties) {
        return new InformationSubmitProtection(kvStoreProvider, captchaApiProvider, properties);
    }

    @Bean
    public RouterFunction<ServerResponse> informationPublicFileRoutes(InformationPublicFileEndpoint endpoint) {
        return route()
                .POST("/information/open/forms/{formCode}/upload-ticket", endpoint::ticket)
                .POST("/information/open/forms/{formCode}/files", endpoint::upload)
                .GET("/information/open/forms/{formCode}/files/detail", endpoint::detail)
                .GET("/information/open/forms/{formCode}/files/preview", endpoint::preview)
                .build();
    }

    /**
     * 平台身份能力存在时装配人员显示名适配；缺失时列表和详情回退展示兜底文案。
     */
    @Bean
    @ConditionalOnClass(IdentityUserApi.class)
    @ConditionalOnMissingBean(InformationActorNameProvider.class)
    public InformationActorNameProvider informationActorNameProvider(ObjectProvider<IdentityUserApi> identityUserApi) {
        return new IdentityInformationActorNameProvider(identityUserApi.getIfAvailable());
    }

    static Class<?> marker() {
        return InformationFormMapper.class;
    }
}
