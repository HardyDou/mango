package io.mango.infra.doc.starter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * API Documentation Auto Configuration
 * Provides OpenAPI/Swagger UI integration
 *
 * @author Mango
 */
@AutoConfiguration
@EnableConfigurationProperties(DocProperties.class)
public class DocAutoConfiguration {

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
                                .name(properties.getLicense())));
    }

    @Bean
    public GroupedOpenApi publicApi(DocProperties properties) {
        return GroupedOpenApi.builder()
                .group(properties.getGroup())
                .pathsToMatch(properties.getPathsToMatch())
                .build();
    }
}
