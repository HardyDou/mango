package io.mango.identity.core.service.impl;

import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("身份用户服务测试")
class IdentityUserServiceImplTest {

    @Test
    @DisplayName("查询身份资料时只返回用户资料字段")
    void getUserInfoShouldMapIdentityProfileOnly() {
        IdentityUserMapper mapper = mock(IdentityUserMapper.class);
        IdentityUser user = new IdentityUser();
        user.setUserId(1L);
        user.setUsername("admin");
        user.setNickname("Administrator");
        user.setRealm("INTERNAL");
        user.setActorType("INTERNAL_USER");
        user.setPartyType("COMPANY");
        user.setPartyId(9001L);
        user.setEmail("admin@example.com");
        user.setPhone("13800138000");
        user.setAvatar("https://example.com/avatar.png");
        user.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(user);

        IdentityUserServiceImpl service = new IdentityUserServiceImpl(
                mapper,
                mock(TenantMemberMapper.class),
                mock(SubjectRoleBindingMapper.class),
                mock(PasswordEncoder.class));
        var profile = service.getUserInfo("admin");

        assertEquals(1L, profile.getUserId());
        assertEquals("admin", profile.getUsername());
        assertEquals("Administrator", profile.getNickname());
        assertEquals("INTERNAL", profile.getRealm());
        assertEquals("INTERNAL_USER", profile.getActorType());
        assertEquals("COMPANY", profile.getPartyType());
        assertEquals(9001L, profile.getPartyId());
        assertEquals("admin@example.com", profile.getEmail());
        assertEquals("13800138000", profile.getPhone());
        assertEquals("https://example.com/avatar.png", profile.getAvatar());
        assertEquals(1, profile.getStatus());
    }

    @Test
    @DisplayName("账号不存在时返回空")
    void getUserInfoShouldReturnNullWhenNotFound() {
        IdentityUserMapper mapper = mock(IdentityUserMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        IdentityUserServiceImpl service = new IdentityUserServiceImpl(
                mapper,
                mock(TenantMemberMapper.class),
                mock(SubjectRoleBindingMapper.class),
                mock(PasswordEncoder.class));

        assertNull(service.getUserInfo("missing"));
    }
}
