package io.mango.job.core.datasource;

import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
import io.mango.infra.persistence.starter.datasource.PersistenceModuleDataSourceResolver;
import io.mango.job.core.entity.MangoJobDefinitionEntity;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.job.core.service.IMangoJobDefinitionService;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import io.mango.job.core.service.impl.MangoJobDefinitionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = MangoJobMultiDataSourceIntegrationTest.TestApplication.class,
        properties = {
                "mango.persistence.datasources.primary.primary=true",
                "mango.persistence.datasources.primary.url=jdbc:h2:mem:mango_job_primary;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.primary.username=sa",
                "mango.persistence.datasources.primary.password=",
                "mango.persistence.datasources.job.url=jdbc:h2:mem:mango_job_job;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.job.username=sa",
                "mango.persistence.datasources.job.password=",
                "mango.persistence.modules.mango-job.datasource=job",
                "mango.persistence.flyway.enabled=true",
                "mango.persistence.mybatis-plus.tenant.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        }
)
class MangoJobMultiDataSourceIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PersistenceModuleDataSourceResolver resolver;

    @Autowired
    private IMangoJobDefinitionService jobDefinitionService;

    @Test
    void flywayAndMybatisPlus_shouldUseConfiguredJobDatasourceForMangoJobModule() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(100L, "tenant-a", "job-test", "test", "user", "tenant", 100L, "internal-admin"));

        assertThat(resolver.resolveDataSource("mango-job")).contains("job");
        assertThat(tableExists("primary", "mango_job_definition")).isFalse();
        assertThat(tableExists("job", "mango_job_definition")).isTrue();

        MangoJobDefinitionEntity entity = new MangoJobDefinitionEntity();
        entity.setId(10001L);
        entity.setTenantId("tenant-a");
        entity.setAppCode("internal-admin");
        entity.setJobCode("sync-user");
        entity.setJobName("Sync User");
        entity.setJobType("BUILTIN");
        entity.setScheduleType("MANUAL");
        entity.setStatus("DRAFT");
        entity.setEngineType("POWERJOB");
        entity.setSyncStatus("PENDING");

        jobDefinitionService.saveDefinition(entity);

        assertThat(rowCount("primary", "mango_job_definition")).isZero();
        assertThat(rowCount("job", "mango_job_definition")).isOne();
        assertThat(jobDefinitionService.findById(10001L).getJobCode()).isEqualTo("sync-user");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    private boolean tableExists(String datasource, String tableName) {
        try {
            return query(datasource, () -> jdbcTemplate().queryForObject("""
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE TABLE_NAME = ?
                    """, Integer.class, tableName.toLowerCase()) > 0);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private int rowCount(String datasource, String tableName) {
        if (!tableExists(datasource, tableName)) {
            return 0;
        }
        return query(datasource, () -> jdbcTemplate().queryForObject("SELECT COUNT(*) FROM " + tableName,
                Integer.class));
    }

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private <T> T query(String datasource, Supplier<T> supplier) {
        try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use(datasource)) {
            return supplier.get();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = MangoJobDefinitionMapper.class)
    static class TestApplication {

        @Bean
        MangoJobDataSourceRouter mangoJobDataSourceRouter(PersistenceModuleDataSourceResolver resolver) {
            return new MangoJobDataSourceRouter(resolver);
        }

        @Bean
        IMangoJobDefinitionService jobDefinitionService(MangoJobDefinitionMapper mapper,
                                                        MangoJobDataSourceRouter dataSourceRouter) {
            return new MangoJobDefinitionService(mapper, dataSourceRouter);
        }
    }

}
