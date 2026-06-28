package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.common.result.R;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.enums.IdentityUserTargetType;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.core.entity.ExternalIdentityBindingEntity;
import io.mango.identity.core.entity.IdentityUser;
import io.mango.identity.core.entity.TenantMember;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.mapper.ExternalIdentityBindingMapper;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.system.api.SysConfigApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        IdentityUserServiceImplIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity_user_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@DisplayName("身份用户服务集成测试")
class IdentityUserServiceImplIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdentityUserMapper userMapper;

    @Autowired
    private TenantMemberMapper memberMapper;

    @Autowired
    private TenantMemberOrgMapper relationMapper;

    @Autowired
    private ExternalIdentityBindingMapper externalBindingMapper;

    @Autowired
    private TestRoleBindingApi roleBindingApi;

    @Autowired
    private IdentityUserServiceImpl service;

    @BeforeEach
    void setUp() {
        resetSchema();
        roleBindingApi.clear();
    }

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    @DisplayName("查询身份资料时只返回用户资料字段")
    void getUserInfoShouldMapIdentityProfileOnlyThroughRealMapper() {
        seedUser(1L, "admin", "Administrator", "1", 1);

        var profile = service.getUserInfo("admin");

        assertThat(profile.getUserId()).isEqualTo(1L);
        assertThat(profile.getUsername()).isEqualTo("admin");
        assertThat(profile.getNickname()).isEqualTo("Administrator");
        assertThat(profile.getRealm()).isEqualTo("INTERNAL");
        assertThat(profile.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(profile.getPartyType()).isEqualTo("COMPANY");
        assertThat(profile.getPartyId()).isEqualTo(9001L);
        assertThat(profile.getEmail()).isEqualTo("admin@example.com");
        assertThat(profile.getPhone()).isEqualTo("13800138000");
        assertThat(profile.getAvatar()).isEqualTo("https://example.com/avatar.png");
        assertThat(profile.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("账号不存在时返回空")
    void getUserInfoShouldReturnNullWhenNotFoundThroughRealMapper() {
        assertThat(service.getUserInfo("missing")).isNull();
    }

    @Test
    @DisplayName("按部门目标解析当前租户启用用户")
    void listUserInfosByTargetOrgReturnsEnabledUsersThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "admin", "管理员", "1", 1);
        seedUser(1002L, "disabled", "禁用用户", "1", 1);
        seedUser(2001L, "other-tenant", "其它租户", "2", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 0, null);
        seedMember(20L, 2L, 2001L, 1, null);
        seedMemberOrg(100L, 1L, 10L, 200L, null);
        seedMemberOrg(101L, 1L, 11L, 200L, null);
        seedMemberOrg(102L, 2L, 20L, 200L, null);
        IdentityUserTargetQuery query = new IdentityUserTargetQuery();
        query.setTargetType(IdentityUserTargetType.ORG);
        query.setTargetId(200L);
        query.setStatus(1);

        var users = service.listUserInfosByTarget(query);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserId()).isEqualTo(1001L);
        assertThat(users.get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("按角色目标解析当前租户启用用户")
    void listUserInfosByTargetRoleReturnsEnabledUsersThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "admin", "管理员", "1", 1);
        seedUser(1002L, "disabled", "禁用用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 0, null);
        roleBindingApi.subjectIdsByRole = List.of(10L, 11L);
        IdentityUserTargetQuery query = new IdentityUserTargetQuery();
        query.setTargetType(IdentityUserTargetType.ROLE);
        query.setTargetId(300L);
        query.setStatus(1);

        var users = service.listUserInfosByTarget(query);

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserId()).isEqualTo(1001L);
        assertThat(roleBindingApi.lastRoleQuery.getTenantId()).isEqualTo(1L);
        assertThat(roleBindingApi.lastRoleQuery.getSubjectType())
                .isEqualTo(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        assertThat(roleBindingApi.lastRoleQuery.getRoleId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("批量删除成员时清理角色和部门关系")
    void deleteBatchShouldRemoveRoleAndOrgRelationsThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L,
                        "internal-admin"));
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedUser(1002L, "target-1", "目标一", "1", 1);
        seedUser(1003L, "target-2", "目标二", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 1, null);
        seedMember(12L, 1L, 1003L, 1, null);
        seedMemberOrg(101L, 1L, 11L, 200L, null);
        seedMemberOrg(102L, 1L, 12L, 201L, null);

        Integer count = service.deleteBatch(List.of(1001L, 1002L, 1003L));

        assertThat(count).isEqualTo(2);
        assertThat(countMembers()).isEqualTo(1L);
        assertThat(countRelations()).isZero();
        assertThat(roleBindingApi.deleteCommands).hasSize(1);
        DeleteSubjectRoleBindingsCommand command = roleBindingApi.deleteCommands.get(0);
        assertThat(command.getTenantId()).isEqualTo(1L);
        assertThat(command.getSubjectType()).isEqualTo(AuthorizationQuery.SUBJECT_TYPE_TENANT_MEMBER);
        assertThat(command.getSubjectIds()).containsExactlyInAnyOrder(11L, 12L);
    }

    @Test
    @DisplayName("批量删除仅包含当前用户时不执行删除")
    void deleteBatchShouldSkipCurrentUserThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L,
                        "internal-admin"));
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);

        Integer count = service.deleteBatch(List.of(1001L));

        assertThat(count).isZero();
        assertThat(countMembers()).isEqualTo(1L);
        assertThat(roleBindingApi.deleteCommands).isEmpty();
    }

    @Test
    @DisplayName("绑定第三方身份时自动修复同租户缺失的成员关系")
    void bindExternalIdentityShouldRepairMissingTenantMemberForSameTenantThroughRealMappers() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1002L, "wecom_user", "企微用户", "1", 1);
        BindExternalIdentityCommand command = new BindExternalIdentityCommand();
        command.setUserId(1002L);
        command.setProvider("WECOM");
        command.setCorpId("corp");
        command.setExternalUserId("wecom_user");
        command.setDisplayName("企微用户");
        command.setBindSource("SYNC");

        var result = service.bindExternalIdentity(command);

        assertThat(result.getUserId()).isEqualTo(1002L);
        List<TenantMember> members = memberMapper.selectList(null);
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getTenantId()).isEqualTo(1L);
        assertThat(members.get(0).getUserId()).isEqualTo(1002L);
        assertThat(members.get(0).getDisplayName()).isEqualTo("企微用户");
        List<ExternalIdentityBindingEntity> bindings = externalBindingMapper.selectList(null);
        assertThat(bindings).hasSize(1);
        assertThat(bindings.get(0).getProvider()).isEqualTo("WECOM");
        assertThat(bindings.get(0).getCorpId()).isEqualTo("corp");
        assertThat(bindings.get(0).getExternalUserId()).isEqualTo("wecom_user");
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists identity_external_binding");
        jdbcTemplate.execute("drop table if exists tenant_member_org");
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
        jdbcTemplate.execute("""
                create table tenant_member_org (
                    id bigint primary key,
                    tenant_id bigint not null,
                    member_id bigint not null,
                    org_id bigint,
                    post_id bigint,
                    primary_flag tinyint,
                    leader_flag tinyint,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("""
                create table identity_external_binding (
                    id bigint primary key,
                    tenant_id bigint not null,
                    user_id bigint not null,
                    provider varchar(32) not null,
                    corp_id varchar(128) not null,
                    external_user_id varchar(128) not null,
                    display_name varchar(100),
                    bind_source varchar(32),
                    bind_status varchar(32),
                    bind_time timestamp,
                    last_login_time timestamp,
                    created_by bigint,
                    created_at timestamp,
                    updated_by bigint,
                    updated_at timestamp
                )
                """);
    }

    private void seedUser(Long userId, String username, String nickname, String tenantId, Integer status) {
        jdbcTemplate.update("""
                        insert into identity_user
                        (id, username, password, password_reset_required, nickname, realm, actor_type, party_type,
                         party_id, email, phone, avatar, status, tenant_id, failed_login_count, create_time, update_time)
                        values (?, ?, 'encoded', false, ?, 'INTERNAL', 'INTERNAL_USER', 'COMPANY',
                                9001, 'admin@example.com', '13800138000', 'https://example.com/avatar.png',
                                ?, ?, 0, current_timestamp, current_timestamp)
                        """,
                userId, username, nickname, status, tenantId);
    }

    private void seedMember(Long memberId, Long tenantId, Long userId, Integer status, LocalDateTime leftAt) {
        jdbcTemplate.update("""
                        insert into tenant_member
                        (id, tenant_id, user_id, member_no, display_name, member_type, status, joined_at, left_at)
                        values (?, ?, ?, ?, ?, 'EMPLOYEE', ?, current_timestamp, ?)
                        """,
                memberId, tenantId, userId, "USER-" + userId, "member-" + userId, status, leftAt);
    }

    private void seedMemberOrg(Long id, Long tenantId, Long memberId, Long orgId, Long postId) {
        jdbcTemplate.update("""
                        insert into tenant_member_org
                        (id, tenant_id, member_id, org_id, post_id, primary_flag, leader_flag)
                        values (?, ?, ?, ?, ?, 0, 0)
                        """,
                id, tenantId, memberId, orgId, postId);
    }

    private Long countMembers() {
        return jdbcTemplate.queryForObject("select count(*) from tenant_member", Long.class);
    }

    private Long countRelations() {
        return jdbcTemplate.queryForObject("select count(*) from tenant_member_org", Long.class);
    }

    @Configuration
    @MapperScan(basePackageClasses = IdentityUserMapper.class)
    @Import({
            IdentityUserServiceImpl.class,
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
        ApplicationEventPublisher applicationEventPublisher() {
            return event -> {
            };
        }

        @Bean
        TestRoleBindingApi roleBindingApi() {
            return new TestRoleBindingApi();
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

    static class TestRoleBindingApi implements RoleBindingApi {

        private List<Long> subjectIdsByRole = List.of();

        private final List<DeleteSubjectRoleBindingsCommand> deleteCommands = new ArrayList<>();

        private SubjectRoleBindingQuery lastRoleQuery;

        @Override
        public R<Long> findRoleId(RoleLookupQuery query) {
            return R.ok(null);
        }

        @Override
        public R<Boolean> ensureSubjectRoleBinding(SubjectRoleBindingCommand command) {
            return R.ok(false);
        }

        @Override
        public R<Integer> deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command) {
            deleteCommands.add(command);
            return R.ok(command.getSubjectIds() == null ? 0 : command.getSubjectIds().size());
        }

        @Override
        public R<List<Long>> listSubjectIdsByRole(SubjectRoleBindingQuery query) {
            this.lastRoleQuery = query;
            return R.ok(subjectIdsByRole);
        }

        void clear() {
            subjectIdsByRole = List.of();
            deleteCommands.clear();
            lastRoleQuery = null;
        }
    }
}
