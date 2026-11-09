package io.mango.authorization.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.sun.net.httpserver.HttpServer;
import io.mango.authorization.api.command.FrontendAppHealthCheckCommand;
import io.mango.authorization.api.vo.FrontendAppHealthCheckVO;
import io.mango.authorization.core.config.OutboundNetworkProperties;
import io.mango.common.exception.BizException;
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

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        FrontendAppHealthServiceTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:frontend_app_health;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("FrontendAppHealthService 集成测试")
class FrontendAppHealthServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboundNetworkProperties properties;

    @Autowired
    private FrontendAppHealthService service;

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        resetSchema();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("健康检查地址可访问时返回成功")
    void checkSucceedsForReachableTarget() {
        seedRegistry("order-web", "MICRO_APP", baseUrl() + "/health", null);
        properties.setAllowPrivateNetwork(true);
        try {
            FrontendAppHealthCheckVO result = service.check(command("order-web"));
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.getTargetUrl()).isEqualTo(baseUrl() + "/health");
            assertThat(result.getMessage()).doesNotContain("password");
        } finally {
            properties.setAllowPrivateNetwork(false);
        }
    }

    @Test
    @DisplayName("目标返回非成功状态码时记录失败原因")
    void checkReportsFailureStatusCode() {
        seedRegistry("cms-web", "MICRO_APP", baseUrl() + "/missing", null);
        properties.setAllowPrivateNetwork(true);
        try {
            FrontendAppHealthCheckVO result = service.check(command("cms-web"));
            assertThat(result.getSuccess()).isFalse();
            assertThat(result.getStatusCode()).isEqualTo(500);
        } finally {
            properties.setAllowPrivateNetwork(false);
        }
    }

    @Test
    @DisplayName("未配置健康检查地址时回落到入口地址")
    void checkFallsBackToEntryUrl() {
        seedRegistry("workflow-web", "MICRO_APP", null, baseUrl() + "/health");
        properties.setAllowPrivateNetwork(true);
        try {
            FrontendAppHealthCheckVO result = service.check(command("workflow-web"));
            assertThat(result.getSuccess()).isTrue();
            assertThat(result.getTargetUrl()).isEqualTo(baseUrl() + "/health");
        } finally {
            properties.setAllowPrivateNetwork(false);
        }
    }

    @Test
    @DisplayName("默认拒绝私网目标且不发起请求")
    void checkRejectsPrivateTargetByDefault() {
        seedRegistry("rbac-web", "MICRO_APP", baseUrl() + "/health", null);

        FrontendAppHealthCheckVO result = service.check(command("rbac-web"));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("目标地址不在允许范围内");
    }

    @Test
    @DisplayName("未登记运行单元和缺少地址时拒绝检查")
    void checkRejectsUnknownOrIncompleteRuntime() {
        assertThatThrownBy(() -> service.check(command("not-exists")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("前端运行单元不存在");

        seedRegistry("no-url-web", "MICRO_APP", null, null);
        assertThatThrownBy(() -> service.check(command("no-url-web")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("运行单元未配置入口或健康检查地址");
    }

    private FrontendAppHealthCheckCommand command(String runtimeCode) {
        FrontendAppHealthCheckCommand command = new FrontendAppHealthCheckCommand();
        command.setRuntimeCode(runtimeCode);
        return command;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void seedRegistry(String appCode, String appType, String healthCheckUrl, String entryUrl) {
        jdbcTemplate.update("""
                insert into authorization_frontend_app_registry
                (id, app_code, app_type, deploy_mode, entry_url, health_check_url, tenant_id)
                values (?, ?, ?, 'REMOTE', ?, ?, 'default')
                """, Math.abs(appCode.hashCode()), appCode, appType, entryUrl, healthCheckUrl);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists authorization_frontend_app_registry");
        jdbcTemplate.execute("""
                create table authorization_frontend_app_registry (
                    id bigint primary key,
                    app_code varchar(64) not null,
                    app_type varchar(32) not null default 'LOCAL',
                    deploy_mode varchar(32) not null default 'EMBEDDED',
                    entry_url varchar(500),
                    mount_path varchar(255),
                    active_rule varchar(255),
                    framework varchar(64),
                    version varchar(64),
                    health_check_url varchar(500),
                    sandbox_enabled tinyint not null default 0,
                    style_isolation varchar(32) not null default 'NONE',
                    tenant_id varchar(64) not null default 'default',
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp default current_timestamp,
                    updated_by bigint,
                    updated_at timestamp default current_timestamp
                )
                """);
    }

    @Configuration
    @MapperScan("io.mango.authorization.core.mapper")
    @Import(FrontendAppHealthService.class)
    static class TestConfig {

        @Bean
        OutboundNetworkProperties outboundNetworkProperties() {
            return new OutboundNetworkProperties();
        }
    }
}
