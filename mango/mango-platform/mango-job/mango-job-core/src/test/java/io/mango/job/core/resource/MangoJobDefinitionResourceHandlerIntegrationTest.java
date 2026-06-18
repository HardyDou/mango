package io.mango.job.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.job.core.mapper.MangoJobDefinitionMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        MangoJobDefinitionResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:job_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class MangoJobDefinitionResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MangoJobDefinitionResourceHandler handler;

    @BeforeEach
    void setUp() {
        rebuildTable();
    }

    @Test
    void jobYamlCreatesDefaultSampleDefinitions() throws Exception {
        List<ResourceDeclaration> declarations = loadDeclarations(
                "../mango-job-starter/src/main/resources/META-INF/mango/resources/job-common-definition.yml");

        for (ResourceDeclaration declaration : declarations) {
            handler.upsert(declaration);
        }

        assertThat(declarations).hasSize(2);
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
    void paymentYamlCreatesBillFetchDefinition() throws Exception {
        List<ResourceDeclaration> declarations = loadDeclarations(
                "../../mango-payment/mango-payment-starter/src/main/resources/META-INF/mango/resources/payment-common-job.yml");

        for (ResourceDeclaration declaration : declarations) {
            handler.upsert(declaration);
        }

        assertThat(declarations).hasSize(1);
        assertThat(count()).isOne();
        assertThat(stringValue("module_code", "id = 2951200000000000001")).isEqualTo("mango-payment");
        assertThat(stringValue("job_code", "id = 2951200000000000001"))
                .isEqualTo("payment_channel_bill_fetch_yesterday");
        assertThat(stringValue("handler_name", "id = 2951200000000000001"))
                .isEqualTo("paymentChannelBillFetchJobHandler");
        assertThat(intValue("timeout_seconds", "id = 2951200000000000001")).isEqualTo(1800);
    }

    @Test
    void upsertUpdatesDefinitionButKeepsNonDraftStatus() {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);
        jdbcTemplate.update("update mango_job_definition set status = 'ENABLED' where id = 100");

        declaration.getFields().get("jobName").setValue("更新后的任务");
        declaration.getFields().get("status").setValue("DISABLED");
        handler.upsert(declaration);

        assertThat(count()).isOne();
        assertThat(stringValue("job_name", "id = 100")).isEqualTo("更新后的任务");
        assertThat(stringValue("status", "id = 100")).isEqualTo("ENABLED");
    }

    @Test
    void disableAndDeleteMarkDefinitionStatus() {
        ResourceDeclaration declaration = declaration();
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(stringValue("status", "id = 100")).isEqualTo("DISABLED");
        assertThat(intValue("deleted", "id = 100")).isZero();

        handler.delete(declaration);

        assertThat(stringValue("status", "id = 100")).isEqualTo("DISABLED");
        assertThat(intValue("deleted", "id = 100")).isOne();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceDeclaration> loadDeclarations(String filePath) throws Exception {
        Path path = Path.of(filePath);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        MapNode root = objectMapper.readValue(path.toFile(), MapNode.class);
        ResourceNode resource = root.mango().resource();
        List<ResourceDeclaration> declarations =
                ((List<ResourceDeclaration>) resource.declarations().get(ResourceTypes.JOB_DEFINITION));
        for (ResourceDeclaration declaration : declarations) {
            declaration.setResourceType(ResourceTypes.JOB_DEFINITION);
            declaration.setModuleCode(resource.moduleCode());
            declaration.setModuleName(resource.moduleName());
            declaration.setSource(path.toString());
        }
        return declarations;
    }

    private ResourceDeclaration declaration() {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId("100");
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.JOB_DEFINITION);
        declaration.setModuleCode("order");
        declaration.setBizKey("order.job.timeout-close");
        declaration.setName("订单超时关闭");
        declaration.setTargetModule("job");
        declaration.setFields(new LinkedHashMap<>());
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

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTable() {
        jdbcTemplate.execute("drop table if exists mango_job_definition");
        jdbcTemplate.execute("""
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

    private long count() {
        return jdbcTemplate.queryForObject("select count(*) from mango_job_definition", Long.class);
    }

    private String stringValue(String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from mango_job_definition where " + whereClause,
                String.class);
    }

    private int intValue(String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject(
                "select " + columnName + " from mango_job_definition where " + whereClause, Integer.class);
        return value == null ? 0 : value;
    }

    @Configuration
    @Import(MangoJobDefinitionResourceHandler.class)
    @MapperScan(basePackageClasses = MangoJobDefinitionMapper.class)
    static class TestConfig {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapNode(MangoNode mango) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MangoNode(ResourceNode resource) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResourceNode(@JsonAlias("module-code") String moduleCode,
                        @JsonAlias("module-name") String moduleName,
                        Map<String, List<ResourceDeclaration>> declarations) {
    }
}
