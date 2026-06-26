package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityUserResourceHandlerTest {

    private final IdentityUserMapper userMapper = mock(IdentityUserMapper.class);
    private final TenantMemberMapper memberMapper = mock(TenantMemberMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final IdentityUserResourceHandler handler =
            new IdentityUserResourceHandler(userMapper, memberMapper, passwordEncoder);

    @Test
    void upsertEncodesInitialPasswordAndCreatesTenantMember() {
        ResourceDeclaration resource = resource();
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(memberMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(passwordEncoder.encode("demo123")).thenReturn("encoded-demo123");
        doAnswer(invocation -> {
            IdentityUser user = invocation.getArgument(0);
            user.setUserId(3001L);
            return 1;
        }).when(userMapper).insert(any(IdentityUser.class));
        doAnswer(invocation -> {
            TenantMember member = invocation.getArgument(0);
            member.setMemberId(4001L);
            return 1;
        }).when(memberMapper).insert(any(TenantMember.class));

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(result.getTargetId()).isEqualTo(3001L);
        assertThat(result.getTargetTable()).isEqualTo("identity_user");
        ArgumentCaptor<IdentityUser> userCaptor = ArgumentCaptor.forClass(IdentityUser.class);
        verify(userMapper).insert(userCaptor.capture());
        IdentityUser user = userCaptor.getValue();
        assertThat(user.getUsername()).isEqualTo("demo.admin");
        assertThat(user.getPassword()).isEqualTo("encoded-demo123");
        assertThat(user.getTenantId()).isEqualTo("1");
        assertThat(user.getRealm()).isEqualTo("INTERNAL");
        assertThat(user.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(user.getStatus()).isEqualTo(1);
        ArgumentCaptor<TenantMember> memberCaptor = ArgumentCaptor.forClass(TenantMember.class);
        verify(memberMapper).insert(memberCaptor.capture());
        TenantMember member = memberCaptor.getValue();
        assertThat(member.getTenantId()).isEqualTo(1L);
        assertThat(member.getUserId()).isEqualTo(3001L);
        assertThat(member.getMemberNo()).isEqualTo("DEMO-ADMIN");
        assertThat(member.getDisplayName()).isEqualTo("Demo Admin");
        assertThat(member.getMemberType()).isEqualTo("EMPLOYEE");
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.IDENTITY_USER);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "username", ResourceFieldType.STRING, "demo.admin");
        put(resource, "password", ResourceFieldType.STRING, "demo123");
        put(resource, "memberNo", ResourceFieldType.STRING, "DEMO-ADMIN");
        put(resource, "displayName", ResourceFieldType.STRING, "Demo Admin");
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        resource.getFields().put(name, field);
    }
}
