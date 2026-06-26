package io.mango.identity.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.identity.api.command.ChangeRequiredPasswordCommand;
import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.system.api.SysConfigApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityUserSecurityServiceTest {

    @Test
    void recordLoginFailureLocksUserAtThresholdAndSuccessClearsState() {
        IdentityUser user = new IdentityUser();
        user.setUserId(1001L);
        IdentityUserSecurityService service = newService(user);

        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        service.recordLoginFailure(1001L);
        assertThat(user.getLockedUntil()).isNull();

        service.recordLoginFailure(1001L);
        assertThat(user.getFailedLoginCount()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
        assertThat(user.getLockedReason()).isEqualTo("TOO_MANY_FAILED_LOGIN_ATTEMPTS");

        AuthUserInfo authUser = new AuthUserInfo();
        authUser.setUserId(1001L);
        authUser.setLockedUntil(user.getLockedUntil());
        assertThatThrownBy(() -> service.assertLoginAllowed(authUser))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账号已被临时锁定");

        service.recordLoginSuccess(1001L);
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLastFailedLoginAt()).isNull();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLockedReason()).isNull();
        assertThat(user.getLastLoginTime()).isNotNull();
    }

    @Test
    void changeRequiredPasswordClearsResetRequiredAndLockState() {
        IdentityUser user = new IdentityUser();
        user.setUserId(1002L);
        user.setPasswordResetRequired(true);
        user.setFailedLoginCount(5);
        user.setLastFailedLoginAt(LocalDateTime.now());
        user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
        user.setLockedReason("TOO_MANY_FAILED_LOGIN_ATTEMPTS");
        IdentityUserSecurityService service = newService(user);

        ChangeRequiredPasswordCommand command = new ChangeRequiredPasswordCommand();
        command.setUserId(1002L);
        command.setNewPassword("Mango@654321");
        command.setConfirmPassword("Mango@654321");

        service.changeRequiredPassword(command);

        assertThat(user.getPassword()).isNotBlank();
        assertThat(user.getPasswordResetRequired()).isFalse();
        assertThat(user.getPasswordUpdatedAt()).isNotNull();
        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLastFailedLoginAt()).isNull();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLockedReason()).isNull();
    }

    @Test
    void recordLoginFailureRestartsCountWhenFailureWindowExpired() {
        IdentityUser user = new IdentityUser();
        user.setUserId(1004L);
        user.setFailedLoginCount(4);
        user.setLastFailedLoginAt(LocalDateTime.now().minusMinutes(61));
        IdentityUserSecurityService service = newService(user);

        service.recordLoginFailure(1004L);

        assertThat(user.getFailedLoginCount()).isEqualTo(1);
        assertThat(user.getLastFailedLoginAt()).isNotNull();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void recordLoginFailureDoesNothingWhenLockDisabled() {
        IdentityUser user = new IdentityUser();
        user.setUserId(1003L);
        IdentitySecurityProperties properties = new IdentitySecurityProperties();
        properties.getLogin().setFailureLockEnabled(false);
        IdentityUserSecurityService service = newService(user, properties);

        service.recordLoginFailure(1003L);

        assertThat(user.getFailedLoginCount()).isNull();
        assertThat(user.getLockedUntil()).isNull();
    }

    private static IdentityUserSecurityService newService(IdentityUser user) {
        return newService(user, new IdentitySecurityProperties());
    }

    private static IdentityUserSecurityService newService(IdentityUser user, IdentitySecurityProperties properties) {
        IdentityUserMapper mapper = mock(IdentityUserMapper.class);
        when(mapper.selectById(user.getUserId())).thenReturn(user);
        when(mapper.updateById(user)).thenReturn(1);
        IdentitySecurityPolicyService policyService = new IdentitySecurityPolicyService(properties, emptyProvider());
        return new IdentityUserSecurityService(mapper, policyService,
                new IdentityPasswordPolicyService(policyService), new BCryptPasswordEncoder());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SysConfigApi> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
