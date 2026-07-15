package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.command.MenuPackageCommand;
import io.mango.authorization.core.mapper.MenuPackageItemMapper;
import io.mango.authorization.core.mapper.MenuPackageMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        MenuPackageServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:menu_package_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class MenuPackageServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MenuPackageService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_menu_package_item");
        jdbcTemplate.execute("drop table if exists authorization_menu_package");
        jdbcTemplate.execute("""
                create table authorization_menu_package (
                    id bigint primary key,
                    package_name varchar(128) not null,
                    package_code varchar(64) not null,
                    app_code varchar(64) not null,
                    status tinyint not null,
                    sort int not null,
                    remark varchar(255),
                    create_by varchar(64),
                    update_by varchar(64),
                    del_flag tinyint not null default 0,
                    tenant_id bigint not null,
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_menu_package_item (
                    id bigint primary key,
                    package_id bigint not null,
                    menu_id bigint not null,
                    sort int not null,
                    tenant_id bigint not null,
                    created_at timestamp default current_timestamp,
                    updated_at timestamp default current_timestamp
                )
                """);
        AuthorizationTestSchema.ensureCanonicalColumns(jdbcTemplate);
    }

    @Test
    void createWithoutRequestContextUsesPlatformTenantForPackageAndItems() {
        Long packageId = service.create(command(null, "platform_admin", List.of(101L, 102L)));

        assertThat(jdbcTemplate.queryForObject(
                "select tenant_id from authorization_menu_package where id = ?", Long.class, packageId))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForList(
                "select tenant_id from authorization_menu_package_item where package_id = ? order by sort",
                Long.class,
                packageId)).containsExactly(1L, 1L);
    }

    @Test
    void updateWithoutRequestContextPreservesPersistedTenant() {
        jdbcTemplate.update("""
                insert into authorization_menu_package
                (id, package_name, package_code, app_code, status, sort, del_flag, tenant_id)
                values (900, 'Tenant Package', 'tenant_package', 'internal-admin', 1, 1, 0, 9)
                """);

        assertThat(service.update(command(900L, "tenant_package", List.of(201L)))).isTrue();

        assertThat(jdbcTemplate.queryForObject(
                "select tenant_id from authorization_menu_package where id = 900", Long.class)).isEqualTo(9L);
        assertThat(jdbcTemplate.queryForObject(
                "select tenant_id from authorization_menu_package_item where package_id = 900", Long.class)).isEqualTo(9L);
    }

    private MenuPackageCommand command(Long packageId, String packageCode, List<Long> menuIds) {
        MenuPackageCommand command = new MenuPackageCommand();
        command.setPackageId(packageId);
        command.setPackageName("Package " + packageCode);
        command.setPackageCode(packageCode);
        command.setAppCode("internal-admin");
        command.setStatus(1);
        command.setSort(1);
        command.setMenuIds(menuIds);
        return command;
    }

    @Configuration
    @MapperScan(basePackageClasses = {MenuPackageMapper.class, MenuPackageItemMapper.class})
    @Import(MenuPackageService.class)
    static class TestConfig {
    }
}
