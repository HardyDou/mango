package io.mango.integration.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.integration.api.command.IntegrationConnectorCommand;
import io.mango.integration.api.command.IntegrationCredentialCommand;
import io.mango.integration.api.command.IntegrationCredentialRotateCommand;
import io.mango.integration.api.command.IntegrationInstanceCommand;
import io.mango.integration.api.enums.IntegrationCredentialStatus;
import io.mango.integration.api.vo.IntegrationCredentialVO;
import io.mango.integration.api.vo.IntegrationInstanceVO;
import io.mango.integration.core.mapper.IntegrationCredentialMapper;
import io.mango.integration.core.entity.IntegrationConnectorEntity;
import io.mango.integration.core.service.IIntegrationConnectorService;
import io.mango.integration.core.mapper.IntegrationInstanceMapper;
import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.crypto.starter.CryptoAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        CryptoAutoConfiguration.class,
        IntegrationModelServiceTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration_model;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false",
        "mango.crypto.sm4.secret-key=MDEyMzQ1Njc4OWFiY2RlZg==",
        "mango.crypto.sm4.mode=CBC"
})
@DisplayName("集成应用连接器模型集成测试")
class IntegrationModelServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IntegrationCredentialMapper credentialMapper;

    @Autowired
    private IntegrationInstanceMapper instanceMapper;

    @Autowired
    private IIntegrationConnectorService connectorService;

    @Autowired
    private io.mango.integration.core.mapper.IntegrationConnectorMapper connectorMapper;

    @Autowired
    private IntegrationCredentialService credentialService;

    @Autowired
    private IntegrationInstanceService instanceService;

    @Autowired
    private IntegrationDeclarationProviderImpl declarationProvider;

    @BeforeEach
    void setUp() {
        resetSchema();
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, 1001L, "default", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        seedConnector("wecom-message");
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("凭证保存后敏感字段加密，列表只返回掩码")
    void credentialSaveEncryptsSecretFields() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));

        String stored = jdbcTemplate.queryForObject(
                "select fields_json from integration_credential where credential_code = 'wecom-corp'",
                String.class);
        assertThat(stored).doesNotContain("s3cr3t");
        assertThat(stored).contains("enc:");

        IntegrationCredentialVO vo = credentialService.list("wecom-message").get(0);
        assertThat(vo.getMaskedFieldsJson()).contains("corpId=wx123");
        assertThat(vo.getMaskedFieldsJson()).contains("corpSecret=******");
        assertThat(vo.getMaskedFieldsJson()).doesNotContain("s3cr3t");
        assertThat(vo.getStatus()).isEqualTo(IntegrationCredentialStatus.ACTIVE);

        Map<String, String> resolved = credentialService.resolve("wecom-corp");
        assertThat(resolved.get("corpSecret")).isEqualTo("s3cr3t");
    }

    @Test
    @DisplayName("凭证校验必填字段、未声明字段，并支持轮换递增版本")
    void credentialValidatesAndRotates() {
        assertThatThrownBy(() -> credentialService.save(credentialCommand("bad", "{\"corpId\":\"wx123\"}")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("凭证缺少必填字段");

        assertThatThrownBy(() -> credentialService.save(credentialCommand("bad2",
                "{\"corpId\":\"wx123\",\"corpSecret\":\"s\",\"unknown\":\"x\"}")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未声明的字段");

        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));
        IntegrationCredentialRotateCommand rotate = new IntegrationCredentialRotateCommand();
        rotate.setCredentialCode("wecom-corp");
        rotate.setFieldsJson("{\"corpSecret\":\"rotated\"}");
        credentialService.rotate(rotate);

        assertThat(credentialService.resolve("wecom-corp").get("corpSecret")).isEqualTo("rotated");
        assertThat(credentialService.resolve("wecom-corp").get("corpId")).isEqualTo("wx123");
    }

    @Test
    @DisplayName("实例按配置规范校验必填项与未声明字段")
    void instanceValidatesConfigValues() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));

        IntegrationInstanceCommand missing = instanceCommand("wecom-main", "{}");
        assertThatThrownBy(() -> instanceService.save(missing))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("缺少必填配置");

        IntegrationInstanceCommand unknown = instanceCommand("wecom-main", "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\",\"nope\":1}");
        assertThatThrownBy(() -> instanceService.save(unknown))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未声明的字段");
    }

    @Test
    @DisplayName("实例能力必须来自连接器声明，启用前必须绑定凭证")
    void instanceValidatesCapabilitiesAndCredential() {
        IntegrationInstanceCommand badCapability = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\"}");
        badCapability.setCapabilitiesJson("[\"message.send\",\"unknown.capability\"]");
        badCapability.setStatus(0);
        assertThatThrownBy(() -> instanceService.save(badCapability))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("连接器未声明能力");

        IntegrationInstanceCommand enableWithoutCredential = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\"}");
        enableWithoutCredential.setStatus(1);
        assertThatThrownBy(() -> instanceService.save(enableWithoutCredential))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("启用实例前必须绑定凭证");
    }

    @Test
    @DisplayName("配置规范支持枚举、正则与长度校验")
    void instanceValidatesFieldRules() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));
        IntegrationInstanceCommand invalidEnum = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\",\"mode\":\"BAD\"}");
        assertThatThrownBy(() -> instanceService.save(invalidEnum))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置取值不在允许范围内");

        IntegrationInstanceCommand invalidPattern = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"ftp://x\",\"mode\":\"API\"}");
        assertThatThrownBy(() -> instanceService.save(invalidPattern))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配置格式不符合要求");
    }

    @Test
    @DisplayName("并发修改连接器时后写者失败")
    void concurrentConnectorUpdateRejected() {
        IntegrationConnectorCommand create = new IntegrationConnectorCommand();
        create.setConnectorCode("wecom-concurrent");
        create.setConnectorName("并发连接器");
        create.setStatus(1);
        create.setConfigSchema("{\"type\":\"object\",\"properties\":{}}");
        create.setAuthSchema("{\"authMode\":\"NONE\",\"fields\":[]}");
        connectorService.save(create);

        IntegrationConnectorEntity stale = connectorMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IntegrationConnectorEntity>()
                        .eq(IntegrationConnectorEntity::getConnectorCode, "wecom-concurrent"));
        int staleRevision = stale.getRevision() == null ? 0 : stale.getRevision();
        jdbcTemplate.update("update integration_connector set revision = revision + 1 where connector_code = 'wecom-concurrent'");

        stale.setConnectorName("并发修改");
        stale.setRevision(staleRevision + 1);
        int updated = connectorMapper.update(stale,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<IntegrationConnectorEntity>()
                        .eq(IntegrationConnectorEntity::getId, stale.getId())
                        .eq(IntegrationConnectorEntity::getRevision, staleRevision));

        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("资源声明的连接器不允许页面覆盖")
    void declaredConnectorRejectsManualSave() {
        IntegrationConnectorCommand declared = new IntegrationConnectorCommand();
        declared.setConnectorCode("wecom-declared");
        declared.setConnectorName("声明连接器");
        declared.setStatus(1);
        declared.setConfigSchema("{\"type\":\"object\",\"properties\":{}}");
        declared.setAuthSchema("{\"authMode\":\"NONE\",\"fields\":[]}");
        assertThat(connectorService.saveDeclaration(declared)).isTrue();

        IntegrationConnectorCommand manual = new IntegrationConnectorCommand();
        manual.setConnectorCode("wecom-declared");
        manual.setConnectorName("页面修改");
        manual.setStatus(1);
        assertThatThrownBy(() -> connectorService.save(manual))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("资源声明维护");

        assertThat(connectorService.detail("wecom-declared").getSource()).isEqualTo("DECLARED");
    }

    @Test
    @DisplayName("连接测试默认拒绝私网目标")
    void testConnectionRejectsPrivateTargetByDefault() {
        IntegrationConnectorCommand connector = new IntegrationConnectorCommand();
        connector.setConnectorCode("wecom-noauth");
        connector.setConnectorName("无认证连接器");
        connector.setStatus(1);
        connector.setConfigSchema("{\"type\":\"object\",\"required\":[\"apiBaseUrl\"],\"properties\":{\"apiBaseUrl\":{\"type\":\"string\",\"title\":\"接口地址\"}}}");
        connector.setAuthSchema("{\"authMode\":\"NONE\",\"fields\":[]}");
        connectorService.save(connector);

        IntegrationInstanceCommand instance = new IntegrationInstanceCommand();
        instance.setInstanceCode("wecom-noauth-instance");
        instance.setInstanceName("无认证实例");
        instance.setConnectorCode("wecom-noauth");
        instance.setConfigValuesJson("{\"apiBaseUrl\":\"http://127.0.0.1:18021\"}");
        instance.setStatus(1);
        instanceService.save(instance);

        io.mango.integration.api.command.IntegrationConnectionTestCommand command =
                new io.mango.integration.api.command.IntegrationConnectionTestCommand();
        command.setInstanceCode("wecom-noauth-instance");
        assertThatThrownBy(() -> instanceService.testConnection(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("连接测试目标不在允许范围内");
    }

    @Test
    @DisplayName("轮换与读取分别记录历史")
    void rotationAndAccessAreRecorded() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));
        IntegrationCredentialRotateCommand rotate = new IntegrationCredentialRotateCommand();
        rotate.setCredentialCode("wecom-corp");
        rotate.setFieldsJson("{\"corpSecret\":\"rotated\"}");
        credentialService.rotate(rotate);
        credentialService.resolve("wecom-corp");

        assertThat(credentialService.rotations("wecom-corp")).hasSizeGreaterThanOrEqualTo(2);
        Long rotations = jdbcTemplate.queryForObject(
                "select count(*) from integration_credential_rotation where credential_code = 'wecom-corp'",
                Long.class);
        Long accessLogs = jdbcTemplate.queryForObject(
                "select count(*) from integration_credential_access_log where credential_code = 'wecom-corp'",
                Long.class);
        assertThat(rotations).isGreaterThanOrEqualTo(2L);
        assertThat(accessLogs).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("过期凭证标记为 EXPIRED 且不可用于启用实例")
    void expiredCredentialBlocksInstanceEnable() {
        IntegrationCredentialCommand credential = credentialCommand("wecom-expired",
                "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}");
        credential.setExpiresAt(java.time.LocalDateTime.now().minusDays(1));
        credentialService.save(credential);

        assertThat(credentialService.list("wecom-message").stream()
                .filter(item -> "wecom-expired".equals(item.getCredentialCode()))
                .findFirst()
                .orElseThrow()
                .getStatus().name()).isEqualTo("EXPIRED");

        IntegrationInstanceCommand command = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\"}");
        command.setStatus(1);
        command.setCredentialCode("wecom-expired");
        assertThatThrownBy(() -> instanceService.save(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("凭证已过期");
    }

    @Test
    @DisplayName("证书类凭证以明文文本保存，列表仍脱敏")
    void certificateCredentialStoredAsPlainText() {
        String pem = "-----BEGIN CERTIFICATE-----\nMIIBdemo\n-----END CERTIFICATE-----";
        credentialService.save(credentialCommand("wecom-cert",
                "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\",\"platformCert\":\"" + pem.replace("\n", "\\n")
                        + "\"}"));

        String stored = jdbcTemplate.queryForObject(
                "select fields_json from integration_credential where credential_code = 'wecom-cert'", String.class);
        assertThat(stored).contains("\"platformCert\":\"-----BEGIN CERTIFICATE-----");
        assertThat(stored).doesNotContain("\"platformCert\":\"enc:");

        Map<String, String> resolved = credentialService.resolve("wecom-cert");
        assertThat(resolved.get("platformCert")).contains("BEGIN CERTIFICATE");
        assertThat(credentialService.list("wecom-message").stream()
                .filter(item -> "wecom-cert".equals(item.getCredentialCode()))
                .findFirst()
                .orElseThrow()
                .getMaskedFieldsJson()).contains("platformCert=******");
    }

    @Test
    @DisplayName("声明读取返回实例、认证规范与解密凭证")
    void declarationProviderReadsDeclarations() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));
        IntegrationInstanceCommand command = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\"}");
        command.setStatus(1);
        command.setCredentialCode("wecom-corp");
        command.setCapabilitiesJson("[\"message.send\"]");
        instanceService.save(command);

        var declaration = declarationProvider.instance("wecom-main");
        assertThat(declaration.getCallbackPath()).isEqualTo("/integration/callback/wecom-message/wecom-main");
        assertThat(declaration.getCredentialCode()).isEqualTo("wecom-corp");
        assertThat(declarationProvider.connectorAuthSchema("wecom-message")).contains("corpSecret");
        assertThat(declarationProvider.credentials("wecom-corp"))
                .anyMatch(field -> "corpSecret".equals(field.getFieldCode())
                        && "s3cr3t".equals(field.getFieldValue()));
    }

    @Test
    @DisplayName("实例保存成功后返回回调路径与状态")
    void instanceSaveReturnsCallbackPath() {
        credentialService.save(credentialCommand("wecom-corp", "{\"corpId\":\"wx123\",\"corpSecret\":\"s3cr3t\"}"));
        IntegrationInstanceCommand command = instanceCommand("wecom-main",
                "{\"apiBaseUrl\":\"https://qyapi.weixin.qq.com\"}");
        command.setStatus(1);
        command.setCredentialCode("wecom-corp");
        command.setCapabilitiesJson("[\"message.send\"]");
        assertThat(instanceService.save(command)).isTrue();

        IntegrationInstanceVO detail = instanceService.detail("wecom-main");
        assertThat(detail.getCallbackPath()).isEqualTo("/integration/callback/wecom-message/wecom-main");
        assertThat(detail.getStatus().name()).isEqualTo("ENABLED");
        assertThat(detail.getCredentialCode()).isEqualTo("wecom-corp");
        assertThat(instanceMapper.selectCount(null)).isEqualTo(1L);
        assertThat(credentialMapper.selectCount(null)).isEqualTo(1L);
    }

    private IntegrationCredentialCommand credentialCommand(String credentialCode, String fieldsJson) {
        IntegrationCredentialCommand command = new IntegrationCredentialCommand();
        command.setCredentialCode(credentialCode);
        command.setConnectorCode("wecom-message");
        command.setFieldsJson(fieldsJson);
        return command;
    }

    private IntegrationInstanceCommand instanceCommand(String instanceCode, String configValuesJson) {
        IntegrationInstanceCommand command = new IntegrationInstanceCommand();
        command.setInstanceCode(instanceCode);
        command.setInstanceName("企业微信应用");
        command.setConnectorCode("wecom-message");
        command.setConfigValuesJson(configValuesJson);
        command.setEnvironment("PROD");
        command.setStatus(0);
        return command;
    }

    private void seedConnector(String connectorCode) {
        jdbcTemplate.update("""
                insert into integration_connector
                (id, tenant_id, connector_code, connector_name, category, provider, status,
                 config_schema, auth_schema, capabilities_json, docs_url)
                values (?, 'default', ?, '企业微信应用消息', 'MESSAGE', 'WECOM', 1, ?, ?, ?, ?)
                """,
                Math.abs(connectorCode.hashCode()), connectorCode,
                """
                {"type":"object","required":["apiBaseUrl"],"properties":{"apiBaseUrl":{"type":"string","title":"接口地址","pattern":"^https://.*"},"timeoutMs":{"type":"integer","title":"超时毫秒"},"mode":{"type":"string","title":"模式","enum":["API","FILE"]}}}
                """,
                """
                {"authMode":"SECRET","fields":[{"fieldCode":"corpId","label":"企业 ID","type":"STRING","required":true},{"fieldCode":"corpSecret","label":"应用 Secret","type":"SECRET","required":true},{"fieldCode":"platformCert","label":"平台证书","type":"CERT","required":false}]}
                """,
                "[\"message.send\",\"token.refresh\"]",
                "https://developer.work.weixin.qq.com/document");
    }

    private void resetSchema() {
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
        jdbcTemplate.execute("drop table if exists integration_credential_rotation");
        jdbcTemplate.execute("drop table if exists integration_credential_access_log");
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
        jdbcTemplate.execute("drop table if exists integration_connector");
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
    @Import({IntegrationConnectorService.class, IntegrationCredentialService.class, IntegrationInstanceService.class,
        IntegrationDeclarationProviderImpl.class, IntegrationCallRecorderImpl.class})
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
