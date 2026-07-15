package io.mango.authorization.starter.resource;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationSubjectReferenceProvider;
import io.mango.authorization.core.entity.SubjectRoleBindingEntity;
import io.mango.authorization.core.mapper.RoleMapper;
import io.mango.authorization.core.mapper.SubjectRoleBindingMapper;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.resource.api.ResourceTypes;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        AuthSubjectRoleResourceHandlerIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_subject_role_resource_handler;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=true",
        "mango.persistence.mybatis-plus.tenant.default-tenant-id=1"
})
class AuthSubjectRoleResourceHandlerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SubjectRoleBindingMapper bindingMapper;

    @Autowired
    private AuthSubjectRoleResourceHandler handler;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("drop table if exists authorization_subject_role");
        jdbcTemplate.execute("drop table if exists authorization_role");
        jdbcTemplate.execute("drop table if exists tenant_member");
        jdbcTemplate.execute("drop table if exists identity_user");
        jdbcTemplate.execute("""
                create table authorization_role (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null,
                    realm varchar(32) not null,
                    actor_type varchar(32),
                    role_code varchar(100) not null,
                    role_name varchar(50) not null,
                    role_type tinyint not null default 1,
                    status tinyint not null default 1,
                    sort int not null default 0,
                    create_time timestamp,
                    update_time timestamp,
                    remark varchar(500)
                )
                """);
        jdbcTemplate.execute("""
                create table authorization_subject_role (
                    id bigint auto_increment primary key,
                    tenant_id bigint not null,
                    subject_id bigint not null,
                    subject_type varchar(32) not null,
                    app_code varchar(64) not null,
                    realm varchar(32) not null,
                    actor_type varchar(32),
                    party_type varchar(32),
                    party_id bigint,
                    role_id bigint not null
                )
                """);
        jdbcTemplate.execute("""
                create table identity_user (
                    id bigint primary key,
                    username varchar(64) not null,
                    password varchar(255),
                    password_reset_required boolean,
                    password_updated_at timestamp,
                    nickname varchar(64),
                    realm varchar(32),
                    actor_type varchar(32),
                    party_type varchar(32),
                    party_id bigint,
                    email varchar(128),
                    phone varchar(32),
                    avatar varchar(255),
                    status tinyint,
                    create_time timestamp,
                    update_time timestamp,
                    last_login_time timestamp,
                    failed_login_count int,
                    last_failed_login_at timestamp,
                    locked_until timestamp,
                    locked_reason varchar(255),
                    remark varchar(500),
                    tenant_id varchar(64)
                )
                """);
        jdbcTemplate.execute("""
                create table tenant_member (
                    id bigint primary key,
                    tenant_id bigint not null,
                    user_id bigint not null,
                    member_no varchar(100),
                    display_name varchar(100),
                    member_type varchar(32),
                    status tinyint,
                    primary_org_id bigint,
                    primary_post_id bigint,
                    joined_at timestamp,
                    left_at timestamp,
                    remark varchar(500)
                )
                """);
        AuthorizationStarterTestSchema.ensureCanonicalColumns(jdbcTemplate);
        jdbcTemplate.update("""
                insert into authorization_role(id, tenant_id, app_code, realm, actor_type, role_code, role_name, role_type, status, sort)
                values (101, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_DEMO', 'Demo RoleEntity', 2, 1, 10)
                """);
        jdbcTemplate.update("""
                insert into authorization_role(id, tenant_id, app_code, realm, actor_type, role_code, role_name, role_type, status, sort)
                values (102, 1, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_AUDIT', 'Audit RoleEntity', 2, 1, 20)
                """);
        jdbcTemplate.update("""
                insert into identity_user(id, username, realm, actor_type, status, tenant_id)
                values (201, 'demo-user', 'INTERNAL', 'INTERNAL_USER', 1, '1')
                """);
        jdbcTemplate.update("""
                insert into tenant_member(id, tenant_id, user_id, member_no, display_name, member_type, status)
                values (301, 1, 201, 'MB-DEMO', 'Demo Member', 'EMPLOYEE', 1)
                """);
        jdbcTemplate.update("""
                insert into identity_user(id, username, realm, actor_type, status, tenant_id)
                values (202, 'other-user', 'INTERNAL', 'INTERNAL_USER', 1, '1')
                """);
        jdbcTemplate.update("""
                insert into tenant_member(id, tenant_id, user_id, member_no, display_name, member_type, status, left_at)
                values (302, 1, 202, 'MB-LEFT', 'Left Member', 'EMPLOYEE', 1, current_timestamp)
                """);
    }

    @Test
    void specAllowsStableSubjectKeysWithoutRequiredSubjectId() {
        ResourceHandlerSpec spec = handler.spec();

        assertThat(spec.getResourceType()).isEqualTo(ResourceTypes.AUTH_SUBJECT_ROLE);
        assertThat(spec.getRequiredFields()).contains("tenantId", "roleCodes");
        assertThat(spec.getRequiredFields()).doesNotContain("subjectId");
        assertThat(spec.getFieldDescriptions()).containsKeys("subjectCode", "memberNo", "username");
    }

    @Test
    void upsertKeepsSubjectIdCompatibilityThroughRealMapper() {
        ResourceSyncResult result = handler.upsert(resource("subjectId", 301L));

        SubjectRoleBindingEntity binding = bindingMapper.selectById(result.getTargetId());
        assertThat(binding.getTenantId()).isEqualTo("1");
        assertThat(binding.getSubjectId()).isEqualTo(301L);
        assertThat(binding.getSubjectType()).isEqualTo("TENANT_MEMBER");
        assertThat(binding.getRoleId()).isEqualTo(101L);
    }

    @Test
    void upsertResolvesSubjectCodeFromTenantMemberNoThroughRealMapper() {
        ResourceSyncResult result = handler.upsert(resource("subjectCode", "MB-DEMO"));

        SubjectRoleBindingEntity binding = bindingMapper.selectById(result.getTargetId());
        assertThat(binding.getSubjectId()).isEqualTo(301L);
        assertThat(binding.getRoleId()).isEqualTo(101L);
    }

    @Test
    void upsertResolvesMemberNoThroughRealMapper() {
        ResourceSyncResult result = handler.upsert(resource("memberNo", "MB-DEMO"));

        SubjectRoleBindingEntity binding = bindingMapper.selectById(result.getTargetId());
        assertThat(binding.getSubjectId()).isEqualTo(301L);
    }

    @Test
    void upsertResolvesUsernameThroughIdentityUserAndTenantMember() {
        ResourceSyncResult result = handler.upsert(resource("username", "demo-user"));

        SubjectRoleBindingEntity binding = bindingMapper.selectById(result.getTargetId());
        assertThat(binding.getSubjectId()).isEqualTo(301L);
    }

    @Test
    void disableResolvesStableSubjectAndDeletesMatchingBindings() {
        handler.upsert(resource("subjectCode", "MB-DEMO"));

        ResourceSyncResult result = handler.disable(resource("username", "demo-user"));

        assertThat(result.getMessage()).contains("changed=1");
        assertThat(bindingCount()).isZero();
    }

    @Test
    void upsertFailsClearlyWhenStableSubjectCannotBeResolved() {
        assertThatThrownBy(() -> handler.upsert(resource("username", "missing-user")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AUTH_SUBJECT_ROLE referenced subject does not exist");
    }

    @Test
    void upsertFailsWhenResolvedMemberHasLeftTenant() {
        assertThatThrownBy(() -> handler.upsert(resource("memberNo", "MB-LEFT")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AUTH_SUBJECT_ROLE referenced subject does not exist");
    }

    @Test
    void upsertResolvesRoleInsideDeclarationTenantInsteadOfAmbientDefaultTenant() {
        jdbcTemplate.update("""
                insert into authorization_role(id, tenant_id, app_code, realm, actor_type, role_code, role_name, role_type, status, sort)
                values (201, 2, 'internal-admin', 'INTERNAL', 'INTERNAL_USER', 'ROLE_DEMO', 'Tenant 2 Demo Role', 2, 1, 10)
                """);
        ResourceDeclaration resource = resource("subjectId", 401L);
        put(resource, "tenantId", ResourceFieldType.LONG, 2L);

        handler.upsert(resource);

        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from authorization_subject_role
                where tenant_id = 2 and subject_id = 401 and role_id = 201
                """, Long.class)).isEqualTo(1L);
    }

    private ResourceDeclaration resource(String subjectField, Object subjectValue) {
        ResourceDeclaration resource = new ResourceDeclaration();
        resource.setResourceType(ResourceTypes.AUTH_SUBJECT_ROLE);
        resource.setFields(new LinkedHashMap<>());
        put(resource, "tenantId", ResourceFieldType.LONG, 1L);
        put(resource, subjectField, fieldType(subjectValue), subjectValue);
        put(resource, "roleCodes", ResourceFieldType.LIST, List.of("ROLE_DEMO"));
        return resource;
    }

    private ResourceFieldType fieldType(Object value) {
        return value instanceof Number ? ResourceFieldType.LONG : ResourceFieldType.STRING;
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

    private Long bindingCount() {
        return jdbcTemplate.queryForObject("select count(*) from authorization_subject_role", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = {
            RoleMapper.class,
            SubjectRoleBindingMapper.class
    })
    @Import(AuthSubjectRoleResourceHandler.class)
    static class TestConfig {

        @Bean
        AuthorizationSubjectReferenceProvider subjectReferenceProvider(JdbcTemplate jdbcTemplate) {
            return (tenantId, memberNo, username) -> {
                if (memberNo != null && !memberNo.isBlank()) {
                    return jdbcTemplate.query("""
                                    select id from tenant_member
                                    where tenant_id = ? and member_no = ? and left_at is null
                                    limit 1
                                    """,
                            (rs, rowNum) -> rs.getLong(1), tenantId, memberNo.trim())
                            .stream().findFirst().orElse(null);
                }
                if (username == null || username.isBlank()) {
                    return null;
                }
                return jdbcTemplate.query("""
                                select tm.id from tenant_member tm
                                join identity_user iu on iu.id = tm.user_id
                                where tm.tenant_id = ? and iu.username = ? and tm.left_at is null
                                limit 1
                                """,
                        (rs, rowNum) -> rs.getLong(1), tenantId, username.trim())
                        .stream().findFirst().orElse(null);
            };
        }
    }
}
