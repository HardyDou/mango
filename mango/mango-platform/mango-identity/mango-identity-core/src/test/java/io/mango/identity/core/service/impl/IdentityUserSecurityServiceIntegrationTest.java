package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.common.exception.BizException;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.system.api.SysConfigApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        IdentityUserSecurityServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity_user_security_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class IdentityUserSecurityServiceIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserMapper userMapper;

    @Autowired
    private IdentityUserSecurityService service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    void recordLoginFailureLocksUserAtThresholdAndSuccessClearsPersistedState() {
        seedUser(1001L, "login-lock", "Initial@123");

        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        IdentityUser beforeThreshold = userMapper.selectById(1001L);
        assertThat(beforeThreshold.getFailedLoginCount()).isEqualTo(4);
        assertThat(beforeThreshold.getLockedUntil()).isNull();

        service.recordLoginFailure(1001L);
        IdentityUser locked = userMapper.selectById(1001L);
        assertThat(locked.getFailedLoginCount()).isEqualTo(5);
        assertThat(locked.getLockedUntil()).isAfter(LocalDateTime.now());
        assertThat(locked.getLockedReason()).isEqualTo("TOO_MANY_FAILED_LOGIN_ATTEMPTS");

        AuthUserInfo authUser = new AuthUserInfo();
        authUser.setUserId(1001L);
        authUser.setLockedUntil(locked.getLockedUntil());
        assertThatThrownBy(() -> service.assertLoginAllowed(authUser))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账号已被临时锁定");

        service.recordLoginSuccess(1001L);
        IdentityUser cleared = userMapper.selectById(1001L);
        assertThat(cleared.getFailedLoginCount()).isZero();
        assertThat(cleared.getLastFailedLoginAt()).isNull();
        assertThat(cleared.getLockedUntil()).isNull();
        assertThat(cleared.getLockedReason()).isNull();
        assertThat(cleared.getLastLoginTime()).isNotNull();
    }

    @Test
    void changeRequiredPasswordClearsResetRequiredAndLockStateThroughRealMapper() {
        seedLockedPasswordResetUser(1002L, "reset-required", "Initial@123");
        ChangeRequiredPasswordCommand command = new ChangeRequiredPasswordCommand();
        command.setUserId(1002L);
        command.setNewPassword("Mango@654321");
        command.setConfirmPassword("Mango@654321");

        service.changeRequiredPassword(command);

        IdentityUser persisted = userMapper.selectById(1002L);
        assertThat(passwordEncoder.matches("Mango@654321", persisted.getPassword())).isTrue();
        assertThat(persisted.getPasswordResetRequired()).isFalse();
        assertThat(persisted.getPasswordUpdatedAt()).isNotNull();
        assertThat(persisted.getFailedLoginCount()).isZero();
        assertThat(persisted.getLastFailedLoginAt()).isNull();
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockedReason()).isNull();
    }

    @Test
    void recordLoginFailureRestartsPersistedCountWhenFailureWindowExpired() {
        seedUser(1004L, "expired-window", "Initial@123");
        jdbcTemplate.update("""
                        update identity_user
                        set failed_login_count = 4,
                            last_failed_login_at = ?,
                            update_time = current_timestamp
                        where id = ?
                        """,
                LocalDateTime.now().minusMinutes(61), 1004L);

        service.recordLoginFailure(1004L);

        IdentityUser persisted = userMapper.selectById(1004L);
        assertThat(persisted.getFailedLoginCount()).isEqualTo(1);
        assertThat(persisted.getLastFailedLoginAt()).isNotNull();
        assertThat(persisted.getLockedUntil()).isNull();
    }

    @Test
    void unlockClearsPersistedLockState() {
        seedLockedPasswordResetUser(1005L, "locked-user", "Initial@123");

        boolean unlocked = service.unlock(1005L);

        IdentityUser persisted = userMapper.selectById(1005L);
        assertThat(unlocked).isTrue();
        assertThat(persisted.getFailedLoginCount()).isZero();
        assertThat(persisted.getLastFailedLoginAt()).isNull();
        assertThat(persisted.getLockedUntil()).isNull();
        assertThat(persisted.getLockedReason()).isNull();
    }

    @Test
    void requirePasswordResetPersistsResetFlag() {
        seedUser(1006L, "need-reset", "Initial@123");

        boolean updated = service.requirePasswordReset(1006L);

        IdentityUser persisted = userMapper.selectById(1006L);
        assertThat(updated).isTrue();
        assertThat(persisted.getPasswordResetRequired()).isTrue();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("""
                create table identity_user (
                    id bigint primary key,
                    username varchar(100) not null,
                    password varchar(255),
                    password_reset_required boolean not null default false,
                    password_updated_at timestamp,
                    nickname varchar(100),
                    realm varchar(32) not null default 'INTERNAL',
                    actor_type varchar(32) not null default 'INTERNAL_USER',
                    party_type varchar(32),
                    party_id bigint,
                    email varchar(128),
                    phone varchar(32),
                    avatar varchar(255),
                    status tinyint not null default 1,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    last_login_time timestamp,
                    failed_login_count int,
                    last_failed_login_at timestamp,
                    locked_until timestamp,
                    locked_reason varchar(100),
                    remark varchar(500),
                    tenant_id varchar(64)
                )
                """);
    }

    private void seedUser(Long userId, String username, String rawPassword) {
        jdbcTemplate.update("""
                        insert into identity_user
                        (id, username, password, password_reset_required, nickname, realm, actor_type, status, tenant_id)
                        values (?, ?, ?, false, ?, 'INTERNAL', 'INTERNAL_USER', 1, '1')
                        """,
                userId, username, passwordEncoder.encode(rawPassword), username);
    }

    private void seedLockedPasswordResetUser(Long userId, String username, String rawPassword) {
        seedUser(userId, username, rawPassword);
        jdbcTemplate.update("""
                        update identity_user
                        set password_reset_required = true,
                            failed_login_count = 5,
                            last_failed_login_at = ?,
                            locked_until = ?,
                            locked_reason = 'TOO_MANY_FAILED_LOGIN_ATTEMPTS',
                            update_time = current_timestamp
                        where id = ?
                        """,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), userId);
    }

    @Configuration
    @MapperScan(basePackageClasses = IdentityUserMapper.class)
    @Import({
            IdentityUserSecurityService.class,
            IdentitySecurityPolicyService.class,
            IdentityPasswordPolicyService.class
    })
    static class TestConfig {

        @Bean
        IdentitySecurityProperties identitySecurityProperties() {
            return new IdentitySecurityProperties();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        ObjectProvider<SysConfigApi> sysConfigApiProvider() {
            return new ObjectProvider<>() {
                @Override
                public SysConfigApi getObject(Object... args) {
                    return null;
                }

                @Override
                public SysConfigApi getIfAvailable() {
                    return null;
                }

                @Override
                public SysConfigApi getIfUnique() {
                    return null;
                }

                @Override
                public SysConfigApi getObject() {
                    return null;
                }
            };
        }
    }
}
