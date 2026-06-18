package io.mango.notice.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.core.mapper.NoticeChannelConfigMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
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
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        NoticeChannelResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:notice_channel_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class NoticeChannelResourceHandlerIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private NoticeChannelResourceHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        rebuildTables();
    }

    @Test
    void upsertCreatesNoticeChannel() throws Exception {
        handler.upsert(channelDeclaration(270501L, "default", "默认系统消息通道", 100));

        assertThat(stringValue("notice_channel_config", "channel_type", "id = 270501")).isEqualTo("SITE");
        assertThat(stringValue("notice_channel_config", "provider_code", "id = 270501")).isEqualTo("INTERNAL");
        assertThat(stringValue("notice_channel_config", "config_status", "id = 270501")).isEqualTo("COMPLETE");
        assertThat(intValue("notice_channel_config", "weight", "id = 270501")).isEqualTo(100);
    }

    @Test
    void upsertUpdatesNoticeChannel() throws Exception {
        handler.upsert(channelDeclaration(270501L, "default", "默认系统消息通道", 100));

        handler.upsert(channelDeclaration(270501L, "default", "默认系统消息通道新版", 80));

        assertThat(stringValue("notice_channel_config", "config_name", "id = 270501")).isEqualTo("默认系统消息通道新版");
        assertThat(intValue("notice_channel_config", "weight", "id = 270501")).isEqualTo(80);
    }

    @Test
    void disableMarksNoticeChannelDisabled() throws Exception {
        ResourceDeclaration declaration = channelDeclaration(270501L, "default", "默认系统消息通道", 100);
        handler.upsert(declaration);

        handler.disable(declaration);

        assertThat(booleanValue("notice_channel_config", "enabled", "id = 270501")).isFalse();
    }

    @Test
    void deletePhysicallyDeletesNoticeChannel() throws Exception {
        ResourceDeclaration declaration = channelDeclaration(270501L, "default", "默认系统消息通道", 100);
        handler.upsert(declaration);

        handler.delete(declaration);

        assertThat(count("notice_channel_config")).isZero();
    }

    @Test
    void noticeChannelsMatchOldFlywaySeeds() throws Exception {
        for (ResourceDeclaration declaration : commonNoticeChannelDeclarations()) {
            handler.upsert(declaration);
        }

        assertThat(count("notice_channel_config")).isEqualTo(2);
        assertThat(stringValue("notice_channel_config", "config_name", "id = 270501")).isEqualTo("默认系统消息通道");
        assertThat(stringValue("notice_channel_config", "tenant_id", "id = 270501")).isEqualTo("default");
        assertThat(stringValue("notice_channel_config", "tenant_id", "id = 270502")).isEqualTo("1");
        assertThat(stringValue("notice_channel_config", "config_json", "id = 270501"))
                .contains("\"soundText\":\"您有新的系统消息，请及时查看\"");
        assertThat(stringValue("notice_channel_config", "config_json", "id = 270502"))
                .contains("\"soundText\":\"您有新的系统消息，请及时查看\"");
    }

    private List<ResourceDeclaration> commonNoticeChannelDeclarations() {
        return List.of(
                channelDeclaration(270501L, "default", "默认系统消息通道", 100),
                channelDeclaration(270502L, "1", "默认系统消息通道", 100)
        );
    }

    private ResourceDeclaration channelDeclaration(Long channelConfigId, String tenantId, String configName, int weight) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setId(String.valueOf(2026061800700000000L + channelConfigId));
        declaration.setVersion(1);
        declaration.setResourceType(ResourceTypes.MESSAGE_CHANNEL);
        declaration.setModuleCode("notice");
        declaration.setBizKey("notice.channel.site-internal-" + tenantId);
        declaration.setName(configName);
        declaration.setTargetModule("notice");
        declaration.setFields(new LinkedHashMap<>());
        field(declaration, "channelConfigId", ResourceFieldType.LONG, channelConfigId);
        field(declaration, "tenantId", ResourceFieldType.STRING, tenantId);
        field(declaration, "channelType", ResourceFieldType.STRING, "SITE");
        field(declaration, "providerCode", ResourceFieldType.STRING, "INTERNAL");
        field(declaration, "configName", ResourceFieldType.STRING, configName);
        field(declaration, "configJson", ResourceFieldType.STRING,
                "{\"senderName\":\"系统通知\",\"soundText\":\"您有新的系统消息，请及时查看\"}");
        field(declaration, "enabled", ResourceFieldType.BOOLEAN, true);
        field(declaration, "priority", ResourceFieldType.INT, 0);
        field(declaration, "weight", ResourceFieldType.INT, weight);
        field(declaration, "configStatus", ResourceFieldType.STRING, "COMPLETE");
        field(declaration, "lastSendStatus", ResourceFieldType.STRING, "NONE");
        field(declaration, "rateLimitConfig", ResourceFieldType.STRING,
                "{\"maxPerMinute\":0,\"timeoutSeconds\":10,\"concurrentLimit\":0}");
        return declaration;
    }

    private void field(ResourceDeclaration declaration, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        declaration.getFields().put(name, field);
    }

    private void rebuildTables() throws Exception {
        execute("drop table if exists notice_channel_config");
        execute("""
                create table notice_channel_config (
                    id bigint not null,
                    channel_type varchar(32) not null,
                    provider_code varchar(64),
                    config_name varchar(128),
                    config_json clob,
                    enabled boolean not null default true,
                    priority int not null default 0,
                    weight int not null default 100,
                    config_status varchar(32) not null default 'INCOMPLETE',
                    last_send_status varchar(32) not null default 'NONE',
                    last_send_time timestamp,
                    last_failure_code varchar(64),
                    last_failure_reason varchar(500),
                    rate_limit_config clob,
                    tenant_id varchar(64) not null default 'default',
                    created_by bigint,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id)
                )
                """);
        execute("create index idx_notice_channel_type on notice_channel_config(tenant_id, channel_type, enabled)");
        execute("""
                create index idx_notice_channel_route
                on notice_channel_config(tenant_id, channel_type, enabled, config_status, weight)
                """);
    }

    private void execute(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select count(*) from " + tableName)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private String stringValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int intValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean booleanValue(String tableName, String columnName, String whereClause) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select " + columnName + " from " + tableName + " where " + whereClause)) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    @Configuration
    @Import(NoticeChannelResourceHandler.class)
    @MapperScan(basePackageClasses = NoticeChannelConfigMapper.class)
    static class TestConfig {
    }
}
