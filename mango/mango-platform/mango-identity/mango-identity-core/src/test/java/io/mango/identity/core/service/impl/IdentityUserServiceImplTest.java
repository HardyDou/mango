package io.mango.identity.core.service.impl;

import io.mango.authorization.core.entity.SubjectRoleBinding;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.api.enums.IdentityUserTargetType;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

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
                mock(TenantMemberOrgMapper.class),
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
                mock(TenantMemberOrgMapper.class),
                mock(SubjectRoleBindingMapper.class),
                mock(PasswordEncoder.class));

        assertNull(service.getUserInfo("missing"));
    }

    @Test
    @DisplayName("按部门目标解析当前租户启用用户")
    void listUserInfosByTarget_org_returnsEnabledUsers() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        try {
            IdentityUserMapper userMapper = mock(IdentityUserMapper.class);
            TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
            TenantMemberOrgMapper relationMapper = mock(TenantMemberOrgMapper.class);
            SubjectRoleBindingMapper roleBindingMapper = mock(SubjectRoleBindingMapper.class);
            TenantMemberOrgEntity relation = new TenantMemberOrgEntity();
            relation.setMemberId(10L);
            when(relationMapper.selectList(any())).thenReturn(List.of(relation));
            TenantMember member = new TenantMember();
            member.setMemberId(10L);
            member.setUserId(1001L);
            when(memberMapper.selectList(any())).thenReturn(List.of(member));
            IdentityUser user = new IdentityUser();
            user.setUserId(1001L);
            user.setUsername("admin");
            user.setNickname("管理员");
            user.setStatus(1);
            when(userMapper.selectList(any())).thenReturn(List.of(user));
            IdentityUserServiceImpl service = new IdentityUserServiceImpl(
                    userMapper,
                    memberMapper,
                    relationMapper,
                    roleBindingMapper,
                    mock(PasswordEncoder.class));
            IdentityUserTargetQuery query = new IdentityUserTargetQuery();
            query.setTargetType(IdentityUserTargetType.ORG);
            query.setTargetId(200L);
            query.setStatus(1);

            var users = service.listUserInfosByTarget(query);

            assertEquals(1, users.size());
            assertEquals(1001L, users.get(0).getUserId());
            assertEquals("admin", users.get(0).getUsername());
        } finally {
            MangoContextHolder.clear();
        }
    }

    @Test
    @DisplayName("按角色目标解析当前租户启用用户")
    void listUserInfosByTarget_role_returnsEnabledUsers() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        try {
            IdentityUserMapper userMapper = mock(IdentityUserMapper.class);
            TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
            TenantMemberOrgMapper relationMapper = mock(TenantMemberOrgMapper.class);
            SubjectRoleBindingMapper roleBindingMapper = mock(SubjectRoleBindingMapper.class);
            SubjectRoleBinding binding = new SubjectRoleBinding();
            binding.setSubjectId(10L);
            when(roleBindingMapper.selectList(any())).thenReturn(List.of(binding));
            TenantMember member = new TenantMember();
            member.setMemberId(10L);
            member.setUserId(1001L);
            when(memberMapper.selectList(any())).thenReturn(List.of(member));
            IdentityUser user = new IdentityUser();
            user.setUserId(1001L);
            user.setUsername("admin");
            user.setStatus(1);
            when(userMapper.selectList(any())).thenReturn(List.of(user));
            IdentityUserServiceImpl service = new IdentityUserServiceImpl(
                    userMapper,
                    memberMapper,
                    relationMapper,
                    roleBindingMapper,
                    mock(PasswordEncoder.class));
            IdentityUserTargetQuery query = new IdentityUserTargetQuery();
            query.setTargetType(IdentityUserTargetType.ROLE);
            query.setTargetId(300L);
            query.setStatus(1);

            var users = service.listUserInfosByTarget(query);

            assertEquals(1, users.size());
            assertEquals(1001L, users.get(0).getUserId());
        } finally {
            MangoContextHolder.clear();
        }
    }
}
