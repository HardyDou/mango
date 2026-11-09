package io.mango.integration.core.scenario;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.crypto.starter.CryptoAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.command.IntegrationInstanceStatusCommand;
import io.mango.integration.api.vo.IntegrationCredentialFieldVO;
import io.mango.integration.api.spi.IntegrationDeclarationProvider;
import io.mango.integration.api.vo.IntegrationInstanceDeclarationVO;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.integration.core.service.IIntegrationCredentialService;
import io.mango.integration.core.service.IIntegrationInstanceService;
import io.mango.integration.core.service.impl.IntegrationCallRecorderImpl;
import io.mango.integration.core.service.impl.IntegrationConnectorService;
import io.mango.integration.core.service.impl.IntegrationCredentialService;
import io.mango.integration.core.service.impl.IntegrationDeclarationProviderImpl;
import io.mango.integration.core.service.impl.IntegrationInstanceService;
import io.mango.integration.support.auth.IntegrationAuthConfig;
import io.mango.integration.support.auth.IntegrationOutboundAuth;
import io.mango.integration.support.auth.IntegrationTokenCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        CryptoAutoConfiguration.class,
        IntegrationRealScenarioTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration_scenario;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mango.crypto.sm4.secret-key=MDEyMzQ1Njc4OWFiY2RlZg==",
        "mango.crypto.sm4.mode=CBC"
})
@DisplayName("企业微信消息真实场景测试")
class IntegrationRealScenarioTest {

    private static final String CONNECTOR_CODE = "wecom-message";
    private static final String CREDENTIAL_CODE = "wecom-real-credential";
    private static final String INSTANCE_CODE = "wecom-real-instance";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IIntegrationConnectorService connectorService;

    @Autowired
    private IIntegrationCredentialService credentialService;

    @Autowired
    private IIntegrationInstanceService instanceService;

    @Autowired
    private IntegrationDeclarationProvider declarationProvider;

    @Autowired
    private IntegrationCallRecorderImpl callRecorder;

    private HttpServer server;
    private final AtomicInteger tokenRequests = new AtomicInteger();
    private volatile String capturedAuthorization;
    private volatile String capturedBody;

    @BeforeEach
    void setUp() throws IOException {
        resetSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, 1001L, "default", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        startMockWecom();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("按声明配置获取令牌并发送企业微信消息")
    void sendsMessageWithDeclaredCredentialAndToken() throws Exception {
        createConnectorAndCredential();
        createInstance();

        IntegrationInstanceDeclarationVO declaration = declarationProvider.instance(INSTANCE_CODE);
        List<IntegrationCredentialFieldVO> fields = declarationProvider.credentials(declaration.getCredentialCode());
        Map<String, String> credentials = fields.stream()
                .collect(Collectors.toMap(IntegrationCredentialFieldVO::getFieldCode,
                        IntegrationCredentialFieldVO::getFieldValue));
        assertThat(credentials.get("corpSecret")).isEqualTo("real-corp-secret");

        IntegrationAuthConfig config = IntegrationAuthConfig.of(
                declarationProvider.connectorAuthSchema(declaration.getConnectorCode()), credentials);
        URI sendUri = URI.create(baseUrl() + "/cgi-bin/message/send");
        String payload = "{\"touser\":\"zhangsan\",\"msgtype\":\"text\",\"text\":{\"content\":\"hello\"}}";
        HttpRequest.Builder builder = HttpRequest.newBuilder(sendUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));

        IntegrationOutboundAuth.of(config, new IntegrationTokenCache()).apply(builder, "POST", sendUri);
        HttpResponse<String> response = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build()
                .send(builder.build(), HttpResponse.BodyHandlers.ofString());

        callRecorder.record(io.mango.integration.support.client.IntegrationCallRecord.builder()
                .instanceCode(INSTANCE_CODE)
                .connectorCode(CONNECTOR_CODE)
                .method("POST")
                .path("/cgi-bin/message/send")
                .statusCode(response.statusCode())
                .elapsedMs(12L)
                .success(response.statusCode() == 200)
                .message("调用成功")
                .build());
        Long callLogs = jdbcTemplate.queryForObject(
                "select count(*) from integration_call_log where instance_code = ?", Long.class, INSTANCE_CODE);
        assertThat(callLogs).isEqualTo(1L);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(capturedAuthorization).isEqualTo("Bearer real-access-token");
        assertThat(capturedBody).contains("\"touser\":\"zhangsan\"");
        assertThat(tokenRequests.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("停用连接器后实例不能启用")
    void disabledConnectorBlocksInstanceEnable() {
        createConnectorAndCredential();
        createInstance();

        IntegrationConnectorCommand disable = connectorCommand(0);
        connectorService.save(disable);

        IntegrationInstanceStatusCommand command = new IntegrationInstanceStatusCommand();
        command.setInstanceCode(INSTANCE_CODE);
        command.setStatus(1);
        assertThatThrownBy(() -> instanceService.changeStatus(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("连接器已停用");
    }

    @Test
    @DisplayName("配置值不符合规范时实例保存失败")
    void invalidConfigRejected() {
        createConnectorAndCredential();

        IntegrationInstanceCommand command = instanceCommand("{\"apiBaseUrl\":\"ftp://invalid\"}", 1);
        assertThatThrownBy(() -> instanceService.save(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置格式不符合要求");
    }

    private void createConnectorAndCredential() {
        assertThat(connectorService.save(connectorCommand(1))).isTrue();

        IntegrationCredentialCommand credential = new IntegrationCredentialCommand();
        credential.setCredentialCode(CREDENTIAL_CODE);
        credential.setConnectorCode(CONNECTOR_CODE);
        credential.setFieldsJson("{\"corpId\":\"wx-real-corp\",\"agentId\":\"100001\",\"corpSecret\":\"real-corp-secret\"}");
        assertThat(credentialService.save(credential)).isTrue();
    }

    private void createInstance() {
        assertThat(instanceService.save(instanceCommand(
                "{\"apiBaseUrl\":\"" + baseUrl() + "\"}", 1))).isTrue();
    }

    private IntegrationConnectorCommand connectorCommand(int status) {
        IntegrationConnectorCommand command = new IntegrationConnectorCommand();
        command.setConnectorCode(CONNECTOR_CODE);
        command.setConnectorName("企业微信应用消息");
        command.setCategory("MESSAGE");
        command.setProvider("WECOM");
        command.setStatus(status);
        command.setConfigSchema("""
                {"type":"object","required":["apiBaseUrl"],"properties":{
                  "apiBaseUrl":{"type":"string","title":"接口地址","pattern":"^https?://.*"}}}
                """);
        command.setAuthSchema("""
                {"authMode":"OAUTH2","params":{
                  "tokenUrl":"%s/cgi-bin/gettoken",
                  "clientIdParam":"corpid","clientSecretParam":"corpsecret",
                  "clientIdField":"corpId","clientSecretField":"corpSecret",
                  "tokenField":"access_token","expiresField":"expires_in"},
                 "fields":[
                  {"fieldCode":"corpId","label":"企业 ID","type":"STRING","required":true},
                  {"fieldCode":"agentId","label":"应用 AgentId","type":"STRING","required":true},
                  {"fieldCode":"corpSecret","label":"应用 Secret","type":"SECRET","required":true}]}
                """.formatted(baseUrl()));
        command.setCapabilitiesJson("""
                [{"code":"message.send","name":"发送应用消息","description":"向成员发送企业微信应用消息"}]
                """);
        command.setTags(List.of("企微", "消息"));
        return command;
    }

    private IntegrationInstanceCommand instanceCommand(String configValuesJson, int status) {
        IntegrationInstanceCommand command = new IntegrationInstanceCommand();
        command.setInstanceCode(INSTANCE_CODE);
        command.setInstanceName("企业微信消息实例");
        command.setConnectorCode(CONNECTOR_CODE);
        command.setEnvironment("PROD");
        command.setConfigValuesJson(configValuesJson);
        command.setCredentialCode(CREDENTIAL_CODE);
        command.setCapabilitiesJson("[\"message.send\"]");
        command.setStatus(status);
        command.setOwner("平台组");
        return command;
    }

    private void startMockWecom() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cgi-bin/gettoken", exchange -> {
            tokenRequests.incrementAndGet();
            byte[] body = "{\"errcode\":0,\"access_token\":\"real-access-token\",\"expires_in\":7200}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/cgi-bin/message/send", exchange -> {
            capturedAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
            capturedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = "{\"errcode\":0,\"errmsg\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists integration_connector");
        jdbcTemplate.execute("drop table if exists integration_credential_rotation");
        jdbcTemplate.execute("drop table if exists integration_credential_access_log");
        jdbcTemplate.execute("drop table if exists integration_call_log");
        jdbcTemplate.execute("""
                create table integration_call_log (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    instance_code varchar(64) not null,
                    connector_code varchar(64) not null,
                    http_method varchar(16) not null,
                    path varchar(500) not null,
                    status_code int not null default 0,
                    elapsed_ms bigint not null default 0,
                    success tinyint not null default 0,
                    message varchar(500),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table integration_credential_rotation (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    credential_code varchar(64) not null,
                    version int not null,
                    field_codes varchar(500),
                    operator_id bigint,
                    remark varchar(500),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table integration_credential_access_log (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    credential_code varchar(64) not null,
                    action varchar(32) not null,
                    field_codes varchar(500),
                    operator_id bigint,
                    source varchar(64),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("drop table if exists integration_credential");
        jdbcTemplate.execute("drop table if exists integration_instance");
        jdbcTemplate.execute("""
                create table integration_connector (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    connector_code varchar(64) not null,
                    connector_name varchar(100) not null,
                    category varchar(32),
                    provider varchar(64),
                    status tinyint not null default 1,
                    source varchar(16) not null default 'MANUAL',
                    revision int not null default 0,
                    config_schema clob,
                    auth_schema clob,
                    capabilities_json clob,
                    tags varchar(255),
                    docs_url varchar(500),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table integration_credential (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    credential_code varchar(64) not null,
                    connector_code varchar(64) not null,
                    fields_json clob,
                    field_versions_json clob,
                    rotated_at timestamp,
                    expires_at timestamp,
                    status varchar(32) not null default 'ACTIVE',
                    revision int not null default 0,
                    remark varchar(500),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table integration_instance (
                    id bigint primary key,
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    instance_code varchar(64) not null,
                    instance_name varchar(100) not null,
                    connector_code varchar(64) not null,
                    environment varchar(16) not null default 'PROD',
                    config_values_json clob,
                    credential_code varchar(64),
                    capabilities_json varchar(2048),
                    status tinyint not null default 0,
                    revision int not null default 0,
                    owner varchar(64),
                    remark varchar(500),
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    @Configuration
    @MapperScan("io.mango.integration.core.mapper")
    @Import({
            IntegrationConnectorService.class,
            IntegrationCredentialService.class,
            IntegrationInstanceService.class,
            IntegrationDeclarationProviderImpl.class,
            IntegrationCallRecorderImpl.class
    })
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        io.mango.integration.core.support.IntegrationTestProperties integrationTestProperties() {
            return new io.mango.integration.core.support.IntegrationTestProperties();
        }
    }
}
