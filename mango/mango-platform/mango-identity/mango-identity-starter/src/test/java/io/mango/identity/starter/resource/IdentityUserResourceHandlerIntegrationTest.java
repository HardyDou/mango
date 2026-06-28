package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        IdentityUserResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity_user_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
class IdentityUserResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserMapper userMapper;

    @Autowired
    private TenantMemberMapper memberMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private IdentityUserResourceHandler handler;

    @BeforeEach
    void setUp() {
        resetSchema();
    }

    @Test
    void upsertEncodesInitialPasswordAndCreatesTenantMemberThroughRealMappers() {
        ResourceDeclaration resource = resource();

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.IDENTITY_USER);
        assertThat(result.getTargetTable()).isEqualTo("identity_user");
        assertThat(result.getTargetId()).isNotNull();

        IdentityUser user = userMapper.selectById(result.getTargetId());
        assertThat(user.getUsername()).isEqualTo("demo.admin");
        assertThat(passwordEncoder.matches("demo123", user.getPassword())).isTrue();
        assertThat(user.getTenantId()).isEqualTo("1");
        assertThat(user.getRealm()).isEqualTo("INTERNAL");
        assertThat(user.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(user.getPartyType()).isEqualTo("INTERNAL_ORG");
        assertThat(user.getStatus()).isEqualTo(1);

        TenantMember member = memberMapper.selectList(null).get(0);
        assertThat(member.getTenantId()).isEqualTo(1L);
        assertThat(member.getUserId()).isEqualTo(user.getUserId());
        assertThat(member.getMemberNo()).isEqualTo("DEMO-ADMIN");
        assertThat(member.getDisplayName()).isEqualTo("Demo Admin");
        assertThat(member.getMemberType()).isEqualTo("EMPLOYEE");
        assertThat(member.getStatus()).isEqualTo(1);
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(countMembers()).isEqualTo(1L);
    }

    @Test
    void upsertUpdatesExistingUserAndMemberByBusinessKeyThroughRealMappers() {
        ResourceSyncResult created = handler.upsert(resource());
        ResourceDeclaration update = resource();
        put(update, "displayName", ResourceFieldType.STRING, "Updated Admin");
        put(update, "nickname", ResourceFieldType.STRING, "Updated Nickname");
        put(update, "status", ResourceFieldType.INT, 0);
        put(update, "remark", ResourceFieldType.STRING, "updated by resource sync");

        ResourceSyncResult updated = handler.upsert(update);

        assertThat(updated.getTargetId()).isEqualTo(created.getTargetId());
        IdentityUser user = userMapper.selectById(created.getTargetId());
        assertThat(user.getNickname()).isEqualTo("Updated Nickname");
        assertThat(user.getStatus()).isZero();
        assertThat(user.getRemark()).isEqualTo("updated by resource sync");
        TenantMember member = memberMapper.selectList(null).get(0);
        assertThat(member.getDisplayName()).isEqualTo("Updated Admin");
        assertThat(member.getStatus()).isZero();
        assertThat(member.getRemark()).isEqualTo("updated by resource sync");
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(countMembers()).isEqualTo(1L);
    }

    @Test
    void disableUpdatesUserAndMemberStatusThroughRealMappers() {
        ResourceSyncResult created = handler.upsert(resource());

        ResourceSyncResult disabled = handler.disable(resource());

        assertThat(disabled.getTargetId()).isEqualTo(created.getTargetId());
        assertThat(userMapper.selectById(created.getTargetId()).getStatus()).isZero();
        assertThat(memberMapper.selectList(null).get(0).getStatus()).isZero();
    }

    @Test
    void disabledResourceDeclarationCreatesInactiveUserAndMemberThroughRealMappers() {
        ResourceDeclaration resource = resource();
        resource.setStatus(ResourceStatus.DISABLED);

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(userMapper.selectById(result.getTargetId()).getStatus()).isZero();
        assertThat(memberMapper.selectList(null).get(0).getStatus()).isZero();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists tenant_member");
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
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint primary key,
                    tenant_id bigint not null,
                    user_id bigint not null,
                    member_no varchar(64),
                    display_name varchar(100),
                    member_type varchar(32),
                    status tinyint not null default 1,
                    primary_org_id bigint,
                    primary_post_id bigint,
                    joined_at timestamp,
                    left_at timestamp,
                    remark varchar(500)
                )
                """);
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
        resource.getFields().put(name, field(type, value));
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }

    private Long countUsers() {
        return jdbcTemplate.queryForObject("select count(*) from identity_user", Long.class);
    }

    private Long countMembers() {
        return jdbcTemplate.queryForObject("select count(*) from tenant_member", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = IdentityUserMapper.class)
    @Import(IdentityUserResourceHandler.class)
    static class TestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
