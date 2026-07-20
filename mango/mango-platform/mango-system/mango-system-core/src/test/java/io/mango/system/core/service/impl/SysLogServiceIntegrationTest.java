package io.mango.system.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.system.api.command.RecordOperationLogCommand;
import io.mango.system.core.mapper.SysOperationLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        PersistenceAuditAutoConfiguration.class,
        SysLogServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:system_operation_log;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mango.persistence.schema-validation.enabled=false"
})
class SysLogServiceIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SysLogService logService;

    @BeforeEach
    void setUp() throws Exception {
        MangoContextHolder.clear();
        rebuildOperationLogTable();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void recordOperationLogWithoutTenantUsesPlatformTenant() throws Exception {
        RecordOperationLogCommand command = operationLog(null, "平台操作");

        assertThat(logService.recordOperationLog(command)).isTrue();

        assertThat(singleValue("tenant_id", "operation = '平台操作'"))
                .isEqualTo("default");
    }

    @Test
    void recordOperationLogKeepsExplicitTenant() throws Exception {
        RecordOperationLogCommand command = operationLog(" tenant-a ", "租户操作");

        assertThat(logService.recordOperationLog(command)).isTrue();

        assertThat(singleValue("tenant_id", "operation = '租户操作'"))
                .isEqualTo("tenant-a");
    }

    private RecordOperationLogCommand operationLog(String tenantId, String operation) {
        RecordOperationLogCommand command = new RecordOperationLogCommand();
        command.setTenantId(tenantId);
        command.setModule("system");
        command.setOperation(operation);
        command.setMethod("POST");
        command.setHandlerMethod("DemoController.save");
        command.setUrl("/demo");
        command.setStatus(1);
        command.setDuration(10L);
        return command;
    }

    private void rebuildOperationLogTable() throws Exception {
        execute("drop table if exists sys_operation_log");
        execute("""
                create table sys_operation_log (
                    id bigint not null primary key,
                    user_id bigint,
                    username varchar(64),
                    module varchar(64),
                    operation varchar(100),
                    method varchar(200),
                    handler_method varchar(200),
                    url varchar(500),
                    params clob,
                    result clob,
                    status tinyint,
                    error_msg varchar(500),
                    duration bigint,
                    ip varchar(128),
                    location varchar(255),
                    operate_time timestamp,
                    tenant_id varchar(64) not null,
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp not null,
                    updated_by bigint,
                    updated_at timestamp not null
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String singleValue(String column, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + column + " from sys_operation_log where " + whereClause)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    @Configuration
    @Import(SysLogService.class)
    @MapperScan(basePackageClasses = SysOperationLogMapper.class)
    static class TestConfig {
    }
}
