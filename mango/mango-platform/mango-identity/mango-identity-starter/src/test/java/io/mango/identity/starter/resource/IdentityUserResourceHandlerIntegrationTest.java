package io.mango.identity.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.mapper.TenantMemberLifecycleLogMapper;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.enums.ResourceStatus;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceField;
import io.mango.resource.support.model.ResourceSyncResult;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final String ENCODED_ADMIN_PASSWORD =
            "$2a$10$xktxOwcAfFdqNAKKpWICDuV8MTEEshM9K1CtofRWA34v2OGoarvHa";
    private static final String INITIALIZED_AT = "2026-07-16T09:00:00";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserMapper userMapper;

    @Autowired
    private TenantMemberMapper memberMapper;

    @Autowired
    private TenantMemberLifecycleLogMapper lifecycleLogMapper;

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
        put(resource, "targetId", ResourceFieldType.LONG, 1L);

        ResourceSyncResult result = handler.upsert(resource);

        assertThat(handler.resourceType()).isEqualTo(ResourceTypes.IDENTITY_USER);
        assertThat(result.getTargetTable()).isEqualTo("identity_user");
        assertThat(result.getTargetId()).isEqualTo(1L);

        IdentityUserEntity user = userMapper.selectById(result.getTargetId());
        assertThat(user.getUsername()).isEqualTo("demo.admin");
        assertThat(passwordEncoder.matches("demo123", user.getPassword())).isTrue();
        assertThat(user.getTenantId()).isEqualTo("1");
        assertThat(user.getRealm()).isEqualTo("INTERNAL");
        assertThat(user.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(user.getPartyType()).isEqualTo("INTERNAL_ORG");
        assertThat(user.getStatus()).isEqualTo(1);

        TenantMemberEntity member = memberMapper.selectList(null).get(0);
        assertThat(member.getTenantId()).isEqualTo("1");
        assertThat(member.getMemberId()).isEqualTo(1001L);
        assertThat(member.getUserId()).isEqualTo(user.getUserId());
        assertThat(member.getMemberNo()).isEqualTo("DEMO-ADMIN");
        assertThat(member.getDisplayName()).isEqualTo("Demo Admin");
        assertThat(member.getMemberType()).isEqualTo("EMPLOYEE");
        assertThat(member.getStatus()).isEqualTo(1);
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(countMembers()).isEqualTo(1L);
        assertThat(countLifecycleEvents("CREATED")).isEqualTo(1L);
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
        IdentityUserEntity user = userMapper.selectById(created.getTargetId());
        assertThat(user.getNickname()).isEqualTo("Updated Nickname");
        assertThat(user.getStatus()).isZero();
        assertThat(user.getRemark()).isEqualTo("updated by resource sync");
        TenantMemberEntity member = memberMapper.selectList(null).get(0);
        assertThat(member.getDisplayName()).isEqualTo("Updated Admin");
        assertThat(member.getStatus()).isZero();
        assertThat(member.getRemark()).isEqualTo("updated by resource sync");
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(countMembers()).isEqualTo(1L);
        assertThat(countLifecycleEvents("CREATED")).isEqualTo(1L);
    }

    @Test
    void upsertPreservesExistingBusinessKeyIdInsteadOfReplacingItWithDeclaredTargetId() {
        ResourceSyncResult created = handler.upsert(resource());
        ResourceDeclaration update = resource();
        put(update, "targetId", ResourceFieldType.LONG, 1L);

        ResourceSyncResult updated = handler.upsert(update);

        assertThat(updated.getTargetId()).isEqualTo(created.getTargetId());
        assertThat(updated.getTargetId()).isNotEqualTo(1L);
        assertThat(countUsers()).isEqualTo(1L);
        assertThat(memberMapper.selectList(null).get(0).getUserId()).isEqualTo(created.getTargetId());
    }

    @Test
    void upsertPersistsEncodedPasswordDeterministicallyAcrossEmptyDatabases() {
        ResourceDeclaration firstResource = resource();
        firstResource.removeField("password");
        put(firstResource, "targetId", ResourceFieldType.LONG, 1L);
        put(firstResource, "encodedPassword", ResourceFieldType.STRING, ENCODED_ADMIN_PASSWORD);

        ResourceSyncResult first = handler.upsert(firstResource);
        String firstStoredPassword = userMapper.selectById(first.getTargetId()).getPassword();

        resetSchema();
        ResourceDeclaration secondResource = resource();
        secondResource.removeField("password");
        put(secondResource, "targetId", ResourceFieldType.LONG, 1L);
        put(secondResource, "encodedPassword", ResourceFieldType.STRING, ENCODED_ADMIN_PASSWORD);
        ResourceSyncResult second = handler.upsert(secondResource);
        String secondStoredPassword = userMapper.selectById(second.getTargetId()).getPassword();

        assertThat(firstStoredPassword).isEqualTo(ENCODED_ADMIN_PASSWORD);
        assertThat(secondStoredPassword).isEqualTo(firstStoredPassword);
        assertThat(passwordEncoder.matches("admin123", secondStoredPassword)).isTrue();
    }

    @Test
    void upsert_sameDeclarationAcrossEmptyDatabases_persistsIdenticalTableSnapshots() {
        ResourceDeclaration declaration = resource();
        declaration.removeField("password");
        put(declaration, "targetId", ResourceFieldType.LONG, 1L);
        put(declaration, "encodedPassword", ResourceFieldType.STRING, ENCODED_ADMIN_PASSWORD);

        handler.upsert(declaration);
        IdentityResourceSnapshot first = snapshot();

        resetSchema();
        handler.upsert(declaration.copy());
        IdentityResourceSnapshot second = snapshot();

        long expectedEventId = PortableResourceIds.stable(
                "tenant_member_lifecycle_log", "1", 1001L, "CREATED");
        assertThat(second).isEqualTo(first);
        assertThat(second.lifecycleEvents()).singleElement().satisfies(row -> {
            assertThat(row.get("id")).isEqualTo(expectedEventId);
            assertThat(row.get("occurred_at").toString()).startsWith("2026-07-16 09:00");
        });
    }

    @Test
    void upsert_missingInitializationTime_rejectsWithoutWrites() {
        ResourceDeclaration declaration = resource();
        declaration.removeField("initializedAt");

        assertThatThrownBy(() -> handler.upsert(declaration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("field is required: initializedAt");
        assertThat(countUsers()).isZero();
        assertThat(countMembers()).isZero();
        assertThat(countLifecycleEvents("CREATED")).isZero();
    }

    @Test
    void upsert_stableLifecycleIdOccupiedByDifferentEvent_rejectsWithoutWrites() {
        long eventId = PortableResourceIds.stable(
                "tenant_member_lifecycle_log", "1", 1001L, "CREATED");
        jdbcTemplate.update("""
                        insert into tenant_member_lifecycle_log
                            (id, tenant_id, user_id, member_id, event_type, occurred_at)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                eventId, 9L, 9L, 9L, "REMOVED", INITIALIZED_AT.replace('T', ' '));

        ResourceDeclaration declaration = resource();
        put(declaration, "targetId", ResourceFieldType.LONG, 1L);

        assertThatThrownBy(() -> handler.upsert(declaration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("portable lifecycle event ID collision");
        assertThat(countUsers()).isZero();
        assertThat(countMembers()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log", Long.class)).isEqualTo(1L);
    }

    @Test
    void upsertRejectsAmbiguousPlainAndEncodedPasswords() {
        ResourceDeclaration resource = resource();
        put(resource, "encodedPassword", ResourceFieldType.STRING, ENCODED_ADMIN_PASSWORD);

        assertThatThrownBy(() -> handler.upsert(resource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("password and encodedPassword cannot both be declared");
        assertThat(countUsers()).isZero();
        assertThat(countMembers()).isZero();
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

    @Test
    void upsertAdditionalTenantMemberKeepsGlobalUserTenantAndStableMemberIds() {
        ResourceSyncResult created = handler.upsert(resource());
        ResourceDeclaration additionalTenant = resource();
        put(additionalTenant, "tenantId", ResourceFieldType.LONG, 2L);
        put(additionalTenant, "memberId", ResourceFieldType.LONG, 1002L);
        put(additionalTenant, "memberNo", ResourceFieldType.STRING, "ADMIN-company_a");
        put(additionalTenant, "displayName", ResourceFieldType.STRING, "A公司管理员");

        ResourceSyncResult updated = handler.upsert(additionalTenant);

        assertThat(updated.getTargetId()).isEqualTo(created.getTargetId());
        assertThat(userMapper.selectById(created.getTargetId()).getTenantId()).isEqualTo("1");
        assertThat(memberMapper.selectList(null))
                .extracting(TenantMemberEntity::getMemberId)
                .containsExactlyInAnyOrder(1001L, 1002L);
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists tenant_member_lifecycle_log");
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
                    real_name varchar(100),
                    document_type varchar(32),
                    document_number varchar(128),
                    verification_status varchar(32) not null default 'UNVERIFIED',
                    verification_source varchar(64),
                    status tinyint not null default 1,
                    create_time timestamp not null default current_timestamp,
                    update_time timestamp not null default current_timestamp,
                    last_login_time timestamp,
                    failed_login_count int,
                    last_failed_login_at timestamp,
                    locked_until timestamp,
                    locked_reason varchar(100),
                    remark varchar(500),
                    tenant_id varchar(64),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
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
                    remark varchar(500),
                    org_id bigint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member_lifecycle_log (
                    id bigint primary key,
                    tenant_id bigint not null,
                    org_id bigint,
                    user_id bigint not null,
                    member_id bigint not null,
                    event_type varchar(16) not null,
                    operator_user_id bigint,
                    occurred_at timestamp not null,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
    }

    private ResourceDeclaration resource() {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.IDENTITY_USER);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, "memberId", ResourceFieldType.LONG, 1001L);
        put(resource, "initializedAt", ResourceFieldType.DATETIME, INITIALIZED_AT);
        put(resource, "username", ResourceFieldType.STRING, "demo.admin");
        put(resource, "password", ResourceFieldType.STRING, "demo123");
        put(resource, "memberNo", ResourceFieldType.STRING, "DEMO-ADMIN");
        put(resource, "displayName", ResourceFieldType.STRING, "Demo Admin");
        return resource;
    }

    private void put(ResourceDeclaration resource, String name, ResourceFieldType type, Object value) {
        resource.putField(name, field(type, value));
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

    private Long countLifecycleEvents(String eventType) {
        return jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where event_type = ?", Long.class, eventType);
    }

    private IdentityResourceSnapshot snapshot() {
        return new IdentityResourceSnapshot(
                jdbcTemplate.queryForList("select * from identity_user order by id"),
                jdbcTemplate.queryForList("select * from tenant_member order by id"),
                jdbcTemplate.queryForList("select * from tenant_member_lifecycle_log order by id"));
    }

    private record IdentityResourceSnapshot(
            List<Map<String, Object>> users,
            List<Map<String, Object>> members,
            List<Map<String, Object>> lifecycleEvents) {
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
