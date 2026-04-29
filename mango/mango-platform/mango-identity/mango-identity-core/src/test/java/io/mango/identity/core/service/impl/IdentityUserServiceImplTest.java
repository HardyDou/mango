package io.mango.identity.core.service.impl;

import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.mapper.IdentityUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        user.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(user);

        IdentityUserServiceImpl service = new IdentityUserServiceImpl(mapper);
        var profile = service.getUserInfo("admin");

        assertEquals(1L, profile.getUserId());
        assertEquals("admin", profile.getUsername());
        assertEquals("Administrator", profile.getNickname());
        assertEquals(1, profile.getStatus());
    }

    @Test
    @DisplayName("账号不存在时返回空")
    void getUserInfoShouldReturnNullWhenNotFound() {
        IdentityUserMapper mapper = mock(IdentityUserMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);

        IdentityUserServiceImpl service = new IdentityUserServiceImpl(mapper);

        assertNull(service.getUserInfo("missing"));
    }
}
