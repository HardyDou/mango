package io.mango.identity.core.service.impl;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import io.mango.authorization.api.AuthorizationQuery;
import io.mango.authorization.api.RoleBindingApi;
import io.mango.authorization.api.command.DeleteSubjectRoleBindingsCommand;
import io.mango.authorization.api.command.SubjectRoleBindingCommand;
import io.mango.authorization.api.query.RoleLookupQuery;
import io.mango.authorization.api.query.SubjectRoleBindingQuery;
import io.mango.common.result.R;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaSendRequest;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.identity.api.command.BindExternalIdentityCommand;
import io.mango.identity.api.command.BatchDeleteIdentityUserCommand;
import io.mango.identity.api.command.CreateTenantMemberInOrgCommand;
import io.mango.identity.api.command.RestoreTenantMemberInOrgCommand;
import io.mango.identity.api.command.SendContactCaptchaCommand;
import io.mango.identity.api.command.UnbindCurrentExternalIdentityCommand;
import io.mango.identity.api.command.UpdateCurrentUserContactCommand;
import io.mango.identity.api.command.UpdateCurrentUserProfileCommand;
import io.mango.identity.api.enums.IdentityUserTargetType;
import io.mango.identity.api.query.ExternalIdentityQuery;
import io.mango.identity.api.query.IdentityUserPageQuery;
import io.mango.identity.api.query.IdentityAccountAvailabilityQuery;
import io.mango.identity.api.query.IdentityUserTargetQuery;
import io.mango.identity.api.request.IdentityUserBatchRequest;
import io.mango.identity.api.vo.IdentityUserInfoVO;
import io.mango.identity.core.entity.ExternalIdentityBindingEntity;
import io.mango.identity.core.entity.IdentityUserEntity;
import io.mango.identity.core.entity.TenantMemberEntity;
import io.mango.identity.core.entity.TenantMemberOrgEntity;
import io.mango.identity.core.adapter.AuthorizationRoleBindingAdapter;
import io.mango.identity.core.adapter.SysConfigValueAdapter;
import io.mango.identity.core.mapper.ExternalIdentityBindingMapper;
import io.mango.identity.core.mapper.IdentityUserMapper;
import io.mango.identity.core.mapper.TenantMemberMapper;
import io.mango.identity.core.mapper.TenantMemberOrgMapper;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.persistence.starter.PersistenceMybatisPlusAutoConfiguration;
import io.mango.notice.api.enums.NoticeSiteMessageActionInteractionType;
import io.mango.notice.api.enums.NoticeSiteMessageTargetType;
import io.mango.notice.api.command.NoticeSendEventCommand;
import io.mango.system.api.tenant.TenantProvisionCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        DataSourceAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        PersistenceMybatisPlusAutoConfiguration.class,
        IdentityUserServiceIntegrationTest.TestConfig.class
})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:identity_user_service;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "mango.persistence.mybatis-plus.tenant.enabled=false"
})
@RecordApplicationEvents
@DisplayName("身份用户服务集成测试")
class IdentityUserServiceIntegrationTest {

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
    private IdentityUserService service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CaptchaApi captchaApi;

    @Autowired
    private IdentityTenantProvisioner tenantProvisioner;

    @Autowired
    private LocalTenantMemberProvider tenantMemberProvider;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void setUp() {
        resetSchema();
        roleBindingApi.clear();
        reset(captchaApi);
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
    @DisplayName("批量查询只返回当前租户成员并去重")
    void listUserInfosShouldResolveMixedKeysWithinCurrentTenant() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "admin", "管理员", "1", 1);
        seedUser(1002L, "reviewer", "复核人", "1", 0);
        seedUser(2001L, "other", "其它租户", "2", 1);
        seedMember(11L, 1L, 1001L, 1, null);
        seedMember(12L, 1L, 1002L, 0, null);
        seedMember(21L, 2L, 2001L, 1, null);
        IdentityUserBatchRequest query = new IdentityUserBatchRequest();
        query.setUserIds(List.of(1001L, 1001L, 2001L, 9999L));
        query.setUsernames(List.of("admin", "reviewer", "other", "missing", " admin "));

        var users = service.listUserInfos(query);

        assertThat(users).extracting(IdentityUserInfoVO::getUserId)
                .containsExactlyInAnyOrder(1001L, 1002L);
        assertThat(users).extracting(IdentityUserInfoVO::getUsername)
                .containsExactlyInAnyOrder("admin", "reviewer");
    }

    @Test
    @DisplayName("批量查询为空时返回空结果")
    void listUserInfosShouldReturnEmptyForEmptyQuery() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        IdentityUserBatchRequest query = new IdentityUserBatchRequest();

        assertThat(service.listUserInfos(query)).isEmpty();
    }

    @Test
    @DisplayName("批量查询排除已退出当前租户的成员")
    void listUserInfosShouldExcludeMembersWhoLeftCurrentTenant() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "former", "已离开", "1", 1);
        seedMember(11L, 1L, 1001L, 1, LocalDateTime.now());
        IdentityUserBatchRequest query = new IdentityUserBatchRequest();
        query.setUsernames(List.of("former"));

        assertThat(service.listUserInfos(query)).isEmpty();
    }

    @Test
    @DisplayName("批量查询总标识数不能超过上限")
    void listUserInfosShouldRejectMoreThanTwoHundredDistinctKeys() {
        IdentityUserBatchRequest query = new IdentityUserBatchRequest();
        query.setUserIds(java.util.stream.LongStream.rangeClosed(1, 201).boxed().toList());

        assertThatThrownBy(() -> service.listUserInfos(query))
                .isInstanceOf(io.mango.common.exception.BizException.class);
    }

    @Test
    @DisplayName("初始化机构管理员时使用完整角色查询契约")
    void tenantProvisionUsesCompleteRoleLookupContract() {
        seedUser(1L, "admin", "Administrator", "1", 1);
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));

        tenantProvisioner.provision(new TenantProvisionCommand(2L, "company_a", "A公司"));

        assertThat(roleBindingApi.lastLookupQuery).isNotNull();
        assertThat(roleBindingApi.lastLookupQuery.getTenantId()).isEqualTo(2L);
        assertThat(roleBindingApi.lastLookupQuery.getAppCode()).isEqualTo("internal-admin");
        assertThat(roleBindingApi.lastLookupQuery.getRealm()).isEqualTo("INTERNAL");
        assertThat(roleBindingApi.lastLookupQuery.getActorType()).isEqualTo("INTERNAL_USER");
        assertThat(roleBindingApi.lastLookupQuery.getRoleCode()).isEqualTo("ROLE_ADMIN");
        assertThat(roleBindingApi.lastBindingCommand).isNotNull();
        assertThat(roleBindingApi.lastBindingCommand.getTenantId()).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member where tenant_id = 2 and user_id = 1",
                Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where tenant_id = 2 and user_id = 1 "
                        + "and event_type = 'CREATED'",
                Long.class)).isEqualTo(1L);

        tenantProvisioner.provision(new TenantProvisionCommand(2L, "company_a", "A公司"));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where tenant_id = 2 and user_id = 1 "
                        + "and event_type = 'CREATED'",
                Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("启动对账时为已有机构管理员恢复管理员角色")
    void tenantProvisionBindsExistingInstitutionAdminWithoutSecurityContext() {
        seedUser(1L, "admin", "Administrator", "1", 1);
        jdbcTemplate.update("""
                        insert into tenant_member
                        (id, tenant_id, user_id, member_no, display_name, member_type, status, joined_at)
                        values (1001, 1, 1, 'ADMIN-default', 'Administrator', 'INSTITUTION_ADMIN', 1, current_timestamp)
                        """);
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));

        tenantProvisioner.provision(new TenantProvisionCommand(1L, "default", "芒果集团"));

        assertThat(roleBindingApi.lastLookupQuery).isNotNull();
        assertThat(roleBindingApi.lastLookupQuery.getRoleCode()).isEqualTo("ROLE_ADMIN");
        assertThat(roleBindingApi.lastBindingCommand).isNotNull();
        assertThat(roleBindingApi.lastBindingCommand.getSubjectId()).isEqualTo(1001L);
        assertThat(roleBindingApi.lastBindingCommand.getRoleId()).isEqualTo(88L);
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
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
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
        assertThat(roleBindingApi.lastRoleQuery.getAppCode()).isEqualTo("internal-admin");
        assertThat(roleBindingApi.lastRoleQuery.getRealm()).isEqualTo("INTERNAL");
    }

    @Test
    @DisplayName("批量移出成员时保留主体并清理角色和部门关系")
    void deleteBatchShouldRetainMemberAndRemoveAccessThroughRealMappers() {
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

        Integer count = service.deleteBatch(deleteCommand(1001L, 1002L, 1003L));

        assertThat(count).isEqualTo(2);
        assertThat(countMembers()).isEqualTo(3L);
        assertThat(countRelations()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member where status = 0 and left_at is not null", Long.class))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where event_type = 'REMOVED'", Long.class))
                .isEqualTo(2L);
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

        Integer count = service.deleteBatch(deleteCommand(1001L));

        assertThat(count).isZero();
        assertThat(countMembers()).isEqualTo(1L);
        assertThat(roleBindingApi.deleteCommands).isEmpty();
    }

    @Test
    @DisplayName("角色撤销失败时不得移出租户成员")
    void deleteBatchShouldFailClosedWhenRoleRevocationFails() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L,
                        "internal-admin"));
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedUser(1002L, "target", "目标用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 1, null);
        seedMemberOrg(101L, 1L, 11L, 200L, null);
        roleBindingApi.deleteFailure = true;

        assertThatThrownBy(() -> service.deleteBatch(deleteCommand(1002L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("role revocation failed");

        assertThat(memberMapper.selectById(11L).getLeftAt()).isNull();
        assertThat(memberMapper.selectById(11L).getStatus()).isEqualTo(1);
        assertThat(relationMapper.selectById(101L)).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where event_type = 'REMOVED'", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("账号可用性只为本特性移出的当前租户成员返回脱敏候选")
    void accountAvailabilityShouldExposeOnlyCurrentTenantRecoverableMember() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1002L, "former", "原成员", "1", 1);
        seedMember(11L, 1L, 1002L, 0, LocalDateTime.now());
        seedLifecycle(1L, 1002L, 11L, "REMOVED");
        IdentityAccountAvailabilityQuery query = new IdentityAccountAvailabilityQuery();
        query.setUsername("former");

        var result = service.accountAvailability(query);

        assertThat(result.getStatus()).isEqualTo("RECOVERABLE");
        assertThat(result.getDisplayName()).isEqualTo("member-1002");
        assertThat(result.getMaskedPhone()).isEqualTo("138****8000");
        assertThat(result.getMaskedEmail()).isEqualTo("a***@example.com");
        assertThat(result.getMemberNo()).isEqualTo("USER-1002");
        assertThat(result.getRemovedAt()).isNotNull();

        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("2"));
        var otherTenant = service.accountAvailability(query);
        assertThat(otherTenant.getStatus()).isEqualTo("UNAVAILABLE");
        assertThat(otherTenant.getDisplayName()).isNull();
        assertThat(otherTenant.getMaskedPhone()).isNull();
    }

    @Test
    @DisplayName("没有本特性移出事件的旧成员不可恢复")
    void accountAvailabilityShouldNotRecoverHistoricalRows() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1002L, "legacy", "旧成员", "1", 1);
        seedMember(11L, 1L, 1002L, 0, LocalDateTime.now().minusDays(1));
        IdentityAccountAvailabilityQuery query = new IdentityAccountAvailabilityQuery();
        query.setUsername("legacy");

        assertThat(service.accountAvailability(query).getStatus()).isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("成员状态与移出时间不一致时不可恢复")
    void recoverMemberShouldRequireDisabledMemberStatus() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withSecurity(
                1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L, "internal-admin"));
        seedUser(1001L, "admin", "管理员", "1", 1);
        seedUser(1002L, "inconsistent", "状态异常成员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 1, LocalDateTime.now());
        seedLifecycle(1L, 1002L, 11L, "REMOVED");
        IdentityAccountAvailabilityQuery query = new IdentityAccountAvailabilityQuery();
        query.setUsername("inconsistent");

        assertThat(service.accountAvailability(query).getStatus()).isEqualTo("UNAVAILABLE");

        RestoreTenantMemberInOrgCommand command = new RestoreTenantMemberInOrgCommand();
        command.setTenantId(1L);
        command.setOrgId(300L);
        command.setUsername("inconsistent");
        command.setOperatorUserId(1001L);

        assertThatThrownBy(() -> tenantMemberProvider.restoreMemberInOrg(command))
                .isInstanceOf(io.mango.common.exception.BizException.class)
                .hasMessage("原成员当前不可恢复");
        assertThat(relationMapper.selectList(null)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where event_type = 'RESTORED'", Long.class))
                .isZero();
    }

    @Test
    @DisplayName("恢复原成员复用身份标识且只建立本次部门关系")
    void restoreMemberShouldReuseIdsAndCreateOnlySelectedOrgRelation() {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(1001L, "1", "admin", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L,
                        "internal-admin"));
        seedUser(1001L, "admin", "管理员", "1", 1);
        seedUser(1002L, "former", "原成员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 0, LocalDateTime.now());
        seedLifecycle(1L, 1002L, 11L, "REMOVED");
        RestoreTenantMemberInOrgCommand command = new RestoreTenantMemberInOrgCommand();
        command.setTenantId(1L);
        command.setOrgId(300L);
        command.setPostId(400L);
        command.setUsername("former");
        command.setRealm("INTERNAL");
        command.setOperatorUserId(1001L);

        Long userId = tenantMemberProvider.restoreMemberInOrg(command);

        assertThat(userId).isEqualTo(1002L);
        TenantMemberEntity member = memberMapper.selectById(11L);
        assertThat(member.getUserId()).isEqualTo(1002L);
        assertThat(member.getStatus()).isEqualTo(1);
        assertThat(member.getLeftAt()).isNull();
        assertThat(member.getPrimaryOrgId()).isEqualTo(300L);
        assertThat(relationMapper.selectList(null)).singleElement().satisfies(relation -> {
            assertThat(relation.getMemberId()).isEqualTo(11L);
            assertThat(relation.getOrgId()).isEqualTo(300L);
            assertThat(relation.getPostId()).isEqualTo(400L);
            assertThat(relation.getPrimaryFlag()).isEqualTo(1);
        });
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from tenant_member_lifecycle_log where member_id = 11 and event_type = 'RESTORED'",
                Long.class)).isEqualTo(1L);
        assertThat(roleBindingApi.deleteCommands).isEmpty();
    }

    @Test
    @DisplayName("绑定第三方身份时要求目标用户已经是当前租户成员")
    void bindExternalIdentityShouldRequireExistingTenantMember() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1002L, "wecom_user", "企微用户", "1", 1);
        BindExternalIdentityCommand command = new BindExternalIdentityCommand();
        command.setUserId(1002L);
        command.setProvider("WECOM");
        command.setCorpId("corp");
        command.setExternalUserId("wecom_user");
        command.setDisplayName("企微用户");
        command.setBindSource("SYNC");

        assertThatThrownBy(() -> service.bindExternalIdentity(command))
                .isInstanceOf(io.mango.common.exception.BizException.class);
        assertThat(memberMapper.selectList(null)).isEmpty();
        assertThat(externalBindingMapper.selectList(null)).isEmpty();
    }

    @Test
    @DisplayName("第三方未返回显示名时不得回退 Mango 昵称")
    void bindExternalIdentityShouldKeepMissingProviderDisplayNameEmpty() {
        setCurrentUser(1001L);
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        BindExternalIdentityCommand command = bindingCommand(1001L);
        command.setExternalUserId("wecom-user-4826");
        command.setDisplayName(null);

        var binding = service.bindExternalIdentity(command);
        var currentBindings = service.listCurrentExternalIdentities();

        assertThat(binding.getDisplayName()).isNull();
        assertThat(currentBindings).singleElement().satisfies(current -> {
            assertThat(current.getDisplayName()).isNull();
            assertThat(current.getExternalUserId()).isEqualTo("****4826");
        });
    }

    @Test
    @DisplayName("当前用户外部身份返回完整第三方昵称和头像文件")
    void listCurrentExternalIdentitiesShouldReturnProviderProfile() {
        setCurrentUser(1001L);
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        BindExternalIdentityCommand command = bindingCommand(1001L);
        command.setExternalUserId("wecom-user-4826");
        command.setDisplayName("企业微信张三");
        command.setAvatarFileId(7001L);

        var binding = service.bindExternalIdentity(command);
        var currentBindings = service.listCurrentExternalIdentities();

        assertThat(binding.getDisplayName()).isEqualTo("企业微信张三");
        assertThat(binding.getAvatarFileId()).isEqualTo(7001L);
        assertThat(currentBindings).singleElement().satisfies(current -> {
            assertThat(current.getDisplayName()).isEqualTo("企业微信张三");
            assertThat(current.getAvatarFileId()).isEqualTo(7001L);
            assertThat(current.getExternalUserId()).isEqualTo("****4826");
        });
    }

    @Test
    @DisplayName("仅显式头像快照允许清空已有第三方头像")
    void bindExternalIdentityShouldOnlyClearAvatarForExplicitSnapshot() {
        setCurrentUser(1001L);
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        BindExternalIdentityCommand initial = bindingCommand(1001L);
        initial.setAvatarFileId(7001L);
        service.bindExternalIdentity(initial);

        var preserved = service.bindExternalIdentity(bindingCommand(1001L));
        BindExternalIdentityCommand clearedSnapshot = bindingCommand(1001L);
        clearedSnapshot.setReplaceAvatarFile(true);
        var cleared = service.bindExternalIdentity(clearedSnapshot);
        var persisted = service.listCurrentExternalIdentities();

        assertThat(preserved.getAvatarFileId()).isEqualTo(7001L);
        assertThat(cleared.getAvatarFileId()).isNull();
        assertThat(persisted).singleElement().satisfies(binding ->
                assertThat(binding.getAvatarFileId()).isNull());
    }

    @Test
    @DisplayName("企业微信资料缺少昵称时清空已持久化的旧昵称")
    void bindExternalIdentity_existingDisplayNameMissing_persistsEmpty() {
        setCurrentUser(1001L);
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        service.bindExternalIdentity(bindingCommand(1001L));

        BindExternalIdentityCommand missingDisplayName = bindingCommand(1001L);
        missingDisplayName.setDisplayName(null);
        var cleared = service.bindExternalIdentity(missingDisplayName);
        var persisted = service.listCurrentExternalIdentities();

        assertThat(cleared.getDisplayName()).isNull();
        assertThat(persisted).singleElement().satisfies(binding ->
                assertThat(binding.getDisplayName()).isNull());
    }

    @Test
    @DisplayName("第三方显示名与 Mango 昵称不同时保留第三方显示名")
    void bindExternalIdentityShouldPreserveProviderDisplayName() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        BindExternalIdentityCommand command = bindingCommand(1001L);
        command.setProvider("DINGTALK");
        command.setExternalUserId("dingtalk-union-id");
        command.setDisplayName("钉钉张三");

        var binding = service.bindExternalIdentity(command);

        assertThat(binding.getDisplayName()).isEqualTo("钉钉张三");
    }

    @Test
    @DisplayName("查询外部身份时只返回有效绑定")
    void findExternalIdentityShouldIgnoreInactiveBinding() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "Administrator", "Mango 管理员", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        var binding = service.bindExternalIdentity(bindingCommand(1001L));
        ExternalIdentityBindingEntity inactive = externalBindingMapper.selectById(binding.getId());
        inactive.setBindStatus("UNBOUND");
        externalBindingMapper.updateById(inactive);
        ExternalIdentityQuery query = new ExternalIdentityQuery();
        query.setUserId(1001L);
        query.setProvider("WECOM");
        query.setCorpId("corp-id");

        assertThat(service.findExternalIdentity(query)).isNull();
    }

    @Test
    @DisplayName("当前用户实名资料默认未认证且证件号码脱敏")
    void currentProfileShouldDefaultToUnverifiedAndMaskDocumentNumber() {
        setCurrentUser(1001L);
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        jdbcTemplate.update("""
                update identity_user
                   set real_name = '测试用户', document_type = 'ID_CARD', document_number = '110101199001011234'
                 where id = 1001
                """);

        var profile = service.currentProfile();

        assertThat(profile.getRealName()).isEqualTo("测试用户");
        assertThat(profile.getDocumentType()).isEqualTo("ID_CARD");
        assertThat(profile.getDocumentNumber()).isEqualTo("****1234");
        assertThat(profile.getVerificationStatus()).isEqualTo("UNVERIFIED");
        assertThat(profile.getVerificationSource()).isNull();
    }

    @Test
    @DisplayName("分页查询支持多个组织范围并回显命中的组织关系")
    void pageResultShouldFilterByAnyRequestedOrganization() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "root-user", "集团成员", "1", 1);
        seedUser(1002L, "child-user", "部门成员", "1", 1);
        seedUser(1003L, "outside-user", "范围外成员", "1", 1);
        seedMember(11L, 1L, 1001L, 1, null);
        seedMember(12L, 1L, 1002L, 1, null);
        seedMember(13L, 1L, 1003L, 1, null);
        seedMemberOrg(101L, 1L, 11L, 200L, null);
        seedMemberOrg(102L, 1L, 12L, 201L, null);
        seedMemberOrg(103L, 1L, 13L, 300L, null);
        IdentityUserPageQuery query = new IdentityUserPageQuery();
        query.setPage(1L);
        query.setSize(20L);
        query.setOrgIds(List.of(200L, 201L));

        var result = service.pageResult(query);

        assertThat(result.getList()).extracting(item -> item.getUserId())
                .containsExactlyInAnyOrder(1001L, 1002L);
        assertThat(result.getList()).extracting(item -> item.getOrgId())
                .containsExactlyInAnyOrder(200L, 201L);
    }

    @Test
    @DisplayName("候选成员关键字按用户名姓名手机邮箱任一字段匹配")
    void pageResultKeywordShouldMatchAnySupportedIdentityField() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "keyword-login", "候选成员", "1", 1);
        seedMember(11L, 1L, 1001L, 1, null);
        jdbcTemplate.update("update identity_user set phone = '13912345678', email = 'candidate@example.com' where id = 1001");

        for (String keyword : List.of("keyword-login", "候选成员", "1391234", "candidate@")) {
            IdentityUserPageQuery query = new IdentityUserPageQuery();
            query.setPage(1L);
            query.setSize(20L);
            query.setKeyword(keyword);

            assertThat(service.pageResult(query).getList()).singleElement()
                    .satisfies(item -> assertThat(item.getUserId()).isEqualTo(1001L));
        }
    }

    @Test
    @DisplayName("候选成员分页在数据库查询阶段排除目标组织已有成员")
    void pageResultShouldExcludeMembersAlreadyInTargetOrganization() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "already-added", "已有成员", "1", 1);
        seedUser(1002L, "available", "可选成员", "1", 1);
        seedMember(11L, 1L, 1001L, 1, null);
        seedMember(12L, 1L, 1002L, 1, null);
        seedMemberOrg(101L, 1L, 11L, 200L, null);
        IdentityUserPageQuery query = new IdentityUserPageQuery();
        query.setPage(1L);
        query.setSize(20L);
        query.setExcludeOrgId(200L);

        var result = service.pageResult(query);

        assertThat(result.getList()).singleElement()
                .satisfies(item -> assertThat(item.getUserId()).isEqualTo(1002L));
        assertThat(result.getTotal()).isEqualTo(1L);
    }

    @Test
    @DisplayName("组织内开户在同一事务写入用户成员和组织关系")
    void createMemberInOrgShouldPersistAllThreeIdentityFactsAtomically() {
        setCurrentUser(9001L);
        CreateTenantMemberInOrgCommand command = createMemberInOrgCommand("new-department-user", 200L);
        command.setPostId(300L);
        command.setLeaderFlag(true);

        Long userId = tenantMemberProvider.createMemberInOrg(command);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from identity_user where id = ? and tenant_id = '1'", Long.class, userId))
                .isEqualTo(1L);
        Long memberId = jdbcTemplate.queryForObject(
                "select id from tenant_member where tenant_id = 1 and user_id = ?", Long.class, userId);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from tenant_member_org
                 where tenant_id = 1 and member_id = ? and org_id = 200 and post_id = 300
                   and primary_flag = 1 and leader_flag = 1 and created_by = 9001
                """, Long.class, memberId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("组织关系写入失败时回滚用户和成员")
    void createMemberInOrgShouldRollbackUserAndMemberWhenRelationFails() {
        setCurrentUser(9001L);
        CreateTenantMemberInOrgCommand command = createMemberInOrgCommand("rollback-user", null);

        assertThatThrownBy(() -> tenantMemberProvider.createMemberInOrg(command))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(jdbcTemplate.queryForObject("select count(*) from identity_user", Long.class)).isZero();
        assertThat(countMembers()).isZero();
        assertThat(countRelations()).isZero();
    }

    @Test
    @DisplayName("脱敏证件号留空更新时保留原号码")
    void updateCurrentProfileShouldPreserveExistingDocumentNumberWhenBlank() {
        setCurrentUser(1001L);
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        jdbcTemplate.update("""
                update identity_user
                   set document_type = 'ID_CARD', document_number = '110101199001011234'
                 where id = 1001
                """);
        UpdateCurrentUserProfileCommand command = new UpdateCurrentUserProfileCommand();
        command.setNickname("新昵称");
        command.setDocumentType("ID_CARD");

        var profile = service.updateCurrentProfile(command);

        assertThat(profile.getDocumentNumber()).isEqualTo("****1234");
        assertThat(jdbcTemplate.queryForObject(
                "select document_number from identity_user where id = 1001", String.class))
                .isEqualTo("110101199001011234");
    }

    @Test
    @DisplayName("当前用户清空实名和证件信息时应持久化空值")
    void updateCurrentProfileShouldPersistClearedIdentityFields() {
        setCurrentUser(1001L);
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        jdbcTemplate.update("""
                update identity_user
                   set real_name = '测试用户', document_type = 'ID_CARD', document_number = '110101199001011234'
                 where id = 1001
                """);

        var profile = service.updateCurrentProfile(new UpdateCurrentUserProfileCommand());

        assertThat(profile.getRealName()).isNull();
        assertThat(profile.getDocumentType()).isNull();
        assertThat(profile.getDocumentNumber()).isNull();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from identity_user
                 where id = 1001
                   and real_name is null
                   and document_type is null
                   and document_number is null
                """, Long.class)).isEqualTo(1L);
    }

    @Test
    @DisplayName("修改联系方式同时要求当前密码和新值验证码")
    void updateCurrentContactShouldRequirePasswordAndMatchingCaptcha() {
        setCurrentUser(1001L);
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        jdbcTemplate.update("update identity_user set password = ? where id = 1001",
                passwordEncoder.encode("current-password"));
        when(captchaApi.send(any(CaptchaSendRequest.class)))
                .thenReturn(R.ok("captcha:CHANGE_EMAIL:new@example.com"));
        when(captchaApi.verify(any(CaptchaVerifyRequest.class))).thenReturn(R.ok(Boolean.TRUE));
        SendContactCaptchaCommand send = new SendContactCaptchaCommand();
        send.setContactType("EMAIL");
        send.setTarget("new@example.com");

        var ticket = service.sendCurrentContactCaptcha(send);
        UpdateCurrentUserContactCommand wrongPassword = contactCommand(ticket.getKey(), "wrong-password");
        assertThatThrownBy(() -> service.updateCurrentContact(wrongPassword))
                .isInstanceOf(io.mango.common.exception.BizException.class);

        var profile = service.updateCurrentContact(contactCommand(ticket.getKey(), "current-password"));

        assertThat(ticket.getKey()).isEqualTo("CHANGE_EMAIL:new@example.com");
        assertThat(profile.getEmail()).isEqualTo("n***@example.com");
        assertThat(jdbcTemplate.queryForObject("select email from identity_user where id = 1001", String.class))
                .isEqualTo("new@example.com");
        verify(captchaApi).verify(any(CaptchaVerifyRequest.class));
    }

    @Test
    @DisplayName("同一外部身份不能被另一个成员抢占")
    void externalIdentityCannotBeClaimedByAnotherUser() {
        MangoContextHolder.set(MangoContextSnapshot.empty().withTenantId("1"));
        seedUser(1001L, "first", "用户一", "1", 1);
        seedUser(1002L, "second", "用户二", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        seedMember(11L, 1L, 1002L, 1, null);

        service.bindExternalIdentity(bindingCommand(1001L));

        assertThatThrownBy(() -> service.bindExternalIdentity(bindingCommand(1002L)))
                .isInstanceOf(io.mango.common.exception.BizException.class);
        assertThat(externalBindingMapper.selectList(null)).singleElement()
                .extracting(ExternalIdentityBindingEntity::getUserId)
                .isEqualTo(1001L);
    }

    @Test
    @DisplayName("当前用户只能使用密码解绑自己的第三方身份")
    void currentUserUnbindShouldRequirePasswordAndOwnership() {
        setCurrentUser(1001L);
        seedUser(1001L, "current", "当前用户", "1", 1);
        seedMember(10L, 1L, 1001L, 1, null);
        jdbcTemplate.update("update identity_user set password = ? where id = 1001",
                passwordEncoder.encode("current-password"));
        var binding = service.bindExternalIdentity(bindingCommand(1001L));
        UnbindCurrentExternalIdentityCommand command = new UnbindCurrentExternalIdentityCommand();
        command.setBindingId(binding.getId());
        command.setCurrentPassword("wrong-password");

        assertThatThrownBy(() -> service.unbindCurrentExternalIdentity(command))
                .isInstanceOf(io.mango.common.exception.BizException.class);
        assertThat(externalBindingMapper.selectById(binding.getId())).isNotNull();

        command.setCurrentPassword("current-password");
        assertThat(service.unbindCurrentExternalIdentity(command)).isTrue();
        assertThat(externalBindingMapper.selectById(binding.getId())).isNull();
    }

    private void resetSchema() {
        jdbcTemplate.execute("drop table if exists identity_external_binding");
        jdbcTemplate.execute("drop table if exists tenant_member_lifecycle_log");
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
                create table tenant_member_org (
                    id bigint primary key,
                    tenant_id bigint not null,
                    member_id bigint not null,
                    org_id bigint not null,
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
        jdbcTemplate.execute("""
                create table identity_external_binding (
                    id bigint primary key,
                    tenant_id bigint not null,
                    app_code varchar(64) not null default 'internal-admin',
                    user_id bigint not null,
                    provider varchar(32) not null,
                    corp_id varchar(128) not null,
                    external_user_id varchar(128) not null,
                    display_name varchar(100),
                    avatar_file_id bigint,
                    bind_source varchar(32),
                    bind_status varchar(32),
                    bind_time timestamp,
                    last_login_time timestamp,
                    org_id bigint,
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

    private void seedLifecycle(Long tenantId, Long userId, Long memberId, String eventType) {
        jdbcTemplate.update("""
                        insert into tenant_member_lifecycle_log
                        (id, tenant_id, user_id, member_id, event_type, operator_user_id, occurred_at)
                        values (?, ?, ?, ?, ?, 1001, current_timestamp)
                        """,
                9000L + memberId, tenantId, userId, memberId, eventType);
    }

    private Long countMembers() {
        return jdbcTemplate.queryForObject("select count(*) from tenant_member", Long.class);
    }

    private Long countRelations() {
        return jdbcTemplate.queryForObject("select count(*) from tenant_member_org", Long.class);
    }

    private BatchDeleteIdentityUserCommand deleteCommand(Long... userIds) {
        BatchDeleteIdentityUserCommand command = new BatchDeleteIdentityUserCommand();
        command.setUserIds(List.of(userIds));
        return command;
    }

    private CreateTenantMemberInOrgCommand createMemberInOrgCommand(String username, Long orgId) {
        CreateTenantMemberInOrgCommand command = new CreateTenantMemberInOrgCommand();
        command.setTenantId(1L);
        command.setOrgId(orgId);
        command.setUsername(username);
        command.setPassword("Mango@123456");
        command.setNickname("新成员");
        command.setEmail(username + "@example.com");
        command.setPhone("13900000000");
        command.setStatus(1);
        command.setPrimaryFlag(true);
        command.setLeaderFlag(false);
        command.setOperatorUserId(9001L);
        return command;
    }

    private void setCurrentUser(Long userId) {
        MangoContextHolder.set(MangoContextSnapshot.empty()
                .withSecurity(userId, "1", "current", "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 1L,
                        "internal-admin"));
    }

    private UpdateCurrentUserContactCommand contactCommand(String captchaKey, String password) {
        UpdateCurrentUserContactCommand command = new UpdateCurrentUserContactCommand();
        command.setContactType("EMAIL");
        command.setTarget("new@example.com");
        command.setCurrentPassword(password);
        command.setCaptchaKey(captchaKey);
        command.setCaptchaCode("123456");
        return command;
    }

    private BindExternalIdentityCommand bindingCommand(Long userId) {
        BindExternalIdentityCommand command = new BindExternalIdentityCommand();
        command.setUserId(userId);
        command.setAppCode("internal-admin");
        command.setProvider("WECOM");
        command.setCorpId("corp-id");
        command.setExternalUserId("external-user");
        command.setDisplayName("第三方用户");
        command.setBindSource("SELF");
        return command;
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    @MapperScan(basePackageClasses = IdentityUserMapper.class)
    @Import({
            IdentityUserService.class,
            IdentityUserSecurityService.class,
            IdentitySecurityPolicyService.class,
            IdentityPasswordPolicyService.class,
            IdentityTenantProvisioner.class,
            LocalTenantMemberProvider.class,
            AuthorizationRoleBindingAdapter.class,
            SysConfigValueAdapter.class
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
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        CaptchaApi captchaApi() {
            return mock(CaptchaApi.class);
        }

        @Bean
        TestRoleBindingApi roleBindingApi() {
            return new TestRoleBindingApi();
        }

    }

    static class TestRoleBindingApi implements RoleBindingApi {

        private List<Long> subjectIdsByRole = List.of();

        private final List<DeleteSubjectRoleBindingsCommand> deleteCommands = new ArrayList<>();

        private SubjectRoleBindingQuery lastRoleQuery;

        private RoleLookupQuery lastLookupQuery;

        private SubjectRoleBindingCommand lastBindingCommand;

        private boolean deleteFailure;

        @Override
        public R<Long> findRoleId(RoleLookupQuery query) {
            lastLookupQuery = query;
            return R.ok(88L);
        }

        @Override
        public R<Boolean> ensureSubjectRoleBinding(SubjectRoleBindingCommand command) {
            lastBindingCommand = command;
            return R.ok(true);
        }

        @Override
        public R<Integer> deleteSubjectRoleBindings(DeleteSubjectRoleBindingsCommand command) {
            deleteCommands.add(command);
            if (deleteFailure) {
                return R.fail("role revocation failed");
            }
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
            lastLookupQuery = null;
            lastBindingCommand = null;
            deleteFailure = false;
        }
    }
}
