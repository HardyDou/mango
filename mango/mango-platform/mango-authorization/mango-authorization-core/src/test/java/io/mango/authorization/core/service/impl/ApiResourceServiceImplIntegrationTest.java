package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.command.ApiResourceRegisterCommand;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import io.mango.authorization.api.vo.ApiResourceAccessDecisionVO;
import io.mango.authorization.api.vo.ApiResourceRegisterResultVO;
import io.mango.authorization.core.entity.ApiResource;
import io.mango.authorization.core.mapper.ApiResourceMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
        ApiResourceServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_resource_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("ApiResourceServiceImpl 集成测试")
class ApiResourceServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApiResourceMapper apiResourceMapper;

    @Autowired
    private ApiResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_api_resource");
        jdbcTemplate.execute("""
                create table authorization_api_resource (
                    id bigint primary key,
                    module_name varchar(128) not null,
                    http_method varchar(16) not null,
                    path_pattern varchar(512) not null,
                    resource_code varchar(640) not null,
                    permission_code varchar(255),
                    access_mode varchar(32) not null default 'LOGIN',
                    handler_class varchar(512),
                    handler_method varchar(128),
                    description varchar(255),
                    status tinyint not null default 1,
                    create_time timestamp default current_timestamp,
                    update_time timestamp default current_timestamp,
                    deleted tinyint not null default 0
                )
                """);
    }

    @Test
    @DisplayName("registerApiResources should disable stale controller paths through real mapper")
    void registerApiResourcesDisablesStaleControllerPathsThroughRealMapper() {
        seedResource(1L, "mango-system", "GET", "/system/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);
        seedResource(2L, "mango-system", "GET", "/area/tree",
                "io.mango.area.core.controller.SysAreaController", 1);

        ApiResourceRegisterResultVO result = service.registerApiResources(List.of(command("mango-system", "GET",
                "/system/area/tree", "io.mango.area.core.controller.SysAreaController")));

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(2);
        assertThat(apiResourceMapper.selectById(1L).getStatus()).isEqualTo(1);
        assertThat(apiResourceMapper.selectById(2L).getStatus()).isZero();
    }

    @Test
    @DisplayName("resolveAccessDecision should match persisted active resources")
    void resolveAccessDecisionMatchesPersistedActiveResources() {
        seedResource(10L, "mango-workflow", "GET", "/workflow/definitions/**",
                "io.mango.workflow.DefinitionController", 1, "workflow:definition:list",
                ApiResourceAccessMode.PERMISSION);

        ApiResourceAccessDecisionVO decision = service.resolveAccessDecision("GET", "/workflow/definitions/100");

        assertThat(decision.matched()).isTrue();
        assertThat(decision.accessMode()).isEqualTo(ApiResourceAccessMode.PERMISSION);
        assertThat(decision.permissionCode()).isEqualTo("workflow:definition:list");
    }

    private ApiResourceRegisterCommand command(String moduleName, String httpMethod, String path, String handlerClass) {
        ApiResourceRegisterCommand command = new ApiResourceRegisterCommand();
        command.setModuleName(moduleName);
        command.setHttpMethod(httpMethod);
        command.setPathPattern(path);
        command.setHandlerClass(handlerClass);
        command.setHandlerMethod("tree");
        command.setAccessMode(ApiResourceAccessMode.LOGIN);
        command.setResourceCode(httpMethod + ":" + path);
        return command;
    }

    private void seedResource(Long id,
                              String moduleName,
                              String httpMethod,
                              String path,
                              String handlerClass,
                              int status) {
        seedResource(id, moduleName, httpMethod, path, handlerClass, status,
                httpMethod + ":" + path, ApiResourceAccessMode.LOGIN);
    }

    private void seedResource(Long id,
                              String moduleName,
                              String httpMethod,
                              String path,
                              String handlerClass,
                              int status,
                              String permissionCode,
                              ApiResourceAccessMode accessMode) {
        jdbcTemplate.update("""
                        insert into authorization_api_resource
                        (id, module_name, http_method, path_pattern, resource_code, permission_code,
                         access_mode, handler_class, handler_method, status, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, 'tree', ?, 0)
                        """,
                id, moduleName, httpMethod, path, httpMethod + ":" + path, permissionCode,
                accessMode.name(), handlerClass, status);
    }

    @Configuration
    @MapperScan(basePackageClasses = ApiResourceMapper.class)
    @Import(ApiResourceServiceImpl.class)
    static class TestConfig {
    }
}
