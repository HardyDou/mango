package io.mango.infra.persistence.starter.datasource;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = PersistenceMybatisPlusMultiDataSourceIntegrationTest.TestApplication.class,
        properties = {
                "mango.persistence.datasources.primary.primary=true",
                "mango.persistence.datasources.primary.url=jdbc:h2:mem:mybatis_primary;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.primary.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.primary.username=sa",
                "mango.persistence.datasources.primary.password=",
                "mango.persistence.datasources.job.url=jdbc:h2:mem:mybatis_job;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "mango.persistence.datasources.job.driver-class-name=org.h2.Driver",
                "mango.persistence.datasources.job.username=sa",
                "mango.persistence.datasources.job.password=",
                "mango.persistence.flyway.enabled=false",
                "mango.persistence.mybatis-plus.tenant.enabled=false",
                "mango.persistence.schema-validation.enabled=false"
        }
)
class PersistenceMybatisPlusMultiDataSourceIntegrationTest {

    @Autowired
    private PrimaryMarkerService primaryMarkerService;

    @Autowired
    private JobMarkerService jobMarkerService;

    @Autowired
    private PersistenceModuleDataSourceResolver resolver;

    @BeforeEach
    void setUp() {
        primaryMarkerService.clear();
        jobMarkerService.clear();
    }

    @Test
    void mapper_shouldRouteByModuleDefaultDatasource() {
        assertThat(resolver.resolveDataSource("mango-job")).contains("job");

        primaryMarkerService.save(1L, "primary");
        jobMarkerService.save(1L, "job");

        assertThat(primaryMarkerService.nameOf(1L)).isEqualTo("primary");
        assertThat(jobMarkerService.nameOf(1L)).isEqualTo("job");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = MarkerMapper.class, annotationClass = Mapper.class)
    static class TestApplication {

        @Bean
        PersistenceModuleDataSourceDefaults persistenceModuleDataSourceDefaults() {
            return new PersistenceModuleDataSourceDefaults(Map.of("mango-job", "job"));
        }

        @Bean
        InitializingBean schemaInitializer(DataSource dataSource) {
            return () -> {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                createMarkerTable(jdbcTemplate);
                try (PersistenceDataSourceContext.Scope ignored = PersistenceDataSourceContext.use("job")) {
                    createMarkerTable(jdbcTemplate);
                }
            };
        }

        @Bean
        PrimaryMarkerService primaryMarkerService(MarkerMapper mapper) {
            return new PrimaryMarkerService(mapper);
        }

        @Bean
        JobMarkerService jobMarkerService(MarkerMapper mapper) {
            return new JobMarkerService(mapper);
        }

        private static void createMarkerTable(JdbcTemplate jdbcTemplate) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS mds_marker");
            jdbcTemplate.execute("""
                    CREATE TABLE mds_marker (
                        id BIGINT PRIMARY KEY,
                        name VARCHAR(64)
                    )
                    """);
        }
    }

    @Mapper
    interface MarkerMapper extends BaseMapper<MarkerEntity> {
    }

    static class PrimaryMarkerService {

        private final MarkerMapper mapper;

        PrimaryMarkerService(MarkerMapper mapper) {
            this.mapper = mapper;
        }

        public void save(Long id, String name) {
            mapper.insert(new MarkerEntity(id, name));
        }

        public String nameOf(Long id) {
            return mapper.selectById(id).getName();
        }

        public void clear() {
            mapper.deleteById(1L);
        }
    }

    @PersistenceDataSource("job")
    static class JobMarkerService {

        private final MarkerMapper mapper;

        JobMarkerService(MarkerMapper mapper) {
            this.mapper = mapper;
        }

        public void save(Long id, String name) {
            mapper.insert(new MarkerEntity(id, name));
        }

        public String nameOf(Long id) {
            return mapper.selectById(id).getName();
        }

        public void clear() {
            mapper.deleteById(1L);
        }
    }

    @TableName("mds_marker")
    static class MarkerEntity {

        @TableId
        private Long id;

        private String name;

        MarkerEntity() {
        }

        MarkerEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
