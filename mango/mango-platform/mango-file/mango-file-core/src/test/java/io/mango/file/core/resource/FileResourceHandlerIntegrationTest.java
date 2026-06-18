package io.mango.file.core.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mango.file.core.mapper.FileSettingsMapper;
import io.mango.file.core.mapper.FileStorageConfigMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.model.ResourceDeclaration;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        FileResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:file_resource;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class FileResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FileStorageConfigResourceHandler storageConfigHandler;

    @Autowired
    private FileSettingsResourceHandler settingsHandler;

    @BeforeEach
    void setUp() {
        rebuildTables();
    }

    @Test
    void fileStorageYamlCreatesExpectedStorageConfigsAndSettings() throws Exception {
        ResourceNode resource = loadFileStorageResource();
        List<ResourceDeclaration> storageConfigs = declarations(resource, ResourceTypes.FILE_STORAGE_CONFIG);
        List<ResourceDeclaration> settings = declarations(resource, ResourceTypes.FILE_SETTINGS);

        for (ResourceDeclaration declaration : storageConfigs) {
            storageConfigHandler.upsert(declaration);
        }
        for (ResourceDeclaration declaration : settings) {
            settingsHandler.upsert(declaration);
        }

        assertThat(storageConfigs).hasSize(2);
        assertThat(settings).hasSize(1);
        assertThat(count("file_storage_config")).isEqualTo(2);
        assertThat(count("file_settings")).isOne();
        assertThat(stringValue("file_storage_config", "storage_type", "id = 1")).isEqualTo("LOCAL");
        assertThat(stringValue("file_storage_config", "storage_path", "id = 1")).isEqualTo("mango-file");
        assertThat(stringValue("file_storage_config", "endpoint", "id = 2")).isEqualTo("http://127.0.0.1:9000");
        assertThat(intValue("file_storage_config", "path_style_access", "id = 2")).isOne();
        assertThat(stringValue("file_settings", "default_access_level", "id = 1")).isEqualTo("PRIVATE");
        assertThat(stringValue("file_settings", "access_mode", "id = 1")).isEqualTo("PROXY");
        assertThat(intValue("file_settings", "archive_retain_days", "id = 1")).isEqualTo(180);
    }

    @Test
    void storageConfigUpsertUpdatesExistingConfig() throws Exception {
        ResourceDeclaration declaration = declarations(loadFileStorageResource(), ResourceTypes.FILE_STORAGE_CONFIG).getFirst();
        storageConfigHandler.upsert(declaration);

        declaration.getFields().get("remark").setValue("更新后的备注");
        declaration.getFields().get("active").setValue(0);
        declaration.getFields().get("tenantId").setValue(2);
        storageConfigHandler.upsert(declaration);

        assertThat(count("file_storage_config")).isOne();
        assertThat(intValue("file_storage_config", "tenant_id", "id = 1")).isEqualTo(2);
        assertThat(stringValue("file_storage_config", "remark", "id = 1")).isEqualTo("更新后的备注");
        assertThat(intValue("file_storage_config", "active", "id = 1")).isZero();
    }

    @Test
    void disableAndDeleteHandleStorageConfigAndSettings() throws Exception {
        ResourceNode resource = loadFileStorageResource();
        ResourceDeclaration storageConfig = declarations(resource, ResourceTypes.FILE_STORAGE_CONFIG).getFirst();
        ResourceDeclaration settings = declarations(resource, ResourceTypes.FILE_SETTINGS).getFirst();
        storageConfigHandler.upsert(storageConfig);
        settingsHandler.upsert(settings);

        storageConfigHandler.disable(storageConfig);
        settingsHandler.disable(settings);

        assertThat(intValue("file_storage_config", "status", "id = 1")).isZero();
        assertThat(intValue("file_storage_config", "active", "id = 1")).isZero();
        assertThat(intValue("file_settings", "instant_upload_enabled", "id = 1")).isZero();

        storageConfigHandler.delete(storageConfig);
        settingsHandler.delete(settings);

        assertThat(count("file_storage_config")).isZero();
        assertThat(count("file_settings")).isZero();
    }

    @SuppressWarnings("unchecked")
    private List<ResourceDeclaration> declarations(ResourceNode resource, String resourceType) {
        List<ResourceDeclaration> declarations =
                ((List<ResourceDeclaration>) resource.declarations().get(resourceType));
        for (ResourceDeclaration declaration : declarations) {
            declaration.setResourceType(resourceType);
            declaration.setModuleCode(resource.moduleCode());
            declaration.setModuleName(resource.moduleName());
            declaration.setSource("file-common-storage.yml");
        }
        return declarations;
    }

    private ResourceNode loadFileStorageResource() throws Exception {
        Path path = Path.of("../mango-file-starter/src/main/resources/META-INF/mango/resources/file-common-storage.yml");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        MapNode root = objectMapper.readValue(path.toFile(), MapNode.class);
        return root.mango().resource();
    }

    private void rebuildTables() {
        jdbcTemplate.execute("drop table if exists file_storage_config");
        jdbcTemplate.execute("drop table if exists file_settings");
        jdbcTemplate.execute("""
                create table file_storage_config (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    config_name varchar(64) not null,
                    storage_type varchar(32) not null,
                    endpoint varchar(255),
                    public_endpoint varchar(255),
                    region varchar(64),
                    bucket_name varchar(128) not null,
                    storage_path varchar(255) not null default '',
                    access_key varchar(255),
                    secret_key varchar(512),
                    path_style_access tinyint not null default 0,
                    ssl_enabled tinyint not null default 0,
                    active tinyint not null default 0,
                    status tinyint not null default 1,
                    remark varchar(255),
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_file_storage_config_name (config_name)
                )
                """);
        jdbcTemplate.execute("""
                create table file_settings (
                    id bigint not null,
                    tenant_id bigint not null default 1,
                    max_size bigint not null default 104857600,
                    allowed_extensions varchar(1000),
                    blocked_extensions varchar(1000) default 'exe,bat,cmd,sh,jar',
                    default_access_level varchar(32) not null default 'PRIVATE',
                    duplicate_name_strategy varchar(32) not null default 'REJECT',
                    duplicate_check_directory_scoped tinyint not null default 1,
                    object_name_strategy varchar(32) not null default 'DATE_UUID',
                    instant_upload_enabled tinyint not null default 1,
                    instant_upload_scope varchar(32) not null default 'TENANT',
                    content_type_check_enabled tinyint not null default 1,
                    allowed_content_types varchar(1000),
                    blocked_content_types varchar(1000) default 'application/x-msdownload,application/x-sh',
                    direct_upload_enabled tinyint not null default 0,
                    direct_upload_expire_seconds bigint not null default 900,
                    access_token_enabled tinyint not null default 0,
                    public_read_requires_token tinyint not null default 0,
                    access_mode varchar(32) not null default 'PROXY',
                    access_token_expire_seconds bigint not null default 600,
                    preview_provider_url varchar(500),
                    preview_expire_seconds bigint not null default 600,
                    preview_external_extensions varchar(1000),
                    archive_retain_enabled tinyint not null default 1,
                    archive_retain_days int not null default 180,
                    archive_restore_enabled tinyint not null default 0,
                    physical_delete_enabled tinyint not null default 0,
                    created_by bigint,
                    created_time timestamp not null default current_timestamp,
                    created_at timestamp not null default current_timestamp,
                    updated_by bigint,
                    updated_time timestamp not null default current_timestamp,
                    updated_at timestamp not null default current_timestamp,
                    primary key (id),
                    unique key uk_file_settings_tenant (tenant_id)
                )
                """);
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String stringValue(String tableName, String columnName, String whereClause) {
        return jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                String.class);
    }

    private int intValue(String tableName, String columnName, String whereClause) {
        Integer value = jdbcTemplate.queryForObject("select " + columnName + " from " + tableName + " where " + whereClause,
                Integer.class);
        return value == null ? 0 : value;
    }

    @Configuration
    @Import({
            FileStorageConfigResourceHandler.class,
            FileSettingsResourceHandler.class
    })
    @MapperScan(basePackageClasses = {
            FileStorageConfigMapper.class,
            FileSettingsMapper.class
    })
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
