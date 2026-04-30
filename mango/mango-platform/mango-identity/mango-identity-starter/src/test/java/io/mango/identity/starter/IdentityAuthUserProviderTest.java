package io.mango.identity.starter;

import io.mango.identity.api.vo.AuthUserInfo;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.service.IIdentityUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("本地身份认证用户 Provider 测试")
class IdentityAuthUserProviderTest {

    @Test
    @DisplayName("认证用户事实应包含密码哈希与身份上下文")
    void getByUsernameForAuthShouldMapPasswordAndIdentityContext() {
        IIdentityUserService identityUserService = mock(IIdentityUserService.class);
        IdentityUser user = new IdentityUser();
        user.setUserId(1001L);
        user.setUsername("admin");
        user.setPassword("{bcrypt}hash");
        user.setNickname("管理员");
        user.setRealm("INTERNAL");
        user.setActorType("INTERNAL_USER");
        user.setPartyType("COMPANY");
        user.setPartyId(9001L);
        user.setStatus(1);
        when(identityUserService.getByUsername("admin", "INTERNAL")).thenReturn(user);

        IdentityAuthUserProvider provider = new IdentityAuthUserProvider(identityUserService);
        AuthUserInfo authUser = provider.getByUsernameForAuth("admin", "INTERNAL");

        assertEquals(1001L, authUser.getUserId());
        assertEquals("admin", authUser.getUsername());
        assertEquals("{bcrypt}hash", authUser.getPassword());
        assertEquals("管理员", authUser.getNickname());
        assertEquals("INTERNAL", authUser.getRealm());
        assertEquals("INTERNAL_USER", authUser.getActorType());
        assertEquals("COMPANY", authUser.getPartyType());
        assertEquals(9001L, authUser.getPartyId());
        assertEquals(1, authUser.getStatus());
    }
}
