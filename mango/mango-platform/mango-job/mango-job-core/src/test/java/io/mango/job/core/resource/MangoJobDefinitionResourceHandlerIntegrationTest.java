package io.mango.job.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MangoJobDefinitionResourceHandlerIntegrationTest {

    private AnnotationConfigApplicationContext context;
    private DataSource dataSource;
    private MangoJobDefinitionResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "spring.datasource.url", "jdbc:h2:mem:job_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.username", "sa",
                "spring.datasource.password", "",
                "spring.datasource.driver-class-name", "org.h2.Driver",
                "spring.flyway.enabled", "false",
                "mango.persistence.mybatis-plus.tenant.enabled", "false"
        )));
        context.register(TestConfig.class);
        context.refresh();
        dataSource = context.getBean(DataSource.class);
        handler = context.getBean(MangoJobDefinitionResourceHandler.class);
        rebuildTable();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void upsertCreatesDefaultSampleDefinitions() throws Exception {
        handler.upsert(defaultSampleManualJob());
        handler.upsert(defaultSampleCronJob());

        assertThat(count()).isEqualTo(2);
        assertThat(stringValue("job_name", "id = 2951100000000000001"))
                .isEqualTo("默认示例 手动 Probe 任务");
        assertThat(stringValue("schedule_type", "id = 2951100000000000001")).isEqualTo("MANUAL");
        assertThat(stringValue("param_value", "id = 2951100000000000001"))
                .isEqualTo("{\"source\":\"default-sample\",\"kind\":\"manual\"}");
        assertThat(stringValue("schedule_expression", "id = 2951100000000000002"))
                .isEqualTo("0 */5 * * * ?");
        assertThat(stringValue("status", "id = 2951100000000000002")).isEqualTo("DISABLED");
    }

    @Test
    void upsertCreatesPaymentBillFetchDefinition() throws Exception {
        handler.upsert(paymentBillFetchJob());

        assertThat(count()).isOne();
        assertThat(stringValue("module_code", "id = 2951200000000000001")).isEqualTo("mango-payment");
        assertThat(stringValue("job_code", "id = 2951200000000000001"))
                .isEqualTo("payment_channel_bill_fetch_yesterday");
        assertThat(stringValue("handler_name", "id = 2951200000000000001"))
                .isEqualTo("paymentChannelBillFetchJobHandler");
        assertThat(intValue("timeout_seconds", "id = 2951200000000000001")).isEqualTo(1800);
    }

    @Test
    void upsertUpdatesDefinitionButKeepsNonDraftStatus() throws Exception {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);
        execute("update mango_job_definition set status = 'ENABLED' where id = 100");

        declaration.getFields().get("jobName").setValue("更新后的任务");
        declaration.getFields().get("status").setValue("DISABLED");
        handler.upsert(declaration);

        assertThat(count()).isOne();
        assertThat(stringValue("job_name", "id = 100")).isEqualTo("更新后的任务");
        assertThat(stringValue("status", "id = 100")).isEqualTo("ENABLED");
    }

    @Test
    void disableAndDeleteMarkDefinitionStatus() throws Exception {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(stringValue("status", "id = 100")).isEqualTo("DISABLED");
        assertThat(intValue("deleted", "id = 100")).isZero();

        handler.delete(declaration);

        assertThat(stringValue("status", "id = 100")).isEqualTo("DISABLED");
        assertThat(intValue("deleted", "id = 100")).isOne();
    }

    private ResourceDeclaration defaultSampleManualJob() {
        ResourceDeclaration declaration = declaration("2951100000000000001", "job.default.sample.manual",
                "默认示例 手动 Probe 任务", "job");
        field(declaration, "jobId", ResourceFieldType.LONG, 2951100000000000001L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "appCode", ResourceFieldType.STRING, "mango-job");
        field(declaration, "ownerService", ResourceFieldType.STRING, "mango-job");
        field(declaration, "workerGroup", ResourceFieldType.STRING, "default");
        field(declaration, "moduleCode", ResourceFieldType.STRING, "mango-job");
        field(declaration, "jobCode", ResourceFieldType.STRING, "default_sample_manual_probe");
        field(declaration, "jobName", ResourceFieldType.STRING, "默认示例 手动 Probe 任务");
        field(declaration, "scheduleType", ResourceFieldType.STRING, "MANUAL");
        field(declaration, "handlerName", ResourceFieldType.STRING, "defaultSampleProbeJobHandler");
        field(declaration, "paramValue", ResourceFieldType.JSON, "{\"source\":\"default-sample\",\"kind\":\"manual\"}");
        field(declaration, "status", ResourceFieldType.STRING, "DISABLED");
        return declaration;
    }

    private ResourceDeclaration defaultSampleCronJob() {
        ResourceDeclaration declaration = declaration("2951100000000000002", "job.default.sample.cron",
                "默认示例 定时 Probe 任务", "job");
        field(declaration, "jobId", ResourceFieldType.LONG, 2951100000000000002L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "appCode", ResourceFieldType.STRING, "mango-job");
        field(declaration, "ownerService", ResourceFieldType.STRING, "mango-job");
        field(declaration, "workerGroup", ResourceFieldType.STRING, "default");
        field(declaration, "moduleCode", ResourceFieldType.STRING, "mango-job");
        field(declaration, "jobCode", ResourceFieldType.STRING, "default_sample_cron_probe");
        field(declaration, "jobName", ResourceFieldType.STRING, "默认示例 定时 Probe 任务");
        field(declaration, "scheduleType", ResourceFieldType.STRING, "CRON");
        field(declaration, "scheduleExpression", ResourceFieldType.STRING, "0 */5 * * * ?");
        field(declaration, "handlerName", ResourceFieldType.STRING, "defaultSampleProbeJobHandler");
        field(declaration, "paramValue", ResourceFieldType.JSON, "{\"source\":\"default-sample\",\"kind\":\"cron\"}");
        field(declaration, "status", ResourceFieldType.STRING, "DISABLED");
        return declaration;
    }

    private ResourceDeclaration paymentBillFetchJob() {
        ResourceDeclaration declaration = declaration("2951200000000000001",
                "payment.job.channel-bill-fetch-yesterday", "支付渠道昨日账单拉取任务", "payment");
        field(declaration, "jobId", ResourceFieldType.LONG, 2951200000000000001L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "appCode", ResourceFieldType.STRING, "mango-payment");
        field(declaration, "ownerService", ResourceFieldType.STRING, "mango-payment");
        field(declaration, "workerGroup", ResourceFieldType.STRING, "payment");
        field(declaration, "moduleCode", ResourceFieldType.STRING, "mango-payment");
        field(declaration, "jobCode", ResourceFieldType.STRING, "payment_channel_bill_fetch_yesterday");
        field(declaration, "jobName", ResourceFieldType.STRING, "支付渠道昨日账单拉取任务");
        field(declaration, "scheduleType", ResourceFieldType.STRING, "CRON");
        field(declaration, "scheduleExpression", ResourceFieldType.STRING, "0 10 1 * * ?");
        field(declaration, "handlerName", ResourceFieldType.STRING, "paymentChannelBillFetchJobHandler");
        field(declaration, "timeoutSeconds", ResourceFieldType.INT, 1800);
        field(declaration, "status", ResourceFieldType.STRING, "DISABLED");
        return declaration;
    }

    private ResourceDeclaration declaration() {
        ResourceDeclaration declaration = declaration("100", "order.job.timeout-close", "订单超时关闭", "order");
        field(declaration, "jobId", ResourceFieldType.LONG, 100L);
        field(declaration, "tenantId", ResourceFieldType.STRING, "1");
        field(declaration, "appCode", ResourceFieldType.STRING, "order");
        field(declaration, "ownerService", ResourceFieldType.STRING, "order-service");
        field(declaration, "workerGroup", ResourceFieldType.STRING, "order-worker");
        field(declaration, "moduleCode", ResourceFieldType.STRING, "order");
        field(declaration, "jobCode", ResourceFieldType.STRING, "order_timeout_close");
        field(declaration, "jobName", ResourceFieldType.STRING, "订单超时关闭");
        field(declaration, "scheduleType", ResourceFieldType.STRING, "CRON");
        field(declaration, "scheduleExpression", ResourceFieldType.STRING, "0 */10 * * * ?");
        field(declaration, "handlerName", ResourceFieldType.STRING, "orderTimeoutCloseJobHandler");
        field(declaration, "timeoutSeconds", ResourceFieldType.INT, 600);
        field(declaration, "status", ResourceFieldType.STRING, "DISABLED");
        return declaration;
    }

    private ResourceDeclaration declaration(String id, String bizKey, String name, String moduleCode) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(id);
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.JOB_DEFINITION);
        declaration.setModuleCode(moduleCode);
        declaration.setBizKey(bizKey);
        declaration.setName(name);
        declaration.setTargetModule("job");
        declaration.setFields(new LinkedHashMap<>());
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTable() throws Exception {
        execute("drop table if exists mango_job_definition");
        execute("""
                create table mango_job_definition (
                    id bigint not null,
                    tenant_id varchar(64) not null,
                    app_code varchar(128) not null,
                    owner_service varchar(128) not null default '',
                    worker_group varchar(128) not null default '',
                    module_code varchar(128),
                    job_code varchar(128) not null,
                    job_name varchar(128) not null,
                    job_type varchar(32) not null,
                    schedule_type varchar(32) not null,
                    schedule_expression varchar(256),
                    handler_name varchar(256),
                    handler_version varchar(64),
                    param_schema text,
                    param_value text,
                    misfire_strategy varchar(64),
                    concurrency_policy varchar(64),
                    timeout_seconds int,
                    retry_policy text,
                    timezone varchar(64) not null default 'Asia/Shanghai',
                    max_retry_count int not null default 0,
                    version int not null default 0,
                    deleted tinyint not null default 0,
                    status varchar(32) not null,
                    engine_type varchar(32) not null,
                    engine_app_id varchar(128),
                    engine_job_id varchar(128),
                    sync_status varchar(32) not null,
                    sync_error varchar(1024),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_job_tenant_app_code (tenant_id, app_code, job_code)
                )
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from mango_job_definition")) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from mango_job_definition where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from mango_job_definition where " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Configuration
    @Import({
            DataSourceAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            MangoJobDefinitionResourceHandler.class
    })
    @MapperScan(basePackageClasses = MangoJobDefinitionMapper.class)
    static class TestConfig {
    }
}
