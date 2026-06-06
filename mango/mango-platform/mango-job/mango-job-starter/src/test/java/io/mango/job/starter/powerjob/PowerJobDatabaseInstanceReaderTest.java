package io.mango.job.starter.powerjob;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.mango.infra.persistence.starter.datasource.PersistenceDataSourceContext;
import io.mango.infra.persistence.starter.datasource.PersistenceModuleDataSourceResolver;
import io.mango.job.core.service.impl.MangoJobDataSourceRouter;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PowerJobDatabaseInstanceReaderTest.TestApplication.class)
@TestPropertySource(properties = "mango.job.powerjob.native-log.datasource=powerjob")
class PowerJobDatabaseInstanceReaderTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PowerJobInstanceInfoMapper instanceInfoMapper;

    @Autowired
    private PowerJobProperties properties;

    @Autowired
    private MangoJobDataSourceRouter dataSourceRouter;

    private PowerJobDatabaseInstanceReader reader;

    @BeforeEach
    void setUp() throws Exception {
        reader = new PowerJobDatabaseInstanceReader(instanceInfoMapper, dataSourceRouter, properties);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists instance_info");
            statement.execute("""
                    create table instance_info (
                      id bigint auto_increment primary key,
                      instance_id bigint,
                      app_id bigint,
                      job_id bigint,
                      expected_trigger_time bigint,
                      actual_trigger_time bigint,
                      finished_time bigint,
                      status int,
                      task_tracker_address varchar(255),
                      outer_key varchar(255),
                      result clob,
                      gmt_create timestamp,
                      gmt_modified timestamp
                    )
                    """);
        }
    }

    @Test
    void shouldReadRecentPowerJobInstancesThroughMybatisPlusMapper() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insertInstance(81001L, 90001L, now.minusMinutes(3), "127.0.0.1:28888");
        insertInstance(81002L, 90001L, now.minusMinutes(1), "127.0.0.1:28889");
        insertInstance(82001L, 90002L, now.minusMinutes(1), "127.0.0.1:28890");

        List<PowerJobInstanceInfoEntity> records = reader.readRecentInstances(
                90001L, now.minusMinutes(5), now.plusMinutes(1), 2);

        assertThat(records).extracting(PowerJobInstanceInfoEntity::getInstanceId)
                .containsExactly(81002L, 81001L);
        assertThat(records).extracting(PowerJobInstanceInfoEntity::getTaskTrackerAddress)
                .containsExactly("127.0.0.1:28889", "127.0.0.1:28888");
        assertThat(PersistenceDataSourceContext.current()).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenJobIdMissing() {
        assertThat(reader.readRecentInstances(null, null, null, 20)).isEmpty();
    }

    private void insertInstance(Long instanceId,
                                Long jobId,
                                LocalDateTime triggerTime,
                                String workerAddress) throws Exception {
        long millis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into instance_info(
                       instance_id, app_id, job_id, expected_trigger_time, actual_trigger_time,
                       finished_time, status, task_tracker_address, gmt_create, gmt_modified
                     )
                     values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                     """)) {
            statement.setLong(1, instanceId);
            statement.setLong(2, 10001L);
            statement.setLong(3, jobId);
            statement.setLong(4, millis);
            statement.setLong(5, millis + 10);
            statement.setLong(6, millis + 100);
            statement.setInt(7, 5);
            statement.setString(8, workerAddress);
            statement.executeUpdate();
        }
    }

    @Configuration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = PowerJobInstanceInfoMapper.class)
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("powerjob_instance_reader;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
                    .build();
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                            MybatisPlusInterceptor tenantInterceptor) throws Exception {
            MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.addMapper(PowerJobInstanceInfoMapper.class);
            factoryBean.setDataSource(dataSource);
            factoryBean.setConfiguration(configuration);
            factoryBean.setPlugins(new Interceptor[]{tenantInterceptor});
            return factoryBean.getObject();
        }

        @Bean
        SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
            return new SqlSessionTemplate(sqlSessionFactory);
        }

        @Bean
        MybatisPlusInterceptor tenantInterceptor() {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
                @Override
                public LongValue getTenantId() {
                    return new LongValue(1L);
                }
            }));
            return interceptor;
        }

        @Bean
        PersistenceModuleDataSourceResolver persistenceModuleDataSourceResolver() {
            return moduleName -> Optional.of("job");
        }

        @Bean
        MangoJobDataSourceRouter mangoJobDataSourceRouter(
                ObjectProvider<PersistenceModuleDataSourceResolver> resolverProvider) {
            return new MangoJobDataSourceRouter(resolverProvider);
        }
    }
}
