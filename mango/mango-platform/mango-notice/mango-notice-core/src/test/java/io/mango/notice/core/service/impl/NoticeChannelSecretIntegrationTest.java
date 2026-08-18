package io.mango.notice.core.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mango.common.exception.BizException;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.crypto.impl.ICryptoService;
import io.mango.infra.crypto.impl.sm.Sm4CryptoService;
import io.mango.infra.crypto.starter.CryptoProperties;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
import io.mango.notice.api.command.NoticeChannelSecretValueCommand;
import io.mango.notice.api.command.SaveNoticeChannelConfigCommand;
import io.mango.notice.api.enums.NoticeChannelCapabilityMode;
import io.mango.notice.api.enums.NoticeChannelType;
import io.mango.notice.api.query.NoticeChannelConfigPageQuery;
import io.mango.notice.api.query.NoticeChannelSecretQuery;
import io.mango.notice.api.vo.NoticeChannelConfigVO;
import io.mango.notice.core.entity.NoticeChannelConfigEntity;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.notice.core.service.NoticeChannelSecretAuditService;
import io.mango.notice.core.service.NoticeChannelSecretCodec;
import io.mango.notice.core.service.NoticeChannelSecretMaterializer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;

@SpringBootTest(
        classes = {
            DataSourceAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            PersistenceMybatisPlusAutoConfiguration.class,
            PersistenceAuditAutoConfiguration.class,
            NoticeChannelSecretIntegrationTest.TestConfig.class
        })
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:notice_secret_825;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.flyway.enabled=false",
            "mango.persistence.mybatis-plus.tenant.enabled=true",
            "mango.persistence.mybatis-plus.pagination.enabled=true"
        })
class NoticeChannelSecretIntegrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private NoticeConfigurationService configurationService;
    @Autowired private NoticeChannelConfigMapper channelConfigMapper;

    @BeforeEach
    void setUp() {
        useTenant("tenant-a");
        resetSchema();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void saveEncryptsSecretListNeverReturnsPlaintextAndRevealWritesSafeAudit() {
        SaveNoticeChannelConfigCommand command = smtpCommand();
        command.setSecretValues(List.of(secret("password", "smtp-password-825")));

        NoticeChannelConfigVO saved = configurationService.saveChannelConfig(command);
        NoticeChannelConfigEntity entity = channelConfigMapper.selectById(saved.getId());

        assertThat(entity.getSecretConfigJson())
                .contains("enc:")
                .doesNotContain("smtp-password-825");
        assertThat(saved.getConfigJson())
                .contains("smtp.example.com")
                .contains("https://callback.example.com")
                .doesNotContain("smtp-password-825");
        assertThat(saved.getConfiguredSecretKeys()).containsExactly("password");

        NoticeChannelConfigPageQuery pageQuery = new NoticeChannelConfigPageQuery();
        NoticeChannelConfigVO listed =
                configurationService.listChannelConfigs(pageQuery).getList().getFirst();
        assertThat(listed.getConfigJson()).doesNotContain("smtp-password-825");

        String revealed =
                configurationService.revealChannelSecret(secretQuery(saved.getId(), "password"))
                        .getValue();
        assertThat(revealed).isEqualTo("smtp-password-825");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select audit_snapshot from notice_audit_log where target_id = ?",
                                String.class,
                                saved.getId()))
                .contains("password", "MANUAL", "SUCCESS")
                .doesNotContain("smtp-password-825")
                .doesNotContain("enc:");
    }

    @Test
    void unchangedSavePreservesCiphertextAndLegacyPlaintextIsMigratedOnReveal() {
        SaveNoticeChannelConfigCommand command = smtpCommand();
        command.setSecretValues(List.of(secret("password", "preserved-password")));
        NoticeChannelConfigVO saved = configurationService.saveChannelConfig(command);
        String originalCiphertext =
                channelConfigMapper.selectById(saved.getId()).getSecretConfigJson();

        SaveNoticeChannelConfigCommand update = smtpCommand();
        update.setId(saved.getId());
        update.setConfigCode(saved.getConfigCode());
        configurationService.saveChannelConfig(update);

        assertThat(channelConfigMapper.selectById(saved.getId()).getSecretConfigJson())
                .isEqualTo(originalCiphertext);

        jdbcTemplate.update(
                "update notice_channel_config set secret_config_json = ? where id = ?",
                "{\"smtpPassword\":\"legacy-password\"}",
                saved.getId());
        assertThat(configurationService.listChannelConfigs(new NoticeChannelConfigPageQuery())
                        .getList()
                        .getFirst()
                        .getConfiguredSecretKeys())
                .containsExactly("password");
        assertThat(
                        configurationService
                                .revealChannelSecret(secretQuery(saved.getId(), "password"))
                                .getValue())
                .isEqualTo("legacy-password");
        assertThat(channelConfigMapper.selectById(saved.getId()).getSecretConfigJson())
                .contains("\"password\":\"enc:")
                .contains("enc:")
                .doesNotContain("legacy-password", "smtpPassword");
    }

    @Test
    void invalidFieldReferenceAndCrossTenantAccessFailClosed() {
        SaveNoticeChannelConfigCommand command = smtpCommand();
        command.setSecretValues(List.of(secret("password", "protected-password")));
        NoticeChannelConfigVO saved = configurationService.saveChannelConfig(command);

        assertThatThrownBy(
                        () ->
                                configurationService.revealChannelSecret(
                                        secretQuery(saved.getId(), "webhookUrl")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不支持查看")
                .hasMessageNotContaining("protected-password");

        jdbcTemplate.update(
                "update notice_channel_config set secret_refs_json = ? where id = ?",
                "{\"password\":\"property:notice.password\"}",
                saved.getId());
        assertThatThrownBy(
                        () ->
                                configurationService.revealChannelSecret(
                                        secretQuery(saved.getId(), "password")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("由引用管理")
                .hasMessageNotContaining("property:notice.password");

        useTenant("tenant-b");
        assertThatThrownBy(
                        () ->
                                configurationService.revealChannelSecret(
                                        secretQuery(saved.getId(), "password")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不存在或无权访问")
                .hasMessageNotContaining("protected-password");
    }

    private SaveNoticeChannelConfigCommand smtpCommand() {
        SaveNoticeChannelConfigCommand command = new SaveNoticeChannelConfigCommand();
        command.setConfigCode("EMAIL_ISSUE_825");
        command.setChannelType(NoticeChannelType.EMAIL);
        command.setCapabilityMode(NoticeChannelCapabilityMode.SEND);
        command.setProviderCode("CUSTOM_SMTP");
        command.setConfigName("Issue 825 SMTP");
        command.setConfigJson(
                "{\"host\":\"smtp.example.com\",\"port\":465,\"username\":\"notice\","
                        + "\"from\":\"notice@example.com\",\"ssl\":true,"
                        + "\"webhookUrl\":\"https://callback.example.com\","
                        + "\"loginEnabled\":true,"
                        + "\"loginRedirectUri\":\"https://admin.example.com/login\"}");
        command.setEnabled(true);
        command.setPriority(1);
        command.setWeight(100);
        return command;
    }

    private NoticeChannelSecretValueCommand secret(String key, String value) {
        NoticeChannelSecretValueCommand command = new NoticeChannelSecretValueCommand();
        command.setKey(key);
        command.setValue(value);
        return command;
    }

    private NoticeChannelSecretQuery secretQuery(Long id, String key) {
        NoticeChannelSecretQuery query = new NoticeChannelSecretQuery();
        query.setChannelConfigId(id);
        query.setSecretKey(key);
        return query;
    }

    private void useTenant(String tenantId) {
        MangoContextHolder.set(
                MangoContextSnapshot.empty()
                        .withSecurity(825L, tenantId, "issue-825", null, null, null, null, "test"));
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists notice_audit_log");
        jdbcTemplate.execute("drop table if exists notice_channel_config_route_tag");
        jdbcTemplate.execute("drop table if exists notice_channel_route_tag");
        jdbcTemplate.execute("drop table if exists notice_channel_config");
        jdbcTemplate.execute(
                """
                create table notice_channel_config (
                    id bigint generated by default as identity primary key,
                    config_code varchar(128), channel_type varchar(32), capability_mode varchar(16),
                    provider_code varchar(64), config_name varchar(128), config_json clob,
                    secret_refs_json clob, secret_config_json clob, resource_id varchar(128),
                    resource_version bigint, resource_module_code varchar(128), resource_source varchar(32),
                    managed_fields_json clob, secret_status varchar(32), enabled boolean, priority int,
                    weight int, config_status varchar(32), last_send_status varchar(32),
                    last_send_time timestamp, last_failure_code varchar(128), last_failure_reason varchar(512),
                    rate_limit_config clob, tenant_id varchar(64), created_by bigint,
                    created_at timestamp default current_timestamp, updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute(
                """
                create table notice_channel_route_tag (
                    id bigint generated by default as identity primary key,
                    channel_type varchar(32), tag_code varchar(64), tag_name varchar(128), description varchar(512),
                    tenant_id varchar(64), created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute(
                """
                create table notice_channel_config_route_tag (
                    id bigint generated by default as identity primary key,
                    channel_config_id bigint, route_tag_id bigint, tenant_id varchar(64),
                    created_by bigint, created_at timestamp default current_timestamp,
                    updated_by bigint, updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute(
                """
                create table notice_audit_log (
                    id bigint generated by default as identity primary key,
                    action_type varchar(64), target_type varchar(64), target_id bigint, operator_id bigint,
                    audit_snapshot clob, tenant_id varchar(64), created_at timestamp default current_timestamp
                )
                """);
    }

    @Configuration
    @MapperScan(basePackageClasses = NoticeChannelConfigMapper.class)
    @Import({
        NoticeConfigurationService.class,
        NoticeChannelSecretCodec.class,
        NoticeChannelSecretMaterializer.class,
        NoticeChannelSecretAuditService.class
    })
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        ICryptoService cryptoService() {
            CryptoProperties properties = new CryptoProperties();
            properties.getSm4().setSecretKey("00112233445566778899aabbccddeeff");
            return new Sm4CryptoService(properties);
        }
    }
}
