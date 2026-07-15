package io.mango.gridlayout.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.gridlayout.api.command.SaveGridLayoutPersonalCommand;
import io.mango.gridlayout.api.query.GridLayoutPersonalQuery;
import io.mango.gridlayout.api.vo.GridLayoutPersonalVO;
import io.mango.gridlayout.core.mapper.MangoUserGridLayoutMapper;
import io.mango.gridlayout.core.service.IGridLayoutPersonalService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceAuditAutoConfiguration;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
        PersistenceAuditAutoConfiguration.class,
        GridLayoutPersonalServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:grid_layout;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"
})
class GridLayoutPersonalServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IGridLayoutPersonalService gridLayoutPersonalService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS mango_user_grid_layout");
        jdbcTemplate.execute("""
                CREATE TABLE mango_user_grid_layout (
                    id BIGINT NOT NULL,
                    tenant_id VARCHAR(64) NOT NULL,
                    org_id BIGINT,
                    user_id BIGINT NOT NULL,
                    page_code VARCHAR(100) NOT NULL,
                    schema_version INT NOT NULL DEFAULT 1,
                    layout_json LONGTEXT NOT NULL,
                    created_by BIGINT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_by BIGINT,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_mango_user_grid_layout_scope (tenant_id, user_id, page_code)
                )
                """);
        setContext("1", 1001L, 91L);
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void saveQueryUpdateDelete_realMapper_preservesTenantUserAndAuditFields() {
        GridLayoutPersonalVO created = gridLayoutPersonalService.savePersonal(command(3));

        assertThat(created.getId()).isNotNull();
        assertThat(gridLayoutPersonalService.getPersonal(query())).isEqualTo(created);
        Map<String, Object> inserted = jdbcTemplate.queryForMap("""
                SELECT tenant_id, org_id, user_id, page_code, schema_version,
                       created_by, created_at, updated_by, updated_at
                  FROM mango_user_grid_layout
                 WHERE id = ?
                """, created.getId());
        assertThat(inserted)
                .containsEntry("tenant_id", "1")
                .containsEntry("org_id", 91L)
                .containsEntry("user_id", 1001L)
                .containsEntry("page_code", "admin-home-workbench")
                .containsEntry("schema_version", 1)
                .containsEntry("created_by", 1001L)
                .containsEntry("updated_by", 1001L);
        assertThat(inserted.get("created_at")).isNotNull();
        assertThat(inserted.get("updated_at")).isNotNull();

        GridLayoutPersonalVO updated = gridLayoutPersonalService.savePersonal(command(4));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getLayoutJson()).contains("\"w\":4");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mango_user_grid_layout", Integer.class)).isEqualTo(1);
        assertThat(gridLayoutPersonalService.deletePersonal(query())).isTrue();
        assertThat(gridLayoutPersonalService.deletePersonal(query())).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mango_user_grid_layout", Integer.class)).isZero();
    }

    @Test
    void getPersonal_differentTenantOrUser_doesNotLeakLayout() {
        gridLayoutPersonalService.savePersonal(command(3));

        setContext("1", 1002L, 91L);
        assertThat(gridLayoutPersonalService.getPersonal(query())).isNull();
        assertThat(gridLayoutPersonalService.deletePersonal(query())).isFalse();

        setContext("2", 1001L, 92L);
        assertThat(gridLayoutPersonalService.getPersonal(query())).isNull();
        assertThat(gridLayoutPersonalService.deletePersonal(query())).isFalse();

        setContext("1", 1001L, 91L);
        assertThat(gridLayoutPersonalService.getPersonal(query())).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mango_user_grid_layout", Integer.class)).isEqualTo(1);
    }

    private void setContext(String tenantId, Long userId, Long orgId) {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                userId, tenantId, "grid-layout-it", "INTERNAL", "INTERNAL_USER", "ORG", orgId,
                "internal-admin"));
    }

    private SaveGridLayoutPersonalCommand command(int width) {
        SaveGridLayoutPersonalCommand command = new SaveGridLayoutPersonalCommand();
        command.setPageCode("admin-home-workbench");
        command.setLayoutJson("""
                {"schemaVersion":1,"pageCode":"admin-home-workbench","items":[{"id":"a","widgetType":"todo","layout":{"x":0,"y":0,"w":%d,"h":3}}]}
                """.formatted(width));
        return command;
    }

    private GridLayoutPersonalQuery query() {
        GridLayoutPersonalQuery query = new GridLayoutPersonalQuery();
        query.setPageCode("admin-home-workbench");
        return query;
    }

    @Configuration(proxyBeanMethods = false)
    @MapperScan(basePackageClasses = MangoUserGridLayoutMapper.class)
    @Import(GridLayoutPersonalService.class)
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
