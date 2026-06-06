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
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PowerJobDatabaseNativeLogReaderTest.TestApplication.class)
@TestPropertySource(properties = "mango.job.powerjob.native-log.datasource=powerjob")
class PowerJobDatabaseNativeLogReaderTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PowerJobFileMapper fileMapper;

    @Autowired
    private PowerJobProperties properties;

    @Autowired
    private MangoJobDataSourceRouter dataSourceRouter;

    private PowerJobDatabaseNativeLogReader reader;

    @BeforeEach
    void setUp() throws Exception {
        reader = new PowerJobDatabaseNativeLogReader(fileMapper, dataSourceRouter, properties);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("drop table if exists powerjob_files");
            statement.execute("""
                    create table powerjob_files (
                      id bigint auto_increment primary key,
                      bucket varchar(255) not null,
                      name varchar(255) not null,
                      version varchar(255) not null,
                      meta varchar(255),
                      length bigint not null,
                      status int not null,
                      data blob not null,
                      extra varchar(255),
                      gmt_create timestamp not null,
                      gmt_modified timestamp
                    )
                    """);
        }
    }

    @Test
    void shouldReadPowerJobNativeLogThroughMybatisPlusMapper() throws Exception {
        String log = "2026-06-06 10:07:00 INFO Mango Job handler output: {\"rows\":3}";
        insertLog(80001L, log);

        PowerJobNativeLog result = reader.readInstanceLog(80001L);

        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getContent()).contains("Mango Job handler output").contains("\"rows\":3");
        assertThat(PersistenceDataSourceContext.current()).isEmpty();
    }

    @Test
    void shouldReturnUnavailableWhenArchivedLogMissing() {
        PowerJobNativeLog result = reader.readInstanceLog(80002L);

        assertThat(result.isAvailable()).isFalse();
        assertThat(result.getErrorSummary()).contains("尚未归档");
        assertThat(result.getErrorSummary()).doesNotContain("原生日志");
        assertThat(PersistenceDataSourceContext.current()).isEmpty();
    }

    private void insertLog(Long instanceId, String content) throws Exception {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into powerjob_files(bucket, name, version, meta, length, status, data, gmt_create)
                     values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
                     """)) {
            statement.setString(1, "log");
            statement.setString(2, "oms-" + instanceId + ".log");
            statement.setString(3, "mu");
            statement.setString(4, "{}");
            statement.setLong(5, bytes.length);
            statement.setInt(6, 1);
            statement.setBytes(7, bytes);
            statement.executeUpdate();
        }
    }

    @Configuration
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @MapperScan(basePackageClasses = PowerJobFileMapper.class)
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .setName("powerjob_native_log;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
                    .build();
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor tenantInterceptor) throws Exception {
            MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.addMapper(PowerJobFileMapper.class);
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
