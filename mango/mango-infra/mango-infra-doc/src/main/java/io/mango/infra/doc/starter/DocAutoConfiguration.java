package io.mango.infra.doc.starter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * API 文档自动配置。
 * <p>
 * 该模块只提供 OpenAPI/Swagger 开发体验，不参与核心运行时链路。
 *
 * @author Mango
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration")
@ConditionalOnProperty(prefix = "mango.doc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DocProperties.class)
@Import(ModuleGroupedOpenApiRegistrar.class)
public class DocAutoConfiguration {

    private static final String BEARER_AUTH_SCHEME = "BearerAuth";

    @Bean
    public OpenAPI mangoOpenAPI(DocProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion())
                        .contact(new Contact()
                                .name(properties.getContact().getName())
                                .email(properties.getContact().getEmail()))
                        .license(new License()
                                .name(properties.getLicense())))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入登录接口返回的 accessToken，调试时会作为 Authorization: Bearer <token> 发送")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }

    @Bean
    @ConditionalOnProperty(prefix = "mango.doc.module-grouping", name = "include-default-group", havingValue = "true", matchIfMissing = true)
    public GroupedOpenApi publicApi(DocProperties properties) {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(properties.getGroup())
                .pathsToMatch(properties.getPathsToMatch());
        if (properties.getModuleGrouping().isIncludeScopeTags()) {
            builder.addOperationCustomizer(new MangoApiScopeOperationCustomizer());
        }
        return builder.build();
    }

}
