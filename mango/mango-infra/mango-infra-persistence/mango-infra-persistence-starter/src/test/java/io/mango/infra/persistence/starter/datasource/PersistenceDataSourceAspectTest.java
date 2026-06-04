package io.mango.infra.persistence.starter.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceDataSourceAspectTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class,
                    PersistenceDataSourceAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "mango.persistence.datasources.primary.primary=true",
                    "mango.persistence.datasources.primary.url=jdbc:h2:mem:primary_aspect;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                    "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                    "mango.persistence.datasources.primary.username=sa",
                    "mango.persistence.datasources.primary.password=",
                    "mango.persistence.datasources.job.url=jdbc:h2:mem:job_aspect;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                    "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                    "mango.persistence.datasources.job.username=sa",
                    "mango.persistence.datasources.job.password=");

    @Test
    void annotation_shouldSetDatasourceContextForMethod() {
        contextRunner.run(ctx -> {
            AnnotatedService service = ctx.getBean(AnnotatedService.class);
            assertThat(PersistenceDataSourceContext.current()).isEmpty();
            assertThat(service.currentDataSource()).isEqualTo("job");
            assertThat(PersistenceDataSourceContext.current()).isEmpty();
        });
    }

    @Configuration
    static class TestConfig {

        @Bean
        AnnotatedService annotatedService() {
            return new AnnotatedService();
        }
    }

    static class AnnotatedService {

        @PersistenceDataSource("job")
        public String currentDataSource() {
            return PersistenceDataSourceContext.current().orElse("");
        }
    }
}
